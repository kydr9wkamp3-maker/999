# CHROMECORE X — AUDIT

## Дата последнего аудита: 2026-11-28

## История проблем

### Корень красных сборок (установлено):
1. `gradle-wrapper.jar` отсутствовал → `ClassNotFoundException: GradleWrapperMain`
2. `ObsidianChrome`/`VaporAccent` — несуществующие символы в CameraScreen
3. `maxZoomFactor` → `maxZoomRatio` (CameraX 1.3.x API)
4. `| tail -80` в CI маскировал exit code Gradle

### Маскирования в истории (исправлены):
- `if: always()` на Upload step
- `continue-on-error`, `|| true`
- `-x lint`, `-x test`
- `abortOnError=false` для lint

## Текущее состояние

### Конфигурация:
- JDK 17, AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9
- compileSdk 35, targetSdk 35, minSdk 26
- Compose BOM 2024.12.01, CameraX 1.3.4
- `configuration-cache=false` — из-за ошибки сериализации mapDebugSourceSetPaths

### Что есть:
- ✅ Зелёная сборка (CI `46138374491`)
- ✅ APK в артефактах (17.0 МБ)
- ✅ CameraX preview + MediaStore сохранение
- ✅ Зум, вспышка, переключение камер
- ✅ ChromecoreTheme + ShutterButton
- ✅ SettingsScreen (заготовка)

### Что НЕ работает (нужны правки):
- ❌ Вспышка AUTO не делает ничего, ON = фонарик а не вспышка при съёмке
- ❌ maxZoom из LiveData без observeAsState — слайдер может не появиться
- ❌ zoomLevel не сбрасывается при смене камеры
- ❌ bindCamera вызывается дважды
- ❌ ModeSelector не подключён
- ❌ Заглушки `/* TODO */` в SettingsScreen

## План исправлений
1. **S0.2** — документация к правде ✅ (in progress)
2. **S1** — исправить вспышку, зум, меню настроек
3. **S2** — тап-фокус, экспозиция
