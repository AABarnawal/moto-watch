# System Architecture: Motorola Razr 40 External Display OS

## 1. High-Level Architecture

The Motorola Razr 40 External Display OS transforms the 1.5-inch external OLED screen (368 × 194 pixels) into a rich, autonomous multitasking environment. The application operates in two parallel execution planes:

```
+-----------------------------------------------------------------------------------------+
|                                    ANDROID RUNTIME                                      |
|                                                                                         |
|   +------------------------------------+    +---------------------------------------+   |
|   | Motorola Host Subsystem            |    | Autonomous External Display Subsystem |   |
|   | (DirectBootAware Services)         |    | (Display ID 1: 368x194 @ 60Hz)        |   |
|   |                                    |    |                                       |   |
|   | - LcdClockService (Plugin binding) |    | - CoverActivity (TaskAffinity: cover) |   |
|   | - LcdSessionService (ViewHost IPC) |    | - SwipeFrame (Gestural Coordinator)   |   |
|   | - LcdSession (Session Controller)  |    | - CoverTabs (Visual Tab Indicator)    |   |
|   | - LcdOptionsProvider (Themes)      |    | - HomeDeckView (Clock + Glance Chips) |   |
|   +-----------------+------------------+    +-------------------+-------------------+   |
|                     |                                           |                       |
|                     +---------------------+---------------------+                       |
|                                           |                                             |
|                                           v                                             |
|                             +---------------------------+                               |
|                             | Unified ClockFaceEngine   |                               |
|                             | - 7-Segment LCD           |                               |
|                             | - Modern Minimal Digital  |                               |
|                             | - Typographic Numeral     |                               |
|                             | - Minimalist Dark Analog  |                               |
|                             +---------------------------+                               |
|                                           ^                                             |
|                                           |                                             |
|   +---------------------------------------+-----------------------------------------+   |
|   | Modular External Display Services & Integrations                                |   |
|   |                                                                                 |   |
|   | - VoiceSessionManager & GeminiProvider (Official Google Gemini 1.5/2.5 REST API)|   |
|   | - AppsView (Multi-display application launcher targeting Display 1)             |   |
|   | - Media & MusicView (MediaSession scrubbing, metadata, art rendering)           |   |
|   | - Calls & CallOverlay (System/VoIP CallStyle notification interception)         |   |
|   | - Notices & NotificationsView (Privacy-aware expandable notification drawer)    |   |
|   | - QuickControls & QuickView (AMOLED toggles: Flashlight, SOS, DND, VPN, etc.)   |   |
|   | - Weather & CalendarView (Open-Meteo REST & Android CalendarContract)          |   |
|   | - RecorderService & RingService (Foreground services: Mic & MediaPlayback)      |   |
|   | - ButtonPause & PipGuard (Hinge angle observer & SystemUI bug workarounds)      |   |
|   +---------------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------------+
```

---

## 2. Layer Separation & Responsibilities

### 2.1 Core & Display Management
- **`DisplayManager` / `CoverScreen.java`**: Detects clamshell hinge angle, physical display availability (`Display.DEFAULT_DISPLAY` vs `CoverScreen.DISPLAY_ID = 1`), and manages multi-display activity launch bundles.
- **`ButtonService.java`**: Android Accessibility Service providing background launch exemptions and monitoring volume-button hold gestures.
- **`ButtonPause.java`**: Dynamically toggles `ButtonService` in system secure settings based on hinge fold telemetry and banking application presence.
- **`PipGuard.java`**: Detects clamshell fold velocity to expand PiP video before SystemUI dismissal, and resets orientation with `PipResetActivity`.

### 2.2 UI & Navigation Engine
- **`CoverActivity.java`**: Manages the cover screen stack, system insets, and Predictive Back callbacks (`OnBackInvokedDispatcher`).
- **`SwipeFrame.java`**: Touch interceptor directing horizontal swipes to adjacent carousel pages and vertical swipes to Quick Settings (down) and Notifications (up).
- **`CoverTabs.java` & `TabIcons.java`**: Top tab header displaying active page pill indicator and custom vector geometry icons.
- **`DesignSystem.java`**: Design tokens, dark AMOLED colors, card layout metrics, and touch target helpers.
- **`HomeDeckView.java`**: Home screen integrating the clock face, battery pill, weather glance, active music chip, and notification indicator.
- **`AppsView.java`**: External display application launcher with intent safety and multi-display target options.
- **`AssistantView.java`**: Dedicated AI voice assistant page with animated audio waveforms, live transcription, scrollable answers, and spoken audio controls.

### 2.3 Services & Data Providers
- **`GeminiProvider.java`**: Official Google Gemini REST API client with multi-turn conversation support and strict JSON schemas.
- **`VoiceSessionManager.java`**: Integrates `SpeechRecognizer`, `GeminiProvider`, and `TextToSpeech` into a reactive state machine.
- **`Calls.java` & `CallOverlay.java`**: Intercepts cellular and VoIP notifications, attaching an overlay above Motorola's call UI.
- **`Media.java`**: Bridges to `MediaSessionManager` for active player transport control and scrubbing.
- **`HomeAssistantApi.java`**: HTTPS REST client communicating with Home Assistant instances.
- **`Weather.java`**: Caching HTTP client for Open-Meteo REST API.
- **`RecorderService.java`**: Audio capture service writing AAC `.m4a` files into `MediaStore`.

---

## 3. Motorola Framework Interoperability
The application preserves complete binary compatibility with Motorola's proprietary lockscreen clock plugin:
- `LcdClockService` responds to `com.motorola.service.action.CLOCK_FACE_SERVICE`.
- `LcdSessionService` creates a `SurfaceControlViewHost` for `com.motorola.service.action.CLOCK_FACE_SESSION_SERVICE`.
- `LcdOptionsProvider` exposes color styles to `com.motorola.clockface.action.CLOCK_FACE_OPTIONS`.
- `Binders.java` executes oneway Binder transactions without AIDL stubs.
- If Motorola's clock plugin is unselected or disabled, `CoverActivity` continues to function independently.
