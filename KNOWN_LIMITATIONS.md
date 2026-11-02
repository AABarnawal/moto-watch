# Known Limitations & Platform Constraints: Motorola Razr 40

This document records physical hardware constraints, Android OS restrictions, and OEM firmware boundaries relevant to the project.

---

## 1. Hardware & Display Constraints

- **Small Viewport Geometry**:
  The physical external display is 1.5 inches with a resolution of **368 × 194 pixels** (approx. 1.9:1 aspect ratio). Complex smartphone layouts cannot simply be scaled down; all custom views must be hand-tailored to this viewport.
- **Single-Touch Limitations**:
  While the capacitive digitizer supports touch, multi-finger gestures (e.g. pinch-to-zoom) are unwieldy on a 1.5" glass surface. Interactions are intentionally limited to single-finger taps, directional swipes, and drag scrolling.
- **Fixed Display ID**:
  The secondary display is assigned Display ID `1` by Motorola's display manager. If an external monitor or wireless display is connected, display enumeration indices should be dynamically confirmed via `DisplayManager.getDisplays()`.

---

## 2. Android Platform Restrictions

- **System Radio Toggles**:
  Android security policies strictly prohibit third-party applications from toggling Wi-Fi, Mobile Data, or Bluetooth without system-signature privileges (`android.permission.MODIFY_PHONE_STATE` or `NETWORK_SETTINGS`). Therefore, Quick Controls provides quick access to Flashlight, SOS, DND, Ringer Mode, Media Volume, and WireGuard, but routes network toggles to system settings.
- **Microphone Privacy Indicator**:
  In Android 14, 15, and 16, using `SpeechRecognizer` or `RecorderService` displays the prominent green Android privacy pill in the status bar. This cannot and should not be bypassed. Background listening without explicit user interaction is prohibited by Android's security model.
- **Background Activity Launches (BAL)**:
  Starting activities from background services is strictly gated. The app uses `ButtonService` (an Accessibility Service) which holds an approved platform exemption to launch `CoverActivity` onto Display 1 when the cover screen wakes or volume buttons are held.

---

## 3. Motorola OEM Firmware Behaviors

- **SystemUI Picture-in-Picture Dismissal**:
  Motorola's `SystemUI` has a hardcoded `FoldStateListener` inside `PipController` that dismisses PiP windows when the phone folds shut. `PipGuard` circumvents this by expanding PiP to fullscreen right before the hinge angle reaches 15°, allowing background audio playback to continue. If Motorola modifies this internal SystemUI behavior in a future firmware update, `PipGuard`'s accessibility action hook may require adjustment.
- **System Call Screen Layering**:
  When a cellular or VoIP call rings, Motorola's stock call screen appears on an overlay window above regular activities. `CallOverlay` uses `TYPE_ACCESSIBILITY_OVERLAY` via `ButtonService` to draw above this system window.
- **Undocumented Clock Face Plugin**:
  `LcdClockService` and `LcdSessionService` implement undocumented Motorola Clock Face interfaces (`com.motorola.clockface.sdk.*`). If Motorola deprecates or modifies this plugin mechanism in a future OTA update, `CoverActivity` will continue operating autonomously as an external display launcher.
