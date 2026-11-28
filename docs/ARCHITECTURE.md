# CHROMECORE X — Реальная архитектура

## Статус: S0.2 (документация приведена к коду)

## Реальная структура проекта

```
app/src/main/kotlin/com/chromecore/camera/
├── ChromecoreApp.kt              # Application (пустой, без DI)
├── ui/
│   ├── MainActivity.kt           # ComponentActivity, ChromecoreTheme
│   ├── theme/
│   │   ├── ChromecoreColors.kt   # палитра Obsidian Chrome
│   │   └── Theme.kt              # Material3 dark theme
│   ├── components/
│   │   ├── ShutterButton.kt      # хромированная кнопка с градиентом
│   │   └── ModeSelector.kt       # переключатель режимов (заготовка, не подключён)
│   └── screens/
│       ├── camera/
│       │   └── CameraScreen.kt   # CameraX preview, зум, вспышка, сохранение
│       └── settings/
│           └── SettingsScreen.kt # экран настроек (заглушки пунктов)
```

## Зависимости

- CameraX 1.3.4 (camera-core, camera-camera2, camera-lifecycle, camera-view)
- Compose BOM 2024.12.01 (material3, material-icons-extended)
- lifecycle-runtime-compose 2.8.7 (LocalLifecycleOwner)

## Что НЕ реализовано (нет в коде)

- Hilt / DI
- Navigation component
- Room / база данных
- ML Kit / MediaPipe
- GPU-шейдеры
- Camera2 engine (CameraX используется напрямую)
- Редактор
- Галерея
- Device Lab
- Video capture

## PLANNED (см. AGENTS.md §5)

- core/ — FeatureFlags, AppLog
- camera/ — CameraController, Capabilities
- gl/ — GL-конвейер для эффектов
- effects/ — registry, builtin, LUT
- ar/ — face, hands, segmentation
- nodes/ — граф-движок
- timeline/ — слои и ключевые кадры
- editor/ — неразрушающий редактор
- video/ — захват видео
- gallery/ — MediaStore галерея
- devicelab/ — диагностика
- settings/ — DataStore
