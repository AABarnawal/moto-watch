# Changelog: Motorola Razr 40 External Display OS Experience

All notable changes to this project are documented in this file.

---

## [2.0.0] - Android 16 Modernization & Feature Expansion

### Added
- **Android 16 (API 36) Support**:
  - Upgraded target SDK to API 36 in `AndroidManifest.xml`.
  - Upgraded build toolchain in `build.sh` and `test/fakeplayer/build.sh` to compile against `android-36/android.jar` with `build-tools/37.0.0`.
  - Added Android 16 Predictive Back support (`android:enableOnBackInvokedCallback="true"` and `OnBackInvokedDispatcher`).
  - Implemented Window Insets consumption listener for true edge-to-edge external display rendering.
- **Unified Dark AMOLED Design System (`DesignSystem.java`)**:
  - Standardized color tokens, surface card elevations (`0xFF121214`), and 1px border outlines.
  - Standardized interactive pill buttons, status badges, and typography scales for 368 × 194 pixels.
- **Unified Clock Face Engine (`ClockFaceEngine.java`)**:
  - **STYLE_LCD**: Signature 7-segment digital look with ghost segments and blinking colon.
  - **STYLE_MINIMAL**: Clean, modern bold sans-serif digital clock.
  - **STYLE_TYPOGRAPHIC**: Stacked geometric numerals with dual-color accents.
  - **STYLE_ANALOG**: Minimalist dark analog watch face with hour/minute ticks and hands.
  - Clock face style selector in `PreviewActivity`.
- **Redesigned Home Screen Deck (`HomeDeckView.java`)**:
  - Dynamic time display with selected clock face.
  - Top status row with battery percentage, charging lightning bolt, alarm status, and notification dot.
  - Bottom glanceable widget chips for Weather (temperature + city) and active Music playback (track title).
  - Tap shortcuts on glance chips directly opening Weather and Music pages.
- **External Display Application Launcher (`AppsView.java`)**:
  - Scrollable application grid displaying installed launcher applications with icons and labels.
  - Multi-display intent launch targeting external display (`CoverScreen.DISPLAY_ID`).
  - Graceful fallback and user alerts for applications requiring the internal main display.
- **Independent AI Voice Assistant (`AssistantView.java`, `AIProvider.java`, `GeminiProvider.java`, `VoiceSessionManager.java`)**:
  - Tap-to-talk speech recognition powered by Android `SpeechRecognizer`.
  - Official Google Gemini REST API integration (`gemini-1.5-flash`) with multi-turn conversation context.
  - Real-time 10-bar audio waveform visualizer and pulsing microphone indicator.
  - Spoken audio responses via Android `TextToSpeech` engine.
  - Multi-line auto-scrolling response presentation and session control actions.
  - Secure API key configuration card in `PreviewActivity`.
- **Documentation Suite**:
  - Added `ARCHITECTURE.md`, `ARCHITECTURE_AUDIT.md`, `ANDROID_16_COMPATIBILITY.md`, `UI_DESIGN_SYSTEM.md`, `FEATURE_INVENTORY.md`, `AI_ASSISTANT.md`, `PERMISSIONS.md`, `BUILD_AND_INSTALL.md`, `TESTING.md`, `REFACTORING_PLAN.md`, `RISK_REGISTER.md`, and `KNOWN_LIMITATIONS.md`.

### Changed
- Refactored `CoverActivity.java` to host new `HomeDeckView`, `AppsView`, and `AssistantView` pages.
- Updated `Prefs.java` with new tab definitions, hold action mappings, and persistent clock style storage.
- Updated `TabIcons.java` with custom vector geometry icons for Apps (grid squares) and Gemini (AI sparkle star).
- Updated `res/values/strings.xml` with clock style names, assistant settings, and tab labels.

### Preserved
- Preserved Motorola Clock Face plugin service integration (`LcdClockService`, `LcdSessionService`, `LcdOptionsProvider`, `Binders`).
- Preserved `ButtonPause` dynamic accessibility unbinding to protect against banking app RASP detection.
- Preserved `PipGuard` clamshell fold velocity detection and SystemUI PiP expansion logic.
- Preserved universal call notification interception (`Calls.java`) and `CallOverlay.java`.
