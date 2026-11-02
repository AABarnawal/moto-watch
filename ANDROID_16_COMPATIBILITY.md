# Android 16 (API 36) Compatibility Audit

## 1. Executive Summary

This document evaluates the compatibility of `razr-lcd-clock` with **Android 16 (API 36)**. The existing codebase declares:
```xml
<uses-sdk android:minSdkVersion="33" android:targetSdkVersion="35" />
```
The local development environment has Android 16 SDK (`android-36/android.jar`) and `build-tools/37.0.0` installed.

Auditing the source code against API 36 platform requirements identifies several key compatibility vectors:
1. **Toolchain & Target SDK Upgrade**: Updating `build.sh` and `AndroidManifest.xml` to compile against `android-36` and target API 36.
2. **Edge-to-Edge & Window Insets Enforcement**: Fullscreen activities on external displays must handle system insets properly.
3. **Foreground Service Policies & Lifecycle**: Strict enforcement of runtime permissions for `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and `FOREGROUND_SERVICE_SPECIAL_USE`.
4. **Background Activity Launch (BAL) Restrictions**: Hardened restrictions when launching activities from background services.
5. **Predictive Back & Gesture Insets**: Handling back invocations via `OnBackInvokedDispatcher`.
6. **Reflection & Restricted Hidden APIs**: Hardened runtime greylists blocking hidden fields like `WindowManager.LayoutParams.userActivityTimeout`.
7. **Microphone Access & Speech Recognition**: Permissions and lifecycle boundaries for speech recognition on the external display.

---

## 2. Itemized Compatibility Audit

### 2.1 Build Toolchain & SDK Resolution
- **Current State**: `build.sh` hardcodes:
  ```bash
  JAR=$SDK/platforms/android-35/android.jar
  ```
  Fails when only `android-36` is installed.
- **Android 16 Behaviour**: Android 16 platform libraries reside in `$ANDROID_HOME/platforms/android-36/android.jar`.
- **Impact**: Immediate build failure.
- **Remediation**: Update `build.sh` to dynamically detect and prioritize `android-36`, falling back to `android-35` if present.

### 2.2 Background Activity Launches (BAL)
- **Current State**: `ButtonService` (Accessibility Service) starts `CoverActivity` from the background using:
  ```java
  context.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(CoverScreen.DISPLAY_COVER).toBundle());
  ```
  `CoverHome` and `Calls` also launch `CoverActivity`.
- **Android 16 Behaviour**: Background Activity Launch (BAL) protections are enforced more strictly. Background launches without explicit intent creator authorization can be dropped even for accessibility services or companion apps unless opt-in flags are set.
- **Remediation**: Explicitly pass `ActivityOptions.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)` when wrapping intents in `PendingIntent`, and ensure `ActivityOptions.setLaunchDisplayId` is consistently coupled with valid display availability checks.

### 2.3 Window Insets & Edge-to-Edge Enforcement
- **Current State**: `CoverActivity` uses:
  ```xml
  android:theme="@android:style/Theme.DeviceDefault.NoActionBar.Fullscreen"
  ```
  In Android 15/16, edge-to-edge is enforced by default. If the external display driver reports system bar or cutout insets, content could overlap or be cropped unless insets are consumed or tracked.
- **Android 16 Behaviour**: Deprecated legacy window flags like `FLAG_FULLSCREEN` are ignored in edge-to-edge mode. Apps must consume `WindowInsets` via `View.setOnApplyWindowInsetsListener`.
- **Remediation**: Add an `OnApplyWindowInsetsListener` on the root frame of `CoverActivity` to ensure insets are properly consumed and content is measured within safe bounds.

### 2.4 Predictive Back Navigation
- **Current State**: `CoverActivity` handles back navigation primarily via custom swipe gesture handling (`SwipeFrame`) and does not declare `android:enableOnBackInvokedCallback="true"`.
- **Android 16 Behaviour**: Predictive back is universally active. Unhandled back gestures trigger default activity finish transitions which can bypass internal page stack logic.
- **Remediation**: Declare `android:enableOnBackInvokedCallback="true"` in the manifest and register an `OnBackInvokedCallback` on `CoverActivity` when sub-pages or panels (such as AI Assistant, Quick Settings, or Notifications) are open.

### 2.5 Reflection on Hidden Window Parameters
- **Current State**: In `CoverActivity.java` (lines 377-383):
  ```java
  WindowManager.LayoutParams attrs = getWindow().getAttributes();
  try {
      WindowManager.LayoutParams.class.getField("userActivityTimeout").setLong(attrs, timeout);
      getWindow().setAttributes(attrs);
  } catch (ReflectiveOperationException | RuntimeException e) {
      Log.w(Binders.TAG, "can't set the screen timeout", e);
  }
  ```
- **Android 16 Behaviour**: In Android 16, access to greylisted non-SDK interfaces such as `userActivityTimeout` via reflection may trigger `NoSuchFieldException` or be blocked outright.
- **Remediation**: The current implementation already safely wraps the reflective call in a `try/catch`. However, fallback handling should be ensured: rely on `FLAG_KEEP_SCREEN_ON` during active ringing/recording and standard system timeout when reflection fails, rather than depending on internal field manipulation.

### 2.6 Foreground Service Restrictions
- **Current State**: Declares 4 foreground services:
  1. `RecorderService`: `foregroundServiceType="microphone"`
  2. `RingService`: `foregroundServiceType="mediaPlayback"`
  3. `SosService`: `foregroundServiceType="specialUse"`
- **Android 16 Behaviour**:
  - `FOREGROUND_SERVICE_MICROPHONE` cannot be launched from the background unless the app holds an active top activity, accessibility service, or explicit user interaction exemption. In `RecorderService`, recording is triggered from `RecorderView` while `CoverActivity` is visibly resumed on the cover screen.
  - `FOREGROUND_SERVICE_SPECIAL_USE` requires a declared subtype property, which is already present:
    ```xml
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Flashes SOS in Morse code with the flashlight until the user stops it." />
    ```
- **Remediation**: Verify that `RecorderService` is only initiated when `CoverActivity` is in the foreground (`RESUMED`). Add runtime state verification before calling `startForegroundService`.

### 2.7 Microphone Privacy & Speech Recognition (AI Assistant)
- **Current State**: `RECORD_AUDIO` is granted at install time via ADB.
- **Android 16 Behaviour**: Android 16 enforces strict privacy indicators (the green microphone pill) and restricts microphone acquisition to foreground apps. Calling `SpeechRecognizer.startListening` while the screen is locked or while in the background will fail with `ERROR_INSUFFICIENT_PERMISSIONS` or `ERROR_AUDIO`.
- **Remediation**: The AI voice assistant must operate only while `CoverActivity` is in the foreground, with explicit user tap-to-talk initiation. Always check `checkSelfPermission(Manifest.permission.RECORD_AUDIO)` and handle user denial gracefully.

### 2.8 Motorola Clock Face Integration Compatibility
- **Current State**: `LcdClockService` and `LcdSessionService` depend on proprietary binder transactions and the permission `com.motorola.permission.BIND_CLOCK_FACE_SERVICE`.
- **Android 16 Behaviour**: Unless Motorola alters their system clock face plugin architecture in their Android 16 firmware updates, the underlying Binder IPC contracts remain binary compatible. If the clock face service is ever unbound or deprecated by Motorola, `CoverActivity` is architecturally decoupled and will continue running as an autonomous external display activity.

---

## 3. Compatibility Matrix Summary

| Subsystem / API | Current Implementation | Android 16 Impact | Required Action | Status |
| :--- | :--- | :--- | :--- | :--- |
| **SDK Platform** | `android-35` | Missing on machine | Point `build.sh` to `android-36` | Planned (Phase 1) |
| **Target SDK** | 35 | Upgrade to 36 | Update `AndroidManifest.xml` | Planned (Phase 1) |
| **Window Insets** | `FLAG_FULLSCREEN` | Edge-to-edge default | Add insets listener | Planned (Phase 1) |
| **Predictive Back** | Unregistered | Navigation drop | Implement `OnBackInvokedCallback` | Planned (Phase 1) |
| **Window Reflection** | `userActivityTimeout` | Greylist restriction | Validate fallback | Verified Safe |
| **Speech Recognition** | Not implemented | Privacy & mic policies | Tap-to-talk within resumed state | Planned (Phase 5) |
| **Foreground Services** | Typed in manifest | Foreground state check | Audit service triggers | Verified Safe |
| **Moto Clock Plugin** | Binder stubs | OEM dependent | Isolate from `CoverActivity` | Verified Safe |
