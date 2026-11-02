# UI Design System: Motorola Razr 40 External Display

## 1. Design Principles

The visual experience is designed specifically for the Motorola Razr 40's 1.5-inch external OLED display (**368 × 194 pixels**, ~282 PPI).

1. **Dark AMOLED Aesthetic**: Pure black backgrounds (`0xFF000000`) eliminate subpixel illumination, blending seamlessly with the physical bezels and conserving battery.
2. **High Information Contrast**: Crisp typography with deliberate typographic hierarchy ensures critical metrics (time, battery, upcoming alerts) are legible at arm's length.
3. **Touch-First Target Sizing**: On a 1.5-inch screen, controls must have generous touch hitboxes (minimum 36dp equivalent) with visual feedback on press.
4. **Glanceable Hierarchy**: Dense, nested mobile interfaces are replaced with direct, glanceable cards and horizontal carousel decks.
5. **No Decorative Bloat**: Avoids heavy gradients, unnecessary drop shadows, and complex nested navigation.

---

## 2. Design Tokens & Color Palette

| Token | Hex Value | Usage |
| :--- | :--- | :--- |
| `COLOR_BG` | `#000000` | AMOLED root display canvas |
| `COLOR_CARD` | `#121214` | Secondary card background |
| `COLOR_CARD_ELEVATED` | `#1C1C20` | Highlighted cards, dialogs, active items |
| `COLOR_CARD_PRESSED` | `#2A2A30` | Visual tap/press state feedback |
| `COLOR_BORDER` | `#2A2A2E` | Card boundaries & subtle dividers |
| `COLOR_TEXT_PRIMARY` | `#FFFFFF` | Primary headings, clock digits, values |
| `COLOR_TEXT_SECONDARY` | `#9E9EA4` | Subtitles, status text, descriptions |
| `COLOR_TEXT_TERTIARY` | `#636366` | Timestamps, inactive indicators |
| `COLOR_ACCENT_AQUA` | `#00D2FF` | Media playing, notifications, primary links |
| `COLOR_ACCENT_GREEN` | `#30D158` | Call answer, battery normal, success |
| `COLOR_ACCENT_AMBER` | `#FF9F0A` | Alarm warnings, timer alert, battery low |
| `COLOR_ACCENT_RED` | `#FF453A` | Call decline, clear all, errors, recording |
| `COLOR_ACCENT_PURPLE` | `#A855F7` | AI Voice Assistant brand, waveforms |

---

## 3. Typography Hierarchy

- **Hero Digital Clock**: 44% - 54% of screen height (`bh * 0.44f - bh * 0.54f`), Bold Sans-Serif or 7-Segment DSEG.
- **Card Headings**: 9% of screen height (`bh * 0.09f`), Semi-Bold Sans-Serif.
- **Body & Captions**: 7.5% of screen height (`bh * 0.075f`), Regular Sans-Serif.
- **Glance Chips & Badges**: 7.0% of screen height (`bh * 0.070f`), Bold Sans-Serif.

---

## 4. Reusable Surface Components

### 4.1 Surface Cards
Implemented via `DesignSystem.drawCard(...)`:
- Rounded rectangles with 6px, 10px, or 14px corner radii.
- Filled with `COLOR_CARD` and stroked with a 1px `COLOR_BORDER` outline.

### 4.2 Interactive Pills & Action Chips
Implemented via `DesignSystem.drawPill(...)`:
- Fully rounded pills used for quick triggers, audio scrubbing knobs, and glanceable chips on the Home Deck.
- Touch feedback alters fill to `COLOR_CARD_PRESSED` immediately on `ACTION_DOWN`.

### 4.3 Waveforms & Voice Feedback
Implemented via `AssistantView` and `DesignSystem`:
- 10-bar dynamic visualizer pulsing with real-time RMS audio dB input from `SpeechRecognizer`.
- Rippling circular microphone indicator providing tactile feedback via `VibrationEffect.EFFECT_CLICK`.
