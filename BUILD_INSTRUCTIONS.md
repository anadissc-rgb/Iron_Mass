# Iron Mass — Build Instructions

## Fixes Applied

The following build errors were found and fixed:

### 1. `compileSdk` invalid DSL syntax (app/build.gradle.kts, line 11)
**Error:** `compileSdk { version = release(36) { minorApiLevel = 1 } }`
`minorApiLevel` is not a valid property in AGP's `SdkExtensionVersion` DSL for a stable SDK release.
**Fix:** Changed to standard `compileSdk = 36`

### 2. KSP / Kotlin version mismatch (gradle/libs.versions.toml)
**Error:** `kotlin = "2.2.10"` paired with `googleDevtoolsKsp = "2.3.5"`
KSP must use a version compiled against the same Kotlin compiler. Using mismatched versions
causes the annotation processor to fail to load, breaking Room and Moshi codegen.
**Fix:** Aligned both to `kotlin = "2.1.21"` and `googleDevtoolsKsp = "2.1.21-1.0.32"`

### 3. Compose BOM out of date (gradle/libs.versions.toml)
**Error:** `composeBom = "2024.09.00"` — September 2024 BOM doesn't include
APIs needed by newer Kotlin/AGP toolchains.
**Fix:** Updated to `composeBom = "2025.04.01"`

### 4. Unsafe signing config (app/build.gradle.kts)
**Error:** `storeFile = file(keystorePath)` ran unconditionally — Gradle evaluates
the file path even when the keystore doesn't exist, causing a configuration-phase failure.
**Fix:** Signing blocks are now guarded; the release signing config is only created
when `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables are all set.

### 5. Missing `kotlinOptions { jvmTarget = "11" }` (app/build.gradle.kts)
**Error:** `compileOptions` set Java 11 compatibility but `kotlinOptions` was missing,
causing a "different JVM target compatibility" lint warning that can escalate to an error.
**Fix:** Added `kotlinOptions { jvmTarget = "11" }`

### 6. String-quoted ksp() calls (app/build.gradle.kts)
**Error:** `"ksp"(libs.androidx.room.compiler)` — string-based configuration names
are deprecated and will be removed in future Gradle versions.
**Fix:** Changed to `ksp(libs.androidx.room.compiler)` (proper KSP DSL)

---

## Building a Debug APK (Android Studio)

1. Open Android Studio (Hedgehog 2023.1.1 or newer)
2. **File → Open** → select this folder
3. Wait for Gradle sync to complete
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. APK is at `app/build/outputs/apk/debug/app-debug.apk`

## Building a Release APK

### Option A — Using Android Studio
1. **Build → Generate Signed Bundle / APK**
2. Choose **APK**, click Next
3. Create or select your keystore, fill in passwords
4. Choose `release` build variant → Finish

### Option B — Command line with environment variables
```bash
export KEYSTORE_PATH=/path/to/your/keystore.jks
export STORE_PASSWORD=yourStorePassword
export KEY_PASSWORD=yourKeyPassword

./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Notes
- `minSdk = 24` (Android 7.0+)
- `targetSdk = 36`
- Room database version 5 (uses `fallbackToDestructiveMigration`)
- No Firebase services are actively initialized (BOM is included but all Firebase libs are commented out)
