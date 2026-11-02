# Testing Strategy & Verification: Motorola Razr 40 External Display OS

## 1. Automated vs Physical Device Testing Matrix

Because the Motorola Razr 40 relies on specialized hardware (a physical hinge sensor, an external 368x194 OLED panel, and proprietary Motorola SystemUI extensions), tests are partitioned into:
1. **Automated / Host Verification**: Build integrity, DEX bytecode compliance, manifest verification, and resource link tests.
2. **Physical Device Verification**: Hinge telemetry, cover screen activity launching, accessibility overlays, and audio capture.

| Test Case | Scope | Execution Method | Expected Result |
| :--- | :--- | :--- | :--- |
| **Clean Toolchain Build** | Automated | `./build.sh` | Zero compilation errors; valid `lcd-clock.apk` produced. |
| **API 36 Bytecode Verification** | Automated | `$BT/d8 --min-api 33` | Valid `classes.dex` generated against `android-36/android.jar`. |
| **Manifest Parsing** | Automated | `aapt2 dump badging build/aligned.apk` | TargetSdk 36, package `dev.lcdclock`, permissions present. |
| **Clock Face Rendering** | Automated | Direct unit call `ClockFaceEngine.drawFace` | No exceptions; valid geometry for all 4 clock styles. |
| **FakePlayer Media Session** | Automated/ADB | `cd test/fakeplayer && ./build.sh install` | Active `MediaSession` registered with scrubbable metadata. |
| **Hinge Angle Fold Transition** | Physical Device | Close phone to 0° | External display wakes, `ButtonPause` logs "closed". |
| **Hinge Unfold Transition** | Physical Device | Open phone to >30° | `ButtonPause` dynamically unbinds accessibility service. |
| **PiP Expansion on Fold** | Physical Device | Play YouTube in PiP, fold phone | Video expands to full screen; audio continues playing. |
| **Gemini AI Tap-to-Talk** | Physical Device | Tap mic on Gemini tab | Pulsing waveform appears; speaks audio response via TTS. |
| **App Launcher Launch Intent** | Physical Device | Tap app icon in Apps tab | Launches on external display or shows "Requires opening phone". |
| **VoIP/Cellular Call Overlay** | Physical Device | Receive phone/WhatsApp call | Full-screen AMOLED call card overrides Moto stock screen. |

---

## 2. Regression Testing Checklist

Execute these checks after any major subsystem modification:

### A. Clock Faces & Home Deck
- [ ] LCD Digits render with unlit segments at 25% alpha.
- [ ] Minimal Digital clock renders crisp white time with colored AM/PM.
- [ ] Typographic clock renders stacked bold numerals.
- [ ] Analog clock renders smooth tick marks, hour hand, and minute hand.
- [ ] Battery percentage and bolt icon update on charger plug/unplug.
- [ ] Weather and Music glance chips navigate to their respective pages.

### B. Navigation & Swipe Gestures
- [ ] Horizontal swipe smoothly slides between carousel tabs.
- [ ] Top tab bar shows active indicator pill on the selected icon.
- [ ] Swipe up on Home Deck opens Notifications drawer.
- [ ] Swipe down on Home Deck opens Quick Settings grid.
- [ ] Predictive Back gesture (or sideways swipe on panels) returns to Home Deck.

### C. AI Voice Assistant
- [ ] Tapping microphone prompts for audio permission if not yet granted.
- [ ] Speech waveform visualizer pulses in response to user voice.
- [ ] Transcription displays interim recognized speech.
- [ ] Gemini API returns concise 1-2 sentence response.
- [ ] `TextToSpeech` speaks the response aloud over phone speaker.
- [ ] Tapping "End Session" clears conversational context.
- [ ] Folding/unfolding phone or turning off screen immediately cancels speech capture.

### D. System Integrations
- [ ] Music seekbar allows dragging to scrub track position.
- [ ] Timer counts down and triggers `RingService` alarm ringtone.
- [ ] Voice recorder writes valid AAC `.m4a` file to `MediaStore`.
- [ ] Quick controls toggle Flashlight, SOS, DND, and sound modes.
- [ ] Banking apps do not detect running accessibility service when phone is open.
