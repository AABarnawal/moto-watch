# Build & Installation Guide: Motorola Razr 40 External Display OS

## 1. Prerequisites

- **Host Machine**: macOS, Linux, or Windows (via WSL / bash).
- **Android SDK**:
  - Platform `android-36` (Android 16 API 36) or `android-35`.
  - Android SDK Build-Tools `37.0.0` or `36.1.0`.
  - Platform-tools with `adb`.
- **JDK**: Java Development Kit 17 or newer (provides `javac` and `keytool`).
- **Utilities**: `zip`, `zipalign`, `apksigner`.

Verify environment variables:
```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"   # macOS
# or export ANDROID_HOME="$HOME/Android/Sdk"     # Linux
```

---

## 2. Fast Build Pipeline (No Gradle Required)

The project uses a high-performance, deterministic non-Gradle build pipeline:

```sh
./build.sh
```

### Build Pipeline Stages:
1. **Resource Compilation**: `aapt2 compile --dir res -o build/res.zip`
2. **Resource Linking & Manifest Generation**: `aapt2 link -I android.jar --manifest AndroidManifest.xml -A assets --java build/gen -o build/unsigned.apk build/res.zip`
3. **Java Compilation**: `javac --release 17 -Xlint:-options -cp android.jar -d build/classes $(find src build/gen -name '*.java')`
4. **DEX Compilation**: `d8 --release --min-api 33 --lib android.jar --output build $(find build/classes -name '*.class')`
5. **APK Packaging**: `zip -q unsigned.apk classes.dex`
6. **Zip Alignment**: `zipalign -f 4 build/unsigned.apk build/aligned.apk`
7. **APK Signing**: Automatically generates `debug.keystore` (if missing) and signs the package:
   `apksigner sign --ks debug.keystore --ks-pass pass:android --out lcd-clock.apk build/aligned.apk`

---

## 3. Installation to Physical Motorola Razr 40

Connect the phone via USB with **USB Debugging** enabled (Settings -> System -> Developer options):

```sh
./install.sh
```

### What `install.sh` Does:
1. Verifies the device model (`motorola razr 40` / `lynkco`) and Android version (>= API 33).
2. Compiles the APK if any source files are newer than `lcd-clock.apk`.
3. Installs the APK with update replacement flags (`adb install --no-incremental -r lcd-clock.apk`).
4. Grants all runtime and privileged permissions via ADB.
5. Launches settings screens on the inside display for settings that require manual user toggles:
   - **Accessibility**: Turn on *"LCD Clock cover button"*.
   - **Notification access**: Allow *"LCD Clock music page"*.
   - **Clock styles**: Select *"LCD Digits"*.

---

## 4. Testing Build Artifacts

A silent test media player is included in `test/fakeplayer` to verify music scrubbing, track progress, and multi-app cover tabs without needing active audio playing:

```sh
cd test/fakeplayer
./build.sh install
```
Start playback on fake player:
```sh
adb shell am start-foreground-service -n dev.lcdclock.fake/dev.lcdclock.fake.FakePlayer -a play
```
