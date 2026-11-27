#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
OUT="${TMPDIR:-/tmp}/adamant-roundtrip-test"
${CXX:-c++} -std=c++17 -O2 -Wall -Wextra -Wpedantic "$ROOT/tests/native/adamant_roundtrip.cpp" -o "$OUT"
"$OUT"
