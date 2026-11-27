# CHROMECORE X — Build Guide

## Quick Start

### Prerequisites
- JDK 17 (Temurin recommended: https://adoptium.net/)
- Android SDK 35
- Android Build Tools 35.0.0

### One-command build
```bash
./scripts/build.sh
```

### Manual build
```bash
# Set SDK path
export ANDROID_HOME=$HOME/Android/Sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Build
chmod +x gradlew
./gradlew assembleDebug

# Find APK
ls -lh app/build/outputs/apk/debug/
```

### Install on device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.chromecore.camera/.ui.MainActivity
```

## GitHub Actions

CI/CD is configured in `.github/workflows/android-ci.yml`.

On every push to `main` or `arena/**` branches:
1. Lint runs
2. Unit tests run
3. Debug APK is built
4. APK is uploaded as a GitHub Artifact

You can download the APK directly from the Actions tab.

## Project Structure

```
CHROMECORE X/
├── app/                        Main application module
│   ├── build.gradle.kts        Dependencies & build config
│   ├── proguard-rules.pro      Release shrinking rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/chromecore/camera/
│       │   │   ├── ChromecoreApp.kt          Application entry
│       │   │   ├── engine/CameraEngine.kt     Camera2 engine
│       │   │   ├── capabilities/              Device scanner
│       │   │   ├── processing/                Image pipeline
│       │   │   ├── filters/                   30+ filter presets
│       │   │   ├── ui/
│       │   │   │   ├── MainActivity.kt        Entry activity
│       │   │   │   ├── theme/                 Chromecore design
│       │   │   │   ├── navigation/            Compose nav
│       │   │   │   └── screens/               5 screens
│       │   │   ├── di/                        Hilt module
│       │   │   └── utils/                     MediaStore saver
│       │   └── res/                           Resources
│       └── test/                              Unit tests
├── docs/                      Documentation
├── scripts/                   Build & verify scripts
├── .github/workflows/         CI/CD pipeline
├── gradle/libs.versions.toml  Version catalog
├── build.gradle.kts           Root build
└── settings.gradle.kts        Project settings
```

## Development

### Run tests
```bash
./gradlew testDebugUnitTest
```

### Run lint
```bash
./gradlew lint
```

### Debug build
```bash
./gradlew assembleDebug
```

### Release build
```bash
./gradlew assembleRelease
```

## Troubleshooting

### "SDK location not found"
```bash
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
```

### "JAVA_HOME not set"
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
# or on macOS:
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### "Gradle wrapper not found"
Generate it:
```bash
gradle wrapper --gradle-version 8.9
```

### Build too slow
```bash
# Use daemon and parallel builds (already configured in gradle.properties)
./gradlew assembleDebug --parallel --daemon
```
