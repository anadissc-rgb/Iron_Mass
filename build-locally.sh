#!/usr/bin/env bash
# =============================================================================
#  Iron Mass — Local Build Script
#  Builds a release APK on Linux/macOS using the included Gradle wrapper.
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ─── Colours ─────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo -e "${GREEN}"
echo "  ██╗██████╗  ██████╗ ███╗   ██╗    ███╗   ███╗ █████╗ ███████╗███████╗"
echo "  ██║██╔══██╗██╔═══██╗████╗  ██║    ████╗ ████║██╔══██╗██╔════╝██╔════╝"
echo "  ██║██████╔╝██║   ██║██╔██╗ ██║    ██╔████╔██║███████║███████╗███████╗"
echo "  ██║██╔══██╗██║   ██║██║╚██╗██║    ██║╚██╔╝██║██╔══██║╚════██║╚════██║"
echo "  ██║██║  ██║╚██████╔╝██║ ╚████║    ██║ ╚═╝ ██║██║  ██║███████║███████║"
echo "  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝    ╚═╝     ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝"
echo -e "${NC}"
info "Iron Mass — Release APK Builder"
echo ""

# ─── 1. Check prerequisites ───────────────────────────────────────────────────
info "Checking prerequisites..."

command -v java >/dev/null 2>&1 || error "Java not found. Install JDK 17 or 21."
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [[ "$JAVA_VER" -lt 17 ]]; then
  error "Java $JAVA_VER detected. JDK 17 or 21 is required for AGP 9.x."
fi
info "Java $JAVA_VER found ✓"

# ─── 2. Android SDK ───────────────────────────────────────────────────────────
if [[ -z "$ANDROID_HOME" ]] && [[ -z "$ANDROID_SDK_ROOT" ]]; then
  # Try common locations
  for candidate in \
      "$HOME/Android/Sdk" \
      "$HOME/Library/Android/sdk" \
      "/opt/android-sdk" \
      "/usr/lib/android-sdk"; do
    if [[ -d "$candidate" ]]; then
      export ANDROID_HOME="$candidate"
      export ANDROID_SDK_ROOT="$candidate"
      info "Found Android SDK at: $candidate"
      break
    fi
  done
fi

if [[ -z "$ANDROID_HOME" ]]; then
  error "Android SDK not found.\n
Please either:\n
  1. Set ANDROID_HOME=/path/to/your/Android/Sdk\n
  2. Install Android Studio from https://developer.android.com/studio\n
  3. Install SDK command-line tools from https://developer.android.com/studio#command-tools"
fi

# Update local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties
info "local.properties → sdk.dir=$ANDROID_HOME"

# ─── 3. Gemini API Key ─────────────────────────────────────────────────────────
if [[ -z "$GEMINI_API_KEY" ]]; then
  if [[ -f ".env" ]]; then
    source .env 2>/dev/null || true
  fi
fi

if [[ -z "$GEMINI_API_KEY" ]] || [[ "$GEMINI_API_KEY" == "YOUR_GEMINI_API_KEY_HERE" ]]; then
  warn "GEMINI_API_KEY not set. The app will build but AI features won't work at runtime."
  echo "GEMINI_API_KEY=PLACEHOLDER_KEY" > .env
else
  echo "GEMINI_API_KEY=$GEMINI_API_KEY" > .env
  info "GEMINI_API_KEY configured ✓"
fi

# ─── 4. Keystore setup ────────────────────────────────────────────────────────
BUILD_TYPE="${1:-release}"
SIGN_RELEASE=false

if [[ "$BUILD_TYPE" == "release" ]]; then
  if [[ -n "$KEYSTORE_PATH" ]] && [[ -f "$KEYSTORE_PATH" ]]; then
    info "Using keystore from KEYSTORE_PATH=$KEYSTORE_PATH"
    SIGN_RELEASE=true
  elif [[ -f "ironmass-release.keystore" ]]; then
    export KEYSTORE_PATH="$(pwd)/ironmass-release.keystore"
    export STORE_PASSWORD="${STORE_PASSWORD:-ironmass2026}"
    export KEY_PASSWORD="${KEY_PASSWORD:-ironmass2026}"
    info "Using bundled keystore: $KEYSTORE_PATH"
    SIGN_RELEASE=true
  else
    warn "No keystore found — building unsigned release APK."
    warn "To use the bundled keystore, place ironmass-release.keystore in the project root."
    warn "Default credentials: STORE_PASSWORD=ironmass2026  KEY_PASSWORD=ironmass2026"
  fi
fi

# ─── 5. Make gradlew executable ───────────────────────────────────────────────
chmod +x gradlew

# ─── 6. Build ─────────────────────────────────────────────────────────────────
GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8"
export GRADLE_OPTS

echo ""
info "Starting Gradle build (type=$BUILD_TYPE)…"
echo ""

if [[ "$BUILD_TYPE" == "debug" ]]; then
  ./gradlew assembleDebug --no-daemon
  APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
else
  ./gradlew assembleRelease --no-daemon
  APK_PATH="app/build/outputs/apk/release/app-release.apk"
  # Also check for unsigned variant
  if [[ ! -f "$APK_PATH" ]]; then
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
  fi
fi

# ─── 7. Result ────────────────────────────────────────────────────────────────
echo ""
if [[ -f "$APK_PATH" ]]; then
  APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
  info "✅ BUILD SUCCESSFUL"
  info "APK → $APK_PATH  ($APK_SIZE)"
  echo ""
  echo "  Install on device: adb install -r $APK_PATH"
else
  error "APK not found at expected path: $APK_PATH"
fi
