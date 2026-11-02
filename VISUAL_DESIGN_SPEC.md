# Visual Design Specification: Motorola Razr 40 External Display OS

## 1. Design Direction: Modern Minimalist / Nothing OS / High-End AMOLED

The redesigned Motorola Razr 40 external display interface (368 × 194 pixels) adopts an original, modern design language:

```
+-----------------------------------------------------------------------------------------+
|                                  VISUAL IDENTITY SYSTEM                                 |
|                                                                                         |
|   +-----------------------+   +------------------------+   +------------------------+   |
|   | Pure Pitch Black      |   | High-Contrast Layering |   | Precision Typography   |   |
|   | #000000 AMOLED Base   |   | Glass Cards & Contours |   | Large Balanced Metrics |   |
|   +-----------------------+   +------------------------+   +------------------------+   |
|                                                                                         |
|   +-----------------------+   +------------------------+   +------------------------+   |
|   | Dynamic Accent Lights |   | Micro-Haptic Touch     |   | Glaceable Information  |   |
|   | Emerald, Amber, Ruby  |   | Tactile Target Hitboxes|   | 368x194 Native Layouts |   |
|   +-----------------------+   +------------------------+   +------------------------+   |
+-----------------------------------------------------------------------------------------+
```

---

## 2. Global Tokens & Layout Matrix

| Token | Value | Description |
| :--- | :--- | :--- |
| `COLOR_BG` | `#000000` | True Pitch Black for OLED efficiency |
| `SURFACE_CARD` | `#111114` | Primary card background |
| `SURFACE_ELEVATED` | `#18181D` | Elevated interaction layer |
| `SURFACE_PRESSED` | `#262630` | Touch feedback highlight |
| `BORDER_SUBTLE` | `#1F1F24` | 1px hairline card contour |
| `BORDER_FOCUS` | `#3A3A44` | Active state contour |
| `TEXT_PRIMARY` | `#FFFFFF` | 100% white, highest hierarchy |
| `TEXT_SECONDARY` | `#9B9BA1` | Muted subtitle text |
| `TEXT_TERTIARY` | `#5E5E66` | Timestamps & inactive items |
| `ACCENT_CYAN` | `#00E5FF` | Primary interface accent / media |
| `ACCENT_EMERALD` | `#10B981` | Answer, running timer, full battery |
| `ACCENT_AMBER` | `#F59E0B` | Warning, alarm, snooze, low power |
| `ACCENT_RUBY` | `#EF4444` | Decline, stop, recording, critical alert |
| `ACCENT_PURPLE` | `#A855F7` | AI Voice Assistant brand |

---

## 3. Screen-by-Screen Specification (368 × 194 Viewport)

### 3.1 Home Screen & Clock Styles (`HomeDeckView.java`, `ClockFaceEngine.java`)
- **Viewport Layout**:
  - `y: 0 - 24`: Floating status bar (Battery pill with progress bar, lock icon, unread notification dot).
  - `y: 24 - 150`: Hero Clock composition in chosen style.
  - `y: 150 - 194`: Dual glance capsules: Left Weather pill (`22° ⛅ London`), Right Music chip (`▶ Track - Artist`).
- **Clock Faces**:
  1. **Luxury Minimal**: Elegant high-fashion numerals with 1px hairline divider and small-caps date.
  2. **Modern Editorial**: Stacked asymmetric bold display numerals with vertical date column.
  3. **Digital Precision**: Nothing OS-inspired precision matrix layout with technical indicators.
  4. **Futuristic AMOLED**: Gauge-inspired concentric radial arc with center digital time.

### 3.2 Music Player (`MusicView.java`)
- **Viewport Layout**:
  - Left (`x: 12, y: 32, w: 90, h: 90`): Rounded square album art card with 12px squircle corners.
  - Right (`x: 110, y: 32, w: 246, h: 90`):
    - Track title in bold white marquee text.
    - Artist name in soft gray.
    - 3px micro-track progress bar with glowing cyan knob and timestamp counters (`1:24 / 3:45`).
  - Bottom (`y: 132 - 184`): Floating control dock with rounded pill buttons for Previous (`⏮`), Play/Pause circle (`▶ / ❚❚`), and Next (`⏭`).

### 3.3 Weather (`WeatherView.java`)
- **Viewport Layout**:
  - Left (`x: 12, w: 160`): Large 48sp hero temperature (`22°`), weather condition badge (`Partly Cloudy`), location pill (`London`).
  - Right (`x: 180, w: 176`): 4-column hourly timeline cards (`15:00 22°`, `16:00 21°`, `17:00 19°`, `18:00 18°`) with precipitation bar.

### 3.4 Notifications (`NotificationsView.java`)
- **Viewport Layout**:
  - Stacked cards with 10px rounded corners and subtle 1px border.
  - Card Header: App icon (20×20), app name, and relative timestamp (`2m ago`).
  - Card Body: Bold sender name and 2-line wrapped message preview.
  - Action row: Dismiss pill (`×`) and global "Clear All" pill button.

### 3.5 Calendar (`CalendarView.java`)
- **Viewport Layout**:
  - Left: Day badge pill (`WED 30`) with calendar accent bar.
  - Right: Scrollable event cards with time range badge (`10:30 - 11:30`) and event title in bold white.

### 3.6 Timer & Alarm (`TimerView.java`, `AlarmView.java`)
- **Timer**:
  - Center: Large 56sp countdown digits (`04:59`) with circular/horizontal progress bar.
  - Preset row: `+1m`, `+5m`, `+10m` pill buttons.
  - Bottom dock: Dual buttons: Start/Pause (Emerald) and Reset (Subtle Card).
- **Alarm**:
  - Hero alarm time with toggle switch capsule.
  - Ringing state: Fullscreen alert card with massive "Snooze" and "Stop" buttons.

### 3.7 Voice Recorder (`RecorderView.java`)
- **Viewport Layout**:
  - Center: 24-bar live audio waveform visualizer responding to mic input.
  - Top: High-precision elapsed time counter (`00:14.8`) in red accent.
  - Bottom: Floating record/stop button (pulsing red pill) and saved recordings library link.

### 3.8 Quick Settings (`QuickView.java`)
- **Viewport Layout**:
  - 2×3 grid of rounded interactive control tiles: Torch, SOS, DND, Sound Mode, Battery Saver, VPN.
  - Active tiles illuminate with cyan/amber accent backgrounds.

### 3.9 Application Launcher (`AppsView.java`)
- **Viewport Layout**:
  - 2-column grid of sleek app cards with squircle icons and high-contrast labels.
  - Smooth vertical drag scrolling with safe display targeting.

### 3.10 Call Interface (`CallView.java`)
- **Viewport Layout**:
  - Incoming: Caller avatar with pulsing ring, caller name in large bold display, app badge, large green Answer and red Decline pill buttons.
  - Ongoing: In-call timer, Mute toggle pill, and red Hang Up button.
