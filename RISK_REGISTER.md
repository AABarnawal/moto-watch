# Risk Register: Motorola Razr 40 External Display OS Experience

This document identifies technical, operational, and compatibility risks across the project, rating their likelihood, impact, and outlining mitigation strategies.

---

## 1. Risk Matrix

| Risk ID | Category | Description | Likelihood | Impact | Mitigation Strategy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **RSK-01** | OEM Firmware | Motorola updates system clock face plugin IPC or deprecates `BIND_CLOCK_FACE_SERVICE`. | Medium | High | Decouple `CoverActivity` completely from `LcdClockService`. If the clock plugin fails, `CoverActivity` runs independently as an external display launcher. |
| **RSK-02** | Android 16 | Background Activity Launch (BAL) restrictions block `ButtonService` launching `CoverActivity`. | Medium | High | Set `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED`. Keep accessibility service bindings active and ensure display IDs are explicitly validated. |
| **RSK-03** | Android 16 | Edge-to-edge default enforcement causes content clipping on the 368x194 display. | High | Medium | Implement `View.setOnApplyWindowInsetsListener` on `CoverActivity` root to consume system insets and enforce dynamic safe area bounds. |
| **RSK-04** | Security / RASP | Sideloaded app flagged by banking apps (e.g. ThreatMetrix, HSBC) due to accessibility service. | High | Critical | Preserve and test `ButtonPause`: unregister accessibility service on hinge open (>30°), keep disabled if banking app is in recents via `DUMP` & `PACKAGE_USAGE_STATS`. |
| **RSK-05** | PiP Behavior | Motorola updates SystemUI, altering PiP accessibility actions and breaking `PipGuard`. | Medium | Medium | Wrap accessibility tree search in defensive null/error checks; fallback to standard background playback if expand action fails. |
| **RSK-06** | AI Assistant | SpeechRecognizer fails or requires internal screen unlock when phone is closed. | Medium | Medium | Tap-to-talk initiation while `CoverActivity` is visibly resumed; clear UI fallback and error messaging ("Unlock or unfold phone for speech input" if recognition service is unavailable). |
| **RSK-07** | AI Assistant | Gemini API keys missing, invalid, or rate-limited. | High | Low | Clear configuration prompt in `PreviewActivity` setup card; graceful UI error states with actionable user hints. Never hardcode credentials. |
| **RSK-08** | Performance | Continuous rendering or speech recognition causes battery drain on small 1.5" display. | Medium | High | Immediately abort recognition, stop TTS, and halt animation runnables on `ACTION_SCREEN_OFF` or hinge unfold. Maintain event-driven UI updates. |
| **RSK-09** | App Launcher | Non-optimized apps crash or display unusable UI when launched on 368x194 cover display. | High | Medium | Inspect activity metadata and intent compatibility before launching; display clear "Requires main display" toast and handle launch exceptions safely. |
| **RSK-10** | Build Toolchain | Build script fails if host environment lacks specific Android SDK platform version. | Low | High | Upgrade `build.sh` to auto-detect highest installed Android SDK platform (`android-36` down to `android-33`) and build-tools dynamically. |

---

## 2. Testing & Verification Checklist for Risks
- **OEM Independence**: Verify application boots and `CoverActivity` functions even if Motorola Clock styles are not selected.
- **Physical Clamshell States**: Verify transitions between closed, half-folded (tent mode), and fully open states.
- **Banking App Isolation**: Verify `ButtonPause` logs "cover button: paused" when phone unfolds and does not awaken while target banking apps are running.
- **Microphone & Privacy**: Verify green privacy indicator appears only during active tap-to-talk listening and disappears immediately when recognition concludes.
