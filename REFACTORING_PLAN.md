# Refactoring Plan: Modular Architecture for External Display OS

## 1. Objectives

1. Modernize the codebase from monolithic UI/logic files into a structured, modular design.
2. Establish clean separation between Core, External Display, UI, Services, Integrations, and Data layers.
3. Retain complete compatibility with the lightweight non-Gradle toolchain (`aapt2`, `javac`, `d8`, `apksigner`).
4. Support Android 16 (API 36) runtime models and permissions.
5. Introduce a unified dark AMOLED design system optimized for 368 × 194 pixels.
6. Implement an independent AI voice assistant with SpeechRecognizer, Gemini API integration, and TextToSpeech.
7. Preserve all existing Motorola clock face bindings, accessibility workarounds, and device state managers.

---

## 2. Logical Architecture

```
Application
|
+-- Core
|   +-- DisplayManager              (External display detection, resolution metrics)
|   +-- PermissionManager           (Runtime and privileged permission checks)
|   +-- DeviceCapabilities          (Hinge sensor telemetry, audio routing)
|
+-- ExternalDisplay
|   +-- CoverActivity               (Container activity for external display)
|   +-- DisplayLifecycleManager     (Hinge fold, display sleep/wake, blackout timer)
|   +-- NavigationController        (Page stack, tabs, drawer states, predictive back)
|   +-- GestureController           (Directional swipe, vertical drawers, tap dispatch)
|
+-- UI
|   +-- Theme / DesignSystem        (Typography, colors, cards, icons, touch targets)
|   +-- Home                        (Modern Home Deck with clock, widgets, status)
|   +-- Clock                       (ClockFaceEngine: LCD, Modern Digital, Typographic, Analog)
|   +-- Widgets                     (Weather, Media, Battery, Calendar quick widgets)
|   +-- Applications                (External display App Launcher with cover compatibility filter)
|   +-- Notifications               (Modernized Notification Cards and Drawer)
|   +-- Music                       (Modern Media Deck with progress bar & scrubbing)
|   +-- Weather                     (Redesigned forecast card & hourly timeline)
|   +-- Calendar                    (Modern 7-day schedule view)
|   +-- Utilities                   (Timer, Alarm, Voice Recorder, Quick Controls)
|   +-- Assistant                   (AI Voice Assistant interface: waveform, transcription, TTS)
|   +-- Settings                    (Inside screen management and external display preferences)
|
+-- Services
|   +-- Media                       (MediaSession listener & transport bridges)
|   +-- Notifications               (NotificationListenerService integration)
|   +-- Calls                       (VoIP & Cellular call listener & CallOverlay)
|   +-- Alarm & Timer               (AlarmManager exact timers & RingService)
|   +-- Voice                       (RecorderService foreground audio capture)
|
+-- Integrations
|   +-- Motorola                    (LcdClockService, LcdSessionService, Binders)
|   +-- Gemini                      (AIProvider, GeminiClient, ConversationalSession)
|   +-- HomeAssistant               (REST API HTTPS client & entity management)
|   +-- AndroidSystem               (QuickControls, System settings, WG Tunnel)
|
+-- Data
    +-- Preferences                 (Prefs wrapper, clock style, assistant config)
    +-- Models                      (WeatherData, CallState, MediaItem, NotificationItem)
```

---

## 3. Phased Execution Roadmap

### Phase 1: Toolchain & Android 16 Compatibility
- Update `build.sh` to compile against `android-36` (`platforms/android-36/android.jar`).
- Update `AndroidManifest.xml` targetSdkVersion to 36.
- Add Window Insets and Edge-to-Edge handling for full-screen activities.
- Register `OnBackInvokedCallback` for modern back gesture support.
- Verify clean compilation with `build.sh`.

### Phase 2: Design System & Core Foundations
- Build `DesignSystem.java` with standardized colors, fonts, margins, card metrics, and touch target bounds for 368x194.
- Build `DisplayManager.java` and `DisplayLifecycleManager.java` to manage external display metrics dynamically.
- Build `ClockFaceEngine.java` supporting multiple clock face renderers (LCD 7-segment, Modern Digital, Minimalist Typographic, Analog).

### Phase 3: External Display Navigation & Home Deck Redesign
- Redesign `CoverActivity` and navigation coordinator:
  - Clean top bar (`CoverTabs`) with modern icon indicators.
  - New **Home Screen Deck** featuring primary clock style, battery status, weather badge, active media chip, and notification pill.
  - New **Application Launcher Page** displaying external-display compatible applications with launch intents and error handling.
  - Redesigned pull-down Quick Controls panel.
  - Redesigned pull-up Notification drawer.

### Phase 4: Feature Redesigns
- **Music Page**: Modernized layout with rounded album artwork, track/artist typography, transport buttons, and smooth touch-scrubbing progress bar.
- **Calls Panel**: Clean AMOLED incoming/ongoing call cards with large touch buttons (Answer, Decline, Mute, Hang Up) for cellular and VoIP.
- **Weather & Calendar Pages**: Refined visual presentation tailored to 368x194.
- **Timer & Alarm**: Streamlined touch-optimized controls with segment/digital digits.
- **Voice Recorder**: Sleek audio waveform visualizer and elapsed recording timer.
- **Home Assistant**: Modern status badges and multi-entity toggles.

### Phase 5: Independent AI Voice Assistant
- Create `AIProvider` interface and official `GeminiProvider` using Google Gemini REST API (`generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent`).
- Create `VoiceSessionManager`:
  - Speech-to-Text using Android `SpeechRecognizer` with tap-to-talk initiation.
  - Multi-turn conversation history management.
  - Text-to-Speech using Android `TextToSpeech` engine.
- Create `AssistantView` for the 368x194 external display:
  - Microphones state indicator.
  - Animated audio waveform.
  - Live transcription text.
  - AI response view with auto-scroll.
  - Spoken response controls (Mute/Stop/End Session).
  - Explicit error handling for network, API keys, and speech recognition errors.
- Secure API key configuration via `PreviewActivity` (persisted in encrypted/private SharedPreferences, never hardcoded).

### Phase 6: System State, Lifecycle & Battery Optimization
- Validate `ButtonPause` and `PipGuard` interactions with new navigation flow.
- Ensure all screen-off and hinge-open events cancel speech recognition, pause rendering loops, and release audio resources.
- Guarantee zero background battery drain when the external display is asleep.

### Phase 7: Settings & Verification
- Update `PreviewActivity` with configuration options for new clock styles, AI Assistant API key setup, voice settings, and app launcher management.
- Execute full clean compilation and packaging via `./build.sh`.
- Update complete project documentation.
