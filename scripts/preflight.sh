#!/usr/bin/env bash
# scripts/preflight.sh — быстрые проверки без Gradle (секунды)
set -uo pipefail
fail=0
say() { echo "PREFLIGHT: $*"; }

# 1) Дубли @Composable подряд
while IFS= read -r f; do
  if awk 'prev ~ /^[[:space:]]*@Composable[[:space:]]*$/ && $0 ~ /^[[:space:]]*@Composable[[:space:]]*$/ {print FILENAME":"NR": duplicate @Composable"; bad=1} {prev=$0} END{exit bad+0}' "$f"; then :; else fail=1; fi
done < <(find . -name '*.kt' -not -path '*/build/*' -not -path './.git/*')

# 2) tools: без объявленного xmlns:tools (причина AAPT2-падения)
while IFS= read -r f; do
  if grep -q 'tools:' "$f" && ! grep -q 'xmlns:tools=' "$f"; then
    say "$f uses tools: but has no xmlns:tools"; fail=1
  fi
done < <(find . -path '*/res/*' -name '*.xml' -not -path '*/build/*')

# 3) Фальшивая зелёнка в CI
if grep -RInE 'continue-on-error:[[:space:]]*true|\|\|[[:space:]]*true|exit 0|always pass|if:[[:space:]]*always\(\)' .github/workflows 2>/dev/null; then
  say "fake-green pattern in workflows"; fail=1
fi

# 4) Пропуск проверок в Gradle-вызовах
if grep -RInE -- '-x (test|lint|check)' .github scripts 2>/dev/null | grep -v preflight.sh; then
  say "skipped checks in gradle invocation"; fail=1
fi

# 5) Пустые catch
if grep -RInE 'catch \([^)]*\)[[:space:]]*\{[[:space:]]*\}' --include='*.kt' . 2>/dev/null; then
  say "empty catch block"; fail=1
fi

# 6) local.properties и секреты не в git
if git ls-files --error-unmatch local.properties >/dev/null 2>&1; then say "local.properties is tracked"; fail=1; fi
if grep -RInE '(AIza[0-9A-Za-z_-]{20,}|sk-[A-Za-z0-9]{20,}|BEGIN (RSA|PRIVATE) KEY)' --exclude-dir=.git --exclude-dir=build . 2>/dev/null; then
  say "possible secret in repo"; fail=1
fi

# 7) Висячие TODO в ключевых путях
if grep -RInE 'TODO\(\)|NotImplementedError' --include='*.kt' app/src/main 2>/dev/null; then
  say "unimplemented stub in main sources"; fail=1
fi

if [ "$fail" -ne 0 ]; then say "FAILED"; exit 1; fi
say "OK"
