# CHROMECORE X — AUDIT

## Дата аудита: 2026-11-27

## История проблем (до Phase 0)

### Коммиты-нарушения из истории репо:
| Коммит | Проблема | Нарушение |
|--------|----------|-----------|
| `6407c1e` | "make Verify APK always pass" | Фальшивая зелёнка — `if: always()` на проверочном шаге |
| `fe3ecc2` | "make assemble step non-failing" | Маскирование падения сборки |
| `4dd2ac5` | "disable configuration-cache" | Крутил ручки без чтения ошибки |
| `9951de4` | "remove duplicate @Composable" | Дубли появились от слепой записи файлов |
| `6a71672` | "declare tools namespace in themes.xml" | AAPT2 ошибка, которую ловит preflight |
| `7fff806` | "create local.properties with SDK path" | Лечение симптома, не причины |

### Корень красных сборок (установлено в Phase 0):
1. **`ObsidianChrome` и `VaporAccent`** — несуществующие символы в CameraScreen.kt (импорт из ниоткуда). Исправлено в `8b65e46`.
2. **Нет runtime permission request** — приложение не запрашивало разрешение камеры, показывало чёрный экран или "Camera permission required" без кнопки. Исправлено в `cbcb324`.
3. **Фото сохранялись в `getExternalFilesDir`** — приватная папка приложения, НЕ видна в галерее. **Не исправлено — S1 blocker.**

## Текущее состояние

### Конфигурация:
- JDK 17, AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9
- compileSdk 35, targetSdk 35, minSdk 26
- Compose BOM 2024.12.01, CameraX 1.3.4
- `configuration-cache=false` — из-за ошибки сериализации `mapDebugSourceSetPaths` в CameraX 1.3.4 с AGP 8.7.3. Крайняя мера.

### Что есть:
- ✅ Зелёная сборка (run `46009081251`)
- ✅ APK в артефактах (16.9 МБ)
- ✅ CameraX preview + permission request
- ✅ ChromecoreTheme + Obsidian Chrome палитра

### Что НЕ работает:
- ❌ Фото НЕ сохраняются в галерею (сохранение в приватную папку)
- ❌ Нет MediaStore интеграции
- ❌ Нет переключения камер в runtime (флаг есть, но перепривязка не вызывается при изменении `useFrontCamera`)
- ❌ Нет зума, тап-фокуса, вспышки

## План исправлений

1. **S0**: Заменить CI на эталонный + preflight → честная зелёная сборка
2. **S1**: MediaStore сохранение + исправление переключения камер
3. **S2**: Зум, тап-фокус, вспышка
