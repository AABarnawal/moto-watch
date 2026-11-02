# Feature Inventory: Motorola Razr 40 External Display Experience

This document details every feature currently in the repository, documenting its source files, dependencies, permissions, operating mechanism, refactoring strategy, and Android 16 considerations.

---

## 1. Inventory Table

| Feature | Source Files | Permissions | Dependencies | Operational Status | Refactoring Strategy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **LCD Clock Face** | `LcdClockView.java`, `SegmentPainter.java`, `Palette.java`, `NoticeDot.java` | None | DSEG fonts | Active | **Preserve & Enhance** (modularize into clock engine) |
| **Moto Clock Plugin** | `LcdClockService.java`, `LcdSessionService.java`, `LcdSession.java`, `LcdOptionsProvider.java`, `Binders.java` | `BIND_CLOCK_FACE_SERVICE`, `READ_CLOCK_FACE_OPTIONS` | Moto Clock Host | Active | **Preserve Intact** |
| **Cover Screen Deck** | `CoverActivity.java`, `CoverTabs.java`, `SwipeFrame.java`, `CoverPage.java` | None | Display 1 | Active | **Refactor & Modernize** (Phase 3 & 4) |
| **Volume Holds** | `ButtonService.java`, `CoverScreen.java` | `BIND_ACCESSIBILITY_SERVICE` | Accessibility | Active | **Preserve & Integrate** |
| **Hinge & Bank Stealth**| `ButtonPause.java`, `BankApps.java` | `WRITE_SECURE_SETTINGS`, `DUMP`, `PACKAGE_USAGE_STATS` | Hinge Sensor | Active | **Preserve & Harden** |
| **PiP Guard** | `PipGuard.java`, `PipResetActivity.java` | `BIND_ACCESSIBILITY_SERVICE` | SystemUI PiP | Active | **Preserve Intact** |
| **Call Management** | `Calls.java`, `CallView.java`, `CallOverlay.java` | `BIND_NOTIFICATION_LISTENER_SERVICE`, `MODIFY_AUDIO_SETTINGS` | NotificationListener | Active | **Refactor UI, Preserve Logic** |
| **Media Player** | `Media.java`, `MediaListener.java`, `MusicView.java` | `BIND_NOTIFICATION_LISTENER_SERVICE` | MediaSessionManager | Active | **Refactor UI, Preserve Logic** |
| **Notification Drawer**| `Notices.java`, `NotificationsView.java` | `BIND_NOTIFICATION_LISTENER_SERVICE` | NotificationListener | Active | **Refactor UI, Preserve Logic** |
| **Quick Settings** | `QuickControls.java`, `QuickView.java`, `SosService.java` | `WRITE_SECURE_SETTINGS`, `FOREGROUND_SERVICE_SPECIAL_USE` | CameraManager, Settings | Active | **Refactor UI, Preserve Logic** |
| **Weather** | `Weather.java`, `WeatherView.java` | `ACCESS_COARSE_LOCATION`, `INTERNET` | Open-Meteo REST API | Active | **Refactor UI, Preserve Logic** |
| **Calendar** | `CalendarView.java` | `READ_CALENDAR` | CalendarContract | Active | **Refactor UI, Preserve Logic** |
| **Countdown Timer** | `TimerController.java`, `TimerReceiver.java`, `CoverView.java` | `USE_EXACT_ALARM`, `WAKE_LOCK`, `VIBRATE` | AlarmManager, RingService | Active | **Refactor UI, Preserve Logic** |
| **Daily Alarm** | `AlarmController.java`, `AlarmReceiver.java`, `RescheduleReceiver.java` | `USE_EXACT_ALARM`, `WAKE_LOCK`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED` | AlarmManager, RingService | Active | **Refactor UI, Preserve Logic** |
| **Voice Recorder** | `RecorderService.java`, `RecorderView.java`, `Recordings.java`, `RecordingsActivity.java` | `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`, `READ_MEDIA_AUDIO` | MediaRecorder, MediaStore | Active | **Refactor UI, Preserve Logic** |
| **Home Assistant** | `HomeAssistant.java`, `HomeAssistantApi.java`, `HomeView.java` | `INTERNET` | Home Assistant REST | Active | **Refactor UI, Preserve Logic** |
| **Widgets & Tiles** | `ClockWidget.java`, `TimeWidget.java`, `ButtonWidget.java`, `ButtonTile.java` | `BIND_QUICK_SETTINGS_TILE` | AppWidgetManager | Active | **Preserve & Modernize** |
| **Inside App Setup** | `PreviewActivity.java`, `Prefs.java` | System runtime perms | Programmatic UI | Active | **Preserve & Update Settings** |

---

## 2. Detailed Feature Breakdown

### 2.1 LCD Clock Face & Motorola Plugin
- **Files**: `LcdClockView.java`, `SegmentPainter.java`, `Palette.java`, `LcdClockService.java`, `LcdSessionService.java`, `LcdSession.java`, `LcdOptionsProvider.java`, `Binders.java`.
- **How it works**: `LcdClockService` is bound by Motorola's clock face manager when the user selects "LCD Digits" in Settings -> External Display. `LcdSessionService` hosts `LcdClockView` inside a `SurfaceControlViewHost` rendered by Motorola's lockscreen compositor.
- **Refactoring Strategy**:
  - Preserve Motorola IPC and Binder implementation without modification.
  - Decouple `LcdClockView` rendering so that alternative clock styles (Minimal Digital, LCD, Typographic, Analog) can be rendered both inside `CoverActivity` and within the Motorola clock face plugin.
- **Android 16 Concerns**: Minimal; Binder interfaces are low-level and independent of framework changes.

### 2.2 Cover Deck & Navigation
- **Files**: `CoverActivity.java`, `CoverTabs.java`, `SwipeFrame.java`, `CoverPage.java`.
- **How it works**: `CoverActivity` is explicitly launched onto Display 1 (368x194). `SwipeFrame` intercepts horizontal touches to switch pages with sliding animations, and vertical touches to open notifications (swipe up) and quick controls (swipe down).
- **Refactoring Strategy**:
  - Refactor into a clean Navigation & Display Controller (`NavigationController`, `DisplayLifecycleManager`).
  - Introduce new clock faces, redesigned widgets, an Application Launcher, and an AI Voice Assistant.
  - Maintain the existing touch gesture responsiveness and ensure compatibility with Predictive Back on Android 16.

### 2.3 PiP Guard & Edge Workarounds
- **Files**: `PipGuard.java`, `PipResetActivity.java`.
- **How it works**: Monitors the hinge angle sensor (`DROP_DEG = 30°` within 400ms). When rapid fold is detected, queries the accessibility hierarchy for the PiP window and invokes SystemUI's expand action to fullscreen the player before the hinge reaches 15°. Uses `PipResetActivity` to reset SystemUI's PiP display bounds.
- **Refactoring Strategy**: Preserve completely. This is a critical hardware bug workaround for the Razr 40.
- **Android 16 Concerns**: Accessibility node inspection and actions remain supported in API 36.

### 2.4 ButtonPause & Banking App Stealth
- **Files**: `ButtonPause.java`, `BankApps.java`.
- **How it works**: Monitors hinge angle; when unfolded (`> 30°`), removes `ButtonService` from enabled accessibility services via `WRITE_SECURE_SETTINGS`. When folded (`< 15°`), re-enables it. If any banking app is active or in Recents (using `PACKAGE_USAGE_STATS` and `DUMP`), it leaves the service disabled.
- **Refactoring Strategy**: Preserve completely. Prevents anti-malware false positives from bank apps.
- **Android 16 Concerns**: Requires `WRITE_SECURE_SETTINGS` and `DUMP` to be granted via ADB.

### 2.5 Calls & Call Overlay
- **Files**: `Calls.java`, `CallView.java`, `CallOverlay.java`.
- **How it works**: `MediaListener` inspects notifications for `CallStyle` extras or button text (English & Korean). If a call is ringing, `CallOverlay` attaches `CallView` via `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` onto Display 1, rendering over Motorola's stock call screen.
- **Refactoring Strategy**:
  - Preserve notification parsing and call action dispatch logic.
  - Redesign `CallView` with the modern dark AMOLED design system (large touch targets, modern caller typography, crisp icons).
- **Android 16 Concerns**: Ensure `TYPE_ACCESSIBILITY_OVERLAY` windows on external displays conform to window inset rules.

### 2.6 Media Player & Controls
- **Files**: `Media.java`, `MediaListener.java`, `MusicView.java`.
- **How it works**: Queries `MediaSessionManager` for active sessions, extracts title, artist, album art bitmaps, playback position, and transport controls. Supports scrubbing along progress bar.
- **Refactoring Strategy**:
  - Preserve `Media.java` session tracking and scrubbing logic.
  - Redesign `MusicView` with refined modern layout, rounded album art cards, subtle progress track, and touch-optimized controls.

### 2.7 Quick Controls & Flashlight/SOS
- **Files**: `QuickControls.java`, `QuickView.java`, `SosService.java`.
- **How it works**: Controls Torch via `CameraManager`, SOS flashing via `SosService`, DND, Volume, Battery Saver, Night Light, Bedtime mode, and WireGuard VPN (WG Tunnel).
- **Refactoring Strategy**:
  - Redesign `QuickView` with modern 2x3 or 3x2 rounded control tiles.
  - Retain legitimate system controls and maintain fallback status messages when permissions are absent.

### 2.8 Voice Recorder & Media
- **Files**: `RecorderService.java`, `RecorderView.java`, `Recordings.java`, `RecordingsActivity.java`.
- **How it works**: Records audio using `MediaRecorder` in AAC `.m4a` format into `MediaStore.Audio.Media`. Shows real-time amplitude meter and recording timer.
- **Refactoring Strategy**:
  - Preserve recording service and MediaStore storage pipeline.
  - Redesign `RecorderView` with a sleek visualizer and modern stop/record controls.

### 2.9 Weather & Calendar
- **Files**: `Weather.java`, `WeatherView.java`, `CalendarView.java`.
- **How it works**: Queries Open-Meteo REST API using coarse location and system Geocoder. `CalendarView` reads system CalendarProvider events for the next 7 days.
- **Refactoring Strategy**:
  - Redesign visual cards for 368x194 resolution with clear typography and weather iconography.
  - Preserve caching (15-min cache) and error handling.

### 2.10 Home Assistant IoT
- **Files**: `HomeAssistant.java`, `HomeAssistantApi.java`, `HomeView.java`.
- **How it works**: Connects via HTTPS to Home Assistant REST API with Long-Lived Access Tokens to toggle entities (switches, lights, fans).
- **Refactoring Strategy**:
  - Preserve secure credential storage and HTTPS-only enforcement.
  - Redesign cover tiles with modern iconography and status badges.
