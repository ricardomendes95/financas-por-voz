#!/usr/bin/env bash
# Builda o APK debug do app. Uso: ./scripts/build.sh
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/temurin-17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew assembleDebug

echo "==> APK gerado em: app/build/outputs/apk/debug/app-debug.apk"
