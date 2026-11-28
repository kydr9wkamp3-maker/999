# CHROMECORE X — Сборка

## Требования
- JDK 17
- Android SDK (API 35)

## Локальная сборка
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Установка на устройство
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.chromecore.camera.debug/com.chromecore.camera.ui.MainActivity
```

## CI
Автоматическая сборка через GitHub Actions: `.github/workflows/android.yml`
Артефакт: `chromecore-x-debug-apk`

## Debug-пакет
`com.chromecore.camera.debug` (applicationIdSuffix = ".debug")
