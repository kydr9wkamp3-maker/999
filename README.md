# CHROMECORE X

<p align="center">
  <strong>Камера для Android с хромовым дизайном.</strong><br>
  <em>Obsidian Chrome UI · CameraX · Jetpack Compose</em><br>
  Target: Motorola Moto G85 5G
</p>

---

## Что сейчас работает

- **CameraX preview** — задняя и фронтальная камеры
- **Сохранение фото** — через MediaStore в `Pictures/ChromecoreX/` (видно в галерее)
- **Зум** — щипок + слайдер + индикатор уровня
- **Вспышка** — переключение OFF / AUTO / ON (иконка)
- **Переключение камер** — кнопка в правом верхнем углу
- **Настройки** — экран настроек (кнопка ⚙️, заготовка)
- **Chromecore Theme** — тёмная тема, хромовая кнопка затвора

## Что НЕ работает (PLANNED)

- [ ] Pro-режим (ISO, выдержка, WB, фокус)
- [ ] HDR / Ночь / Портрет (через CameraX Extensions)
- [ ] Живые GPU-фильтры
- [ ] AR маски и стикеры
- [ ] Нод-редактор эффектов
- [ ] Видео
- [ ] Редактор фото
- [ ] Галерея внутри приложения
- [ ] Device Lab (диагностика железа)

См. полный план: `AGENTS.md` §8.

## Стек

| Компонент | Версия |
|-----------|--------|
| Kotlin | 2.0.21 |
| AGP | 8.7.3 |
| Gradle | 8.9 |
| JDK | 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Compose BOM | 2024.12.01 |
| CameraX | 1.3.4 |

## Сборка

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Debug-пакет: `com.chromecore.camera.debug`

Запуск на устройстве:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.chromecore.camera.debug/com.chromecore.camera.ui.MainActivity
```

## Структура репо

```
app/src/main/kotlin/com/chromecore/camera/
├── ChromecoreApp.kt
├── ui/
│   ├── MainActivity.kt
│   ├── theme/
│   │   ├── ChromecoreColors.kt   # палитра Obsidian Chrome
│   │   └── Theme.kt
│   ├── components/
│   │   ├── ShutterButton.kt      # хромированная кнопка затвора
│   │   └── ModeSelector.kt       # переключатель режимов (заготовка)
│   └── screens/
│       ├── camera/CameraScreen.kt
│       └── settings/SettingsScreen.kt
```

## Документация

- `AGENTS.md` — полный план разработки и правила
- `EFFECTS_CATALOG.md` — каталог планируемых эффектов
- `docs/PROGRESS.md` — прогресс по этапам
- `docs/AUDIT.md` — история проблем и причины

## Лицензия

См. `LICENSE`.
