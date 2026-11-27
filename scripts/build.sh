#!/usr/bin/env bash
# CHROMECORE X — Local build and verification script
# Usage: ./scripts/build.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

echo "═══════════════════════════════════════════════"
echo "  CHROMECORE X — Build & Verify"
echo "═══════════════════════════════════════════════"
echo ""

# --- Step 0: Check prerequisites ---
echo "▸ Checking prerequisites..."

if ! command -v java &>/dev/null; then
    echo "✗ JDK not found. Install JDK 17:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  macOS: brew install openjdk@17"
    echo "  Or download: https://adoptium.net/"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1)
echo "  Java: $JAVA_VER"

if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
    echo "✗ ANDROID_HOME not set."
    echo "  Set it to your Android SDK path:"
    echo "  export ANDROID_HOME=\$HOME/Android/Sdk"
    echo "  Or use Android Studio → SDK Manager → SDK Location"
    exit 1
fi

SDK_DIR="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo "  Android SDK: $SDK_DIR"

if [ ! -f "local.properties" ]; then
    echo "sdk.dir=$SDK_DIR" > local.properties
    echo "  Created local.properties"
fi

# --- Step 1: Clean ---
echo ""
echo "▸ Cleaning previous build..."
chmod +x gradlew
./gradlew --no-daemon clean 2>&1 | tail -3

# --- Step 2: Lint ---
echo ""
echo "▸ Running lint..."
./gradlew --no-daemon lint 2>&1 | tail -5

# --- Step 3: Unit tests ---
echo ""
echo "▸ Running unit tests..."
./gradlew --no-daemon testDebugUnitTest 2>&1 | tail -5

# --- Step 4: Assemble debug APK ---
echo ""
echo "▸ Building debug APK..."
./gradlew --no-daemon assembleDebug 2>&1 | tail -5

# --- Step 5: Verify APK ---
echo ""
echo "▸ Verifying APK..."
APK=$(find app/build/outputs/apk/debug -name "*.apk" -print -quit 2>/dev/null || true)

if [ -z "$APK" ]; then
    echo "✗ Build failed — no APK found."
    echo "  Check the errors above."
    exit 1
fi

SIZE=$(ls -lh "$APK" | awk '{print $5}')
SHA=$(sha256sum "$APK" | cut -d' ' -f1)

echo ""
echo "═══════════════════════════════════════════════"
echo "  ✓ BUILD SUCCESSFUL"
echo "═══════════════════════════════════════════════"
echo "  APK:   $APK"
echo "  Size:  $SIZE"
echo "  SHA:   $SHA"
echo ""

# --- Step 6: Install on device (optional) ---
if command -v adb &>/dev/null; then
    DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
    if [ "$DEVICE_COUNT" -gt 0 ]; then
        echo "▸ Installing on device..."
        adb install -r "$APK"
        echo "▸ Launching app..."
        adb shell am start -n com.chromecore.camera/.ui.MainActivity
        echo "  ✓ App launched on device"
    else
        echo "  No device connected. Install manually:"
        echo "  adb install $APK"
    fi
else
    echo "  adb not found. Install manually:"
    echo "  adb install $APK"
fi

echo ""
echo "═══════════════════════════════════════════════"
echo "  CHROMECORE X v1.0.0-alpha01"
echo "═══════════════════════════════════════════════"
