package dev.lcdclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Clock Face Engine providing 10 radically distinct, creative watch faces:
 * 1. STYLE_TERMINAL: Hacker Matrix CLI (phosphor green CRT terminal with matrix rain & cursor)
 * 2. STYLE_DOT_MATRIX: Active Dot Matrix LED (Braun / Nothing OS circular LED hardware matrix)
 * 3. STYLE_EDITORIAL: Modern Editorial (asymmetric high-fashion bold split-column typography)
 * 4. STYLE_SPLIT_FLAP: Split-Flap Departure Board (mechanical airport flipper board with split seams)
 * 5. STYLE_NIXIE: Nixie Vacuum Tubes (Soviet IN-14 steampunk glowing fiery glass tubes)
 * 6. STYLE_GAMEBOY: Game Boy Classic (1989 Nintendo DMG-01 olive LCD screen with pixel art)
 * 7. STYLE_BLUEPRINT: Architectural CAD Blueprint (cyanotype drafting grid with dimension callouts)
 * 8. STYLE_HEX_HUD: Cyberpunk Hex Cockpit (spaceship cockpit HUD with radar sweep & hex telemetry)
 * 9. STYLE_TURNTABLE: Vinyl Hi-Fi Turntable (spinning vinyl record with tonearm & stereo VU meters)
 * 10. STYLE_CASSETTE: 80s Audio Cassette Tape (vintage cassette tape with spinning spools & mixtape label)
 */
public final class ClockFaceEngine {
    public static final String STYLE_TERMINAL = "terminal";      // Hacker Matrix CLI
    public static final String STYLE_DOT_MATRIX = "dot_matrix";  // Active Dot Matrix LED
    public static final String STYLE_EDITORIAL = "typographic";  // Modern Editorial
    public static final String STYLE_SPLIT_FLAP = "split_flap";  // Split-Flap Departure Board
    public static final String STYLE_NIXIE = "nixie";            // Nixie Vacuum Tubes
    public static final String STYLE_GAMEBOY = "gameboy";        // Game Boy Classic (1989)
    public static final String STYLE_BLUEPRINT = "blueprint";    // Architectural CAD Blueprint
    public static final String STYLE_HEX_HUD = "hex_hud";        // Cyberpunk Hex Cockpit
    public static final String STYLE_TURNTABLE = "turntable";    // Vinyl Hi-Fi Turntable
    public static final String STYLE_CASSETTE = "cassette";      // 80s Audio Cassette Tape

    // Legacy style aliases for backward compatibility
    public static final String STYLE_GLASS = "glass";
    public static final String STYLE_BRUTALIST = "brutalist";
    public static final String STYLE_CLAY = "clay";
    public static final String STYLE_TILING = "tiling";
    public static final String STYLE_LUXURY = "minimal";
    public static final String STYLE_PRECISION = "lcd";
    public static final String STYLE_FUTURISTIC = "futuristic";
    public static final String STYLE_ANALOG = STYLE_HEX_HUD;
    public static final String STYLE_MINIMAL = STYLE_EDITORIAL;
    public static final String STYLE_TYPOGRAPHIC = STYLE_EDITORIAL;
    public static final String STYLE_LCD = STYLE_DOT_MATRIX;

    public static final String[] ALL_STYLES = {
            STYLE_TERMINAL, STYLE_DOT_MATRIX, STYLE_EDITORIAL,
            STYLE_SPLIT_FLAP, STYLE_NIXIE, STYLE_GAMEBOY,
            STYLE_BLUEPRINT, STYLE_HEX_HUD, STYLE_TURNTABLE,
            STYLE_CASSETTE
    };

    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final Rect RECT = new Rect();
    private static final RectF RECT_F = new RectF();

    // 5x7 bitmap font for digits 0-9
    private static final byte[][] FONT_5X7 = {
            {0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E}, // 0
            {0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E}, // 1
            {0x0E, 0x11, 0x01, 0x06, 0x08, 0x10, 0x1F}, // 2
            {0x1E, 0x01, 0x01, 0x0E, 0x01, 0x01, 0x1E}, // 3
            {0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02}, // 4
            {0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E}, // 5
            {0x0E, 0x10, 0x1E, 0x11, 0x11, 0x11, 0x0E}, // 6
            {0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08}, // 7
            {0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E}, // 8
            {0x0E, 0x11, 0x11, 0x0F, 0x01, 0x01, 0x0E}  // 9
    };

    public static String styleName(Context context, String style) {
        String[] names = context.getResources().getStringArray(R.array.clock_style_names);
        for (int i = 0; i < ALL_STYLES.length && i < names.length; i++) {
            if (ALL_STYLES[i].equals(style)) {
                return names[i];
            }
        }
        return names[0];
    }

    private ClockFaceEngine() {
    }

    /**
     * Renders the chosen clock face style into the bounding box (bx, by, bw, bh).
     */
    public static void drawFace(Canvas canvas, Context context, String style,
                                float bx, float by, float bw, float bh,
                                Calendar now, boolean is24Hour, int color,
                                boolean alarmSet, boolean locked,
                                SegmentPainter segmentPainter) {
        if (STYLE_TERMINAL.equals(style)) {
            drawHackerTerminal(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_DOT_MATRIX.equals(style) || "lcd".equals(style)) {
            drawDotMatrixLed(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_EDITORIAL.equals(style) || "minimal".equals(style) || "glass".equals(style) || "swiss".equals(style)) {
            drawModernEditorial(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_SPLIT_FLAP.equals(style) || "tiling".equals(style) || "brutalist".equals(style)) {
            drawSplitFlapBoard(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_NIXIE.equals(style) || "neon_outline".equals(style)) {
            drawNixieTubes(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_GAMEBOY.equals(style) || "arcade".equals(style)) {
            drawGameBoyClassic(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_BLUEPRINT.equals(style) || "bauhaus".equals(style)) {
            drawBlueprintDraft(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_HEX_HUD.equals(style) || "futuristic".equals(style) || "analog".equals(style) || "automotive".equals(style)) {
            drawCyberHexHud(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_TURNTABLE.equals(style) || "aura".equals(style)) {
            drawVinylTurntable(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else if (STYLE_CASSETTE.equals(style) || "synthwave".equals(style) || "clay".equals(style) || "prism".equals(style)) {
            drawAudioCassette(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        } else {
            drawHackerTerminal(canvas, bx, by, bw, bh, now, is24Hour, color, alarmSet, locked);
        }
    }

    /**
     * 1. Hacker Matrix CLI:
     * Phosphor green CRT terminal with live matrix code streams, terminal prompt, and blinking cursor.
     */
    private static void drawHackerTerminal(Canvas canvas, float bx, float by, float bw, float bh,
                                          Calendar now, boolean is24Hour, int color,
                                          boolean alarmSet, boolean locked) {
        // Matrix falling code streams on right margin
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setTypeface(Typeface.MONOSPACE);
        PAINT.setFontFeatureSettings(null);
        PAINT.setTextSize(bh * 0.08f);

        long t = System.currentTimeMillis();
        int shift = (int) ((t / 300) % 8);
        String[] codeLines = {"0101", "A9F2", "X7C0", "882B", "1011", "D4E9", "00FF", "CA77"};

        for (int col = 0; col < 3; col++) {
            float streamX = bx + bw * (0.76f + col * 0.08f);
            for (int row = 0; row < 5; row++) {
                int charIdx = (shift + col * 2 + row) % codeLines.length;
                PAINT.setColor((row == 4) ? 0xAA00FF66 : 0x4000FF66);
                canvas.drawText(codeLines[charIdx].substring(0, 1), streamX, by + bh * (0.22f + row * 0.14f), PAINT);
            }
        }

        // Top terminal prompt
        PAINT.setColor(0xFF15803D); // Dim terminal green
        PAINT.setTextSize(bh * 0.085f);
        canvas.drawText("usr@razr40:~# ./chronos --tnum", bx + bw * 0.06f, by + bh * 0.18f, PAINT);

        // Phosphor green terminal time
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, min);

        boolean cursorBlink = (now.get(Calendar.MILLISECOND) < 500);

        PAINT.setTextSize(bh * 0.46f);
        PAINT.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");
        PAINT.setColor(0xFF00FF66); // Bright phosphor green

        float tw = PAINT.measureText(timeStr);
        float tx = bx + bw * 0.06f;
        float ty = by + bh * 0.62f;
        canvas.drawText(timeStr, tx, ty, PAINT);

        // Blinking solid block terminal cursor
        if (cursorBlink) {
            float curX = tx + tw + 6f;
            float curW = bh * 0.16f;
            float curH = bh * 0.36f;
            canvas.drawRect(curX, ty - curH + 4f, curX + curW, ty + 4f, PAINT);
        }

        // Bottom status telemetry
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd // EEE", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setTextSize(bh * 0.082f);
        PAINT.setTypeface(Typeface.MONOSPACE);
        PAINT.setFontFeatureSettings(null);
        PAINT.setColor(0xFF4ADE80);
        canvas.drawText("> CORE: 32°C // PWR: 88%", bx + bw * 0.06f, by + bh * 0.76f, PAINT);
        canvas.drawText("> CHRONO: " + dateStr, bx + bw * 0.06f, by + bh * 0.88f, PAINT);
    }

    /**
     * 2. Active Dot Matrix LED:
     * Nothing OS / Braun vintage 5x7 circular LED hardware dot matrix with authentic ghosted inactive LEDs.
     */
    private static void drawDotMatrixLed(Canvas canvas, float bx, float by, float bw, float bh,
                                        Calendar now, boolean is24Hour, int color,
                                        boolean alarmSet, boolean locked) {
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);

        int[] digits = {
                hour / 10,
                hour % 10,
                min / 10,
                min % 10
        };

        float dotR = bh * 0.024f;
        float step = dotR * 2.6f;
        float digitW = 5 * step;
        float digitGap = 1.6f * step;
        float colonW = 1.4f * step;

        float totalW = digitW * 4f + digitGap * 2f + colonW;
        float startX = bx + (bw - totalW) / 2f;
        float startY = by + bh * 0.16f;

        PAINT.setStyle(Paint.Style.FILL);

        float curX = startX;
        for (int d = 0; d < 4; d++) {
            if (d == 2) {
                // Colon between hour and minute
                float colCenter = curX + colonW / 2f;
                for (int r = 0; r < 7; r++) {
                    PAINT.setColor(0x14FFFFFF);
                    canvas.drawCircle(colCenter, startY + r * step, dotR, PAINT);
                }
                // Active colon dots at row 2 and row 4
                PAINT.setColor(0x33FFFFFF);
                canvas.drawCircle(colCenter, startY + 2 * step, dotR * 1.5f, PAINT);
                canvas.drawCircle(colCenter, startY + 4 * step, dotR * 1.5f, PAINT);
                PAINT.setColor(0xFFFFFFFF);
                canvas.drawCircle(colCenter, startY + 2 * step, dotR, PAINT);
                canvas.drawCircle(colCenter, startY + 4 * step, dotR, PAINT);
                curX += colonW;
            }

            int val = Math.max(0, Math.min(9, digits[d]));
            byte[] pattern = FONT_5X7[val];

            for (int r = 0; r < 7; r++) {
                int rowBits = pattern[r];
                for (int c = 0; c < 5; c++) {
                    float cx = curX + c * step;
                    float cy = startY + r * step;
                    boolean on = ((rowBits >> (4 - c)) & 1) == 1;

                    if (on) {
                        PAINT.setColor(0x33FFFFFF);
                        canvas.drawCircle(cx, cy, dotR * 1.6f, PAINT);
                        PAINT.setColor(0xFFFFFFFF);
                        canvas.drawCircle(cx, cy, dotR, PAINT);
                    } else {
                        PAINT.setColor(0x14FFFFFF);
                        canvas.drawCircle(cx, cy, dotR, PAINT);
                    }
                }
            }
            curX += digitW + (d != 1 ? digitGap : 0);
        }

        // Dot matrix date below
        SimpleDateFormat df = new SimpleDateFormat("EEE · dd MMM yyyy", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setTextSize(bh * 0.09f);
        PAINT.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        PAINT.setFontFeatureSettings(null);
        PAINT.setColor(0xCCFFFFFF);
        float dw = PAINT.measureText(dateStr);
        canvas.drawText(dateStr, bx + (bw - dw) / 2f, startY + 8f * step + bh * 0.08f, PAINT);
    }

    /**
     * 3. Modern Editorial:
     * High-contrast bold editorial numerals with overlapping last hour digit,
     * knockout drop shadow over minutes, and vertical date strip.
     */
    private static void drawModernEditorial(Canvas canvas, float bx, float by, float bw, float bh,
                                            Calendar now, boolean is24Hour, int color,
                                            boolean alarmSet, boolean locked) {
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);

        String hStr = String.format(Locale.getDefault(), "%02d", hour);
        String mStr = String.format(Locale.getDefault(), "%02d", min);

        String h1 = hStr.substring(0, 1);
        String h2 = hStr.substring(1, 2);
        String m1 = mStr.substring(0, 1);
        String m2 = mStr.substring(1, 2);

        float textSize = bh * 0.56f;
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setTextSize(textSize);
        PAINT.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");

        float h1W = PAINT.measureText(h1);
        float h2W = PAINT.measureText(h2);
        float m1W = PAINT.measureText(m1);
        float m2W = PAINT.measureText(m2);

        float hStartX = bx + bw * 0.06f;
        float h1X = hStartX;
        float h2X = h1X + h1W;

        // Overlap: minutes start BEFORE h2 finishes, creating overlapping typography
        float overlapAmount = h2W * 0.35f;
        float mStartX = h2X + h2W - overlapAmount;
        float m1X = mStartX;
        float m2X = m1X + m1W;
        float ty = by + bh * 0.63f;

        // 1. Draw Minutes (Background layer) in crisp white
        PAINT.setColor(0xFFFFFFFF);
        canvas.drawText(m1, m1X, ty, PAINT);
        canvas.drawText(m2, m2X, ty, PAINT);

        // 2. Overlap knockout cutout / shadow for h2 overlapping m1:
        // Bold black cutout outline around h2 so it cleanly cuts through m1
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(12f);
        STROKE.setColor(0xFF000000); // Pitch-black knockout cutout
        STROKE.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        STROKE.setFontFeatureSettings("tnum");
        STROKE.setTextSize(textSize);
        canvas.drawText(h2, h2X, ty, STROKE);

        // Dark editorial drop shadow under h2 onto m1 for rich visual depth
        PAINT.setColor(0xEE000000);
        canvas.drawText(h2, h2X + 4f, ty + 4f, PAINT);

        // 3. Draw Hours on top (Foreground layer)
        int accentColor = color != 0 ? color : 0xFF00E5FF;
        // h1 in crisp white
        PAINT.setColor(0xFFFFFFFF);
        canvas.drawText(h1, h1X, ty, PAINT);
        // h2 in vibrant accent color prominently overlapping m1
        PAINT.setColor(accentColor);
        canvas.drawText(h2, h2X, ty, PAINT);

        // Subtle vertical editorial separator
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(1.2f);
        STROKE.setColor(0x2EFFFFFF);
        float divX = bx + bw * 0.77f;
        canvas.drawLine(divX, by + bh * 0.18f, divX, by + bh * 0.82f, STROKE);

        // Vertical Editorial Date Strip on the right edge
        float rightX = bx + bw * 0.82f;
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat numFmt = new SimpleDateFormat("d", Locale.getDefault());

        String dayStr = dayFmt.format(now.getTime()).toUpperCase(Locale.getDefault());
        String numStr = numFmt.format(now.getTime());

        PAINT.setFontFeatureSettings(null);
        PAINT.setTextSize(bh * 0.13f);
        PAINT.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        PAINT.setColor(accentColor);
        canvas.drawText(dayStr, rightX, by + bh * 0.36f, PAINT);

        PAINT.setTextSize(bh * 0.24f);
        PAINT.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        PAINT.setColor(0xFFFFFFFF);
        canvas.drawText(numStr, rightX, by + bh * 0.66f, PAINT);
    }

    /**
     * 4. Split-Flap Departure Board:
     * Mechanical Solari airport flipper cards with hours, minutes, and actively flipping seconds tiles.
     */
    private static void drawSplitFlapBoard(Canvas canvas, float bx, float by, float bw, float bh,
                                          Calendar now, boolean is24Hour, int color,
                                          boolean alarmSet, boolean locked) {
        // Airport Header
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        PAINT.setFontFeatureSettings(null);
        PAINT.setTextSize(bh * 0.082f);
        PAINT.setColor(0xFFF59E0B); // Amber airport header
        canvas.drawText("DEPARTURES // FLIGHT RZ-40", bx + bw * 0.06f, by + bh * 0.16f, PAINT);

        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        int sec = now.get(Calendar.SECOND);
        int ms = now.get(Calendar.MILLISECOND);

        String[] digits = {
                String.valueOf(hour / 10),
                String.valueOf(hour % 10),
                String.valueOf(min / 10),
                String.valueOf(min % 10),
                String.valueOf(sec / 10),
                String.valueOf(sec % 10)
        };

        // Prev seconds digit for the flipping mechanical motion
        int prevSec = (sec + 59) % 60;
        String[] prevDigits = {
                digits[0], digits[1], digits[2], digits[3],
                String.valueOf(prevSec / 10),
                String.valueOf(prevSec % 10)
        };

        // Flipping progress: first 250ms of each second flips the flap down!
        float flipProgress = Math.min(1.0f, ms / 250.0f);

        // Tile sizing: 6 tiles across width 368
        float tileW = bw * 0.125f;  // ~46px
        float tileH = bh * 0.46f;   // ~89px
        float tileY = by + bh * 0.22f;
        float gap = bw * 0.015f;    // ~5.5px
        float colonGap = bw * 0.045f; // ~16.5px

        float totalW = tileW * 6f + gap * 3f + colonGap * 2f;
        float startX = bx + (bw - totalW) / 2f;

        PAINT.setTextSize(tileH * 0.72f);
        PAINT.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");

        float curX = startX;
        for (int i = 0; i < 6; i++) {
            if (i == 2 || i == 4) {
                // Mechanical colon clips between HH-MM and MM-SS
                float colonX = curX + colonGap / 2f;
                float dotSize = bh * 0.040f;
                PAINT.setColor(i == 4 ? 0xFFEF4444 : 0xFFF59E0B);
                canvas.drawCircle(colonX, tileY + tileH * 0.35f, dotSize / 2f, PAINT);
                canvas.drawCircle(colonX, tileY + tileH * 0.65f, dotSize / 2f, PAINT);
                curX += colonGap;
            }

            RectF card = new RectF(curX, tileY, curX + tileW, tileY + tileH);
            float midY = card.centerY();
            boolean isSec = (i >= 4);

            // Base Card: Bottom half shadow
            PAINT.setColor(0xFF141416);
            canvas.drawRoundRect(new RectF(card.left, midY, card.right, card.bottom), 4f, 4f, PAINT);
            // Top half lighter
            PAINT.setColor(0xFF242428);
            canvas.drawRoundRect(new RectF(card.left, card.top, card.right, midY), 4f, 4f, PAINT);

            // Digits: Crisp white for HH:MM, Amber tint for SS
            PAINT.setColor(isSec ? 0xFFFBBF24 : 0xFFFFFFFF);

            if (isSec && flipProgress < 1.0f) {
                // Active mechanical flipping animation for seconds flap!
                if (flipProgress < 0.5f) {
                    // Top flap folding down (showing previous digit top half)
                    float scaleY = 1.0f - (flipProgress * 2.0f);
                    canvas.save();
                    canvas.clipRect(card.left, card.top, card.right, card.bottom);

                    // Draw incoming bottom half
                    canvas.save();
                    canvas.clipRect(card.left, midY, card.right, card.bottom);
                    float dw = PAINT.measureText(digits[i]);
                    canvas.drawText(digits[i], card.centerX() - dw / 2f, card.centerY() + tileH * 0.25f, PAINT);
                    canvas.restore();

                    // Draw folding top half
                    canvas.save();
                    canvas.scale(1.0f, scaleY, card.centerX(), midY);
                    canvas.clipRect(card.left, card.top, card.right, midY);
                    PAINT.setColor(0xFF2A2A30);
                    canvas.drawRect(card.left, card.top, card.right, midY, PAINT);
                    PAINT.setColor(0xFFFBBF24);
                    float pdw = PAINT.measureText(prevDigits[i]);
                    canvas.drawText(prevDigits[i], card.centerX() - pdw / 2f, card.centerY() + tileH * 0.25f, PAINT);
                    canvas.restore();

                    canvas.restore();
                } else {
                    // Bottom flap falling into place
                    float scaleY = (flipProgress - 0.5f) * 2.0f;
                    canvas.save();
                    canvas.clipRect(card.left, card.top, card.right, card.bottom);

                    // Draw static new top half
                    canvas.save();
                    canvas.clipRect(card.left, card.top, card.right, midY);
                    float dw = PAINT.measureText(digits[i]);
                    canvas.drawText(digits[i], card.centerX() - dw / 2f, card.centerY() + tileH * 0.25f, PAINT);
                    canvas.restore();

                    // Draw falling bottom flap
                    canvas.save();
                    canvas.scale(1.0f, scaleY, card.centerX(), midY);
                    canvas.clipRect(card.left, midY, card.right, card.bottom);
                    PAINT.setColor(0xFF141416);
                    canvas.drawRect(card.left, midY, card.right, card.bottom, PAINT);
                    PAINT.setColor(0xFFFBBF24);
                    canvas.drawText(digits[i], card.centerX() - dw / 2f, card.centerY() + tileH * 0.25f, PAINT);
                    canvas.restore();

                    canvas.restore();
                }
            } else {
                // Static display
                float dw = PAINT.measureText(digits[i]);
                canvas.drawText(digits[i], card.centerX() - dw / 2f, card.centerY() + tileH * 0.25f, PAINT);
            }

            // Horizontal Mechanical Split Seam across the card
            STROKE.setStyle(Paint.Style.STROKE);
            STROKE.setStrokeWidth(2.0f);
            STROKE.setColor(0xFF09090B);
            canvas.drawLine(card.left, midY, card.right, midY, STROKE);

            STROKE.setStrokeWidth(1.0f);
            STROKE.setColor(0x33FFFFFF);
            canvas.drawLine(card.left, midY + 1.5f, card.right, midY + 1.5f, STROKE);

            // Side mounting hinge clips
            PAINT.setColor(0xFF09090B);
            canvas.drawRect(card.left - 2f, midY - 3f, card.left + 3f, midY + 3f, PAINT);
            canvas.drawRect(card.right - 3f, midY - 3f, card.right + 2f, midY + 3f, PAINT);

            curX += tileW + ((i % 2 == 0) ? gap : 0);
        }

        // Bottom status line
        SimpleDateFormat df = new SimpleDateFormat("EEE · d MMM", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setFontFeatureSettings(null);
        PAINT.setTextSize(bh * 0.082f);
        PAINT.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        PAINT.setColor(0xFFA1A1AA);
        canvas.drawText("STATUS: ON TIME // GATE 31 · " + dateStr, bx + bw * 0.06f, by + bh * 0.88f, PAINT);
    }

    /**
     * 5. Nixie Vacuum Tubes:
     * Soviet IN-14 steampunk warm fiery glowing glass vacuum tubes with internal cathode mesh.
     */
    private static void drawNixieTubes(Canvas canvas, float bx, float by, float bw, float bh,
                                       Calendar now, boolean is24Hour, int color,
                                       boolean alarmSet, boolean locked) {
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        String[] digits = {
                String.valueOf(hour / 10),
                String.valueOf(hour % 10),
                String.valueOf(min / 10),
                String.valueOf(min % 10)
        };

        float tubeW = bw * 0.18f;
        float tubeH = bh * 0.60f;
        float tubeY = by + bh * 0.12f;
        float gap = bw * 0.02f;
        float colonGap = bw * 0.06f;

        float totalW = tubeW * 4f + gap * 2f + colonGap;
        float startX = bx + (bw - totalW) / 2f;

        STROKE.setStyle(Paint.Style.STROKE);
        PAINT.setStyle(Paint.Style.FILL);

        // Bottom Bakelite/Brass Base Pedestal
        float baseY = tubeY + tubeH;
        PAINT.setColor(0xFF1C1917);
        canvas.drawRect(startX - 8f, baseY, startX + totalW + 8f, baseY + bh * 0.08f, PAINT);
        STROKE.setStrokeWidth(1.2f);
        STROKE.setColor(0xFFB45309); // Brass rim line
        canvas.drawLine(startX - 8f, baseY, startX + totalW + 8f, baseY, STROKE);

        float curX = startX;
        for (int i = 0; i < 4; i++) {
            if (i == 2) {
                // Neon glow colon dots
                float cx = curX + colonGap / 2f;
                PAINT.setColor(0x30FF5500);
                canvas.drawCircle(cx, tubeY + tubeH * 0.40f, 8f, PAINT);
                canvas.drawCircle(cx, tubeY + tubeH * 0.65f, 8f, PAINT);
                PAINT.setColor(0xFFFFAA00);
                canvas.drawCircle(cx, tubeY + tubeH * 0.40f, 3.5f, PAINT);
                canvas.drawCircle(cx, tubeY + tubeH * 0.65f, 3.5f, PAINT);
                curX += colonGap;
            }

            RectF tube = new RectF(curX, tubeY, curX + tubeW, tubeY + tubeH);

            // Glass envelope body
            PAINT.setColor(0x18FFFFFF);
            canvas.drawRoundRect(tube, 14f, 14f, PAINT);
            STROKE.setStrokeWidth(1.2f);
            STROKE.setColor(0x33FFFFFF);
            canvas.drawRoundRect(tube, 14f, 14f, STROKE);

            // Subtle vertical glass specular reflection on left edge
            STROKE.setColor(0x22FFFFFF);
            canvas.drawLine(tube.left + 3f, tube.top + 8f, tube.left + 3f, tube.bottom - 8f, STROKE);

            // Fiery Nixie Cathode Numeral
            STROKE.setTextSize(tubeH * 0.68f);
            STROKE.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
            STROKE.setFontFeatureSettings("tnum");

            float dw = STROKE.measureText(digits[i]);
            float tx = tube.centerX() - dw / 2f;
            float ty = tube.centerY() + tubeH * 0.24f;

            // Pass 1: Warm Outer Filament Aura
            STROKE.setStrokeWidth(7.0f);
            STROKE.setColor(0x35FF5500);
            canvas.drawText(digits[i], tx, ty, STROKE);

            // Pass 2: Intense Neon Orange Glow
            STROKE.setStrokeWidth(3.2f);
            STROKE.setColor(0x95FFAA00);
            canvas.drawText(digits[i], tx, ty, STROKE);

            // Pass 3: White-Hot Filament Wire Core
            STROKE.setStrokeWidth(1.2f);
            STROKE.setColor(0xFFFFF7ED);
            canvas.drawText(digits[i], tx, ty, STROKE);

            curX += tubeW + (i != 1 ? gap : 0);
        }

        // Bottom Date in Amber Neon
        SimpleDateFormat df = new SimpleDateFormat("d MMMM yyyy · EEE", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setTextSize(bh * 0.08f);
        PAINT.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        PAINT.setFontFeatureSettings(null);
        PAINT.setColor(0xFFF59E0B);
        float dw = PAINT.measureText(dateStr);
        canvas.drawText(dateStr, bx + (bw - dw) / 2f, baseY + bh * 0.16f, PAINT);
    }

    /**
     * 6. Game Boy Classic (1989):
     * Authentic Nintendo DMG-01 4-shade olive green LCD screen with pixelated retro graphics.
     */
    private static void drawGameBoyClassic(Canvas canvas, float bx, float by, float bw, float bh,
                                          Calendar now, boolean is24Hour, int color,
                                          boolean alarmSet, boolean locked) {
        // Classic DMG-01 Olive LCD Screen Container
        RectF screen = new RectF(bx + bw * 0.04f, by + bh * 0.06f, bx + bw * 0.96f, by + bh * 0.94f);
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(0xFF8BAC0F); // Authentic GameBoy LCD olive green
        canvas.drawRoundRect(screen, 10f, 10f, PAINT);

        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(2.5f);
        STROKE.setColor(0xFF306230); // Dark olive rim
        canvas.drawRoundRect(screen, 10f, 10f, STROKE);

        // Battery Power LED
        float ledX = screen.left + 14f;
        float ledY = screen.top + 16f;
        PAINT.setColor(0xFFEF4444); // Red battery LED
        canvas.drawCircle(ledX, ledY, 3.5f, PAINT);

        PAINT.setTextSize(bh * 0.075f);
        PAINT.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        PAINT.setFontFeatureSettings(null);
        PAINT.setColor(0xFF0F380F); // Deepest GameBoy dark green
        canvas.drawText("BATTERY", ledX + 8f, ledY + 3.5f, PAINT);

        // Game Time Readout
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        String hStr = String.format(Locale.getDefault(), "%02d", hour);
        String mStr = String.format(Locale.getDefault(), "%02d", min);

        PAINT.setTextSize(bh * 0.44f);
        PAINT.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");
        PAINT.setColor(0xFF0F380F);

        float hw = PAINT.measureText(hStr);
        float mw = PAINT.measureText(mStr);
        float colonGap = bh * 0.12f;
        float totalW = hw + colonGap + mw;
        float startX = screen.centerX() - totalW / 2f;
        float ty = screen.centerY() + bh * 0.10f;

        canvas.drawText(hStr, startX, ty, PAINT);

        // Blinking pixel block colon
        boolean blink = (now.get(Calendar.MILLISECOND) < 500);
        if (blink) {
            float sqX = startX + hw + (colonGap - bh * 0.05f) / 2f;
            float sqSize = bh * 0.05f;
            canvas.drawRect(sqX, ty - bh * 0.26f, sqX + sqSize, ty - bh * 0.26f + sqSize, PAINT);
            canvas.drawRect(sqX, ty - bh * 0.12f, sqX + sqSize, ty - bh * 0.12f + sqSize, PAINT);
        }

        canvas.drawText(mStr, startX + hw + colonGap, ty, PAINT);

        // Game Status Footer
        SimpleDateFormat df = new SimpleDateFormat("d MMM", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setTextSize(bh * 0.082f);
        PAINT.setFontFeatureSettings(null);
        canvas.drawText("♥ ♥ ♥ ♥", screen.left + 14f, screen.bottom - 12f, PAINT);
        canvas.drawText("[ " + dateStr + " // LVL 1 ]", screen.right - 130f, screen.bottom - 12f, PAINT);
    }

    /**
     * 7. Architectural CAD Blueprint:
     * Engineering cyanotype drafting sheet with grid lines, dimension callouts, and CAD stenciling.
     */
    private static void drawBlueprintDraft(Canvas canvas, float bx, float by, float bw, float bh,
                                          Calendar now, boolean is24Hour, int color,
                                          boolean alarmSet, boolean locked) {
        // Deep blueprint navy background
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(0xFF0A192F);
        canvas.drawRect(bx, by, bx + bw, by + bh, PAINT);

        // Cyan Drafting Grid Lines
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(0.8f);
        STROKE.setColor(0x1800F5FF);
        float gridStep = 18f;
        for (float x = bx; x <= bx + bw; x += gridStep) {
            canvas.drawLine(x, by, x, by + bh, STROKE);
        }
        for (float y = by; y <= by + bh; y += gridStep) {
            canvas.drawLine(bx, y, bx + bw, y, STROKE);
        }

        // Top Dimension Callout Arrow
        float dimY = by + bh * 0.14f;
        STROKE.setColor(0xFF64FFDA);
        STROKE.setStrokeWidth(1.2f);
        canvas.drawLine(bx + 14f, dimY, bx + bw - 14f, dimY, STROKE);
        // Ticks
        canvas.drawLine(bx + 14f, dimY - 4f, bx + 14f, dimY + 4f, STROKE);
        canvas.drawLine(bx + bw - 14f, dimY - 4f, bx + bw - 14f, dimY + 4f, STROKE);

        PAINT.setTextSize(bh * 0.075f);
        PAINT.setTypeface(Typeface.MONOSPACE);
        PAINT.setFontFeatureSettings(null);
        PAINT.setColor(0xFF64FFDA);
        canvas.drawText("<--- 368.00 mm --->", bx + bw * 0.34f, dimY - 4f, PAINT);

        // Technical Drafting Time
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, min);

        PAINT.setTextSize(bh * 0.48f);
        PAINT.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");
        PAINT.setColor(0xFFE2E8F0);
        canvas.drawText(timeStr, bx + bw * 0.08f, by + bh * 0.64f, PAINT);

        // Corner drafting crosshairs
        STROKE.setColor(0xFF64FFDA);
        canvas.drawLine(bx + 8f, by + 12f, bx + 16f, by + 12f, STROKE);
        canvas.drawLine(bx + 12f, by + 8f, bx + 12f, by + 16f, STROKE);

        // Title Block in Bottom-Right
        RectF titleBox = new RectF(bx + bw * 0.58f, by + bh * 0.66f, bx + bw * 0.94f, by + bh * 0.92f);
        canvas.drawRect(titleBox, STROKE);

        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        String dateStr = df.format(now.getTime());

        PAINT.setFontFeatureSettings(null);
        PAINT.setTextSize(bh * 0.075f);
        PAINT.setColor(0xFF64FFDA);
        canvas.drawText("DWG: CHRONOS-A1", titleBox.left + 6f, titleBox.top + bh * 0.10f, PAINT);
        canvas.drawText("DATE: " + dateStr, titleBox.left + 6f, titleBox.bottom - bh * 0.05f, PAINT);
    }

    /**
     * 8. Cyberpunk Hex Cockpit:
     * Sci-fi mech/spacecraft cockpit HUD with live radar sweep, angular chamfered frame, and telemetry.
     */
    private static void drawCyberHexHud(Canvas canvas, float bx, float by, float bw, float bh,
                                       Calendar now, boolean is24Hour, int color,
                                       boolean alarmSet, boolean locked) {
        // Center Chamfered Frame
        float frameL = bx + bw * 0.28f;
        float frameT = by + bh * 0.16f;
        float frameR = bx + bw * 0.94f;
        float frameB = by + bh * 0.74f;
        float cut = 12f;

        PATH.reset();
        PATH.moveTo(frameL + cut, frameT);
        PATH.lineTo(frameR - cut, frameT);
        PATH.lineTo(frameR, frameT + cut);
        PATH.lineTo(frameR, frameB - cut);
        PATH.lineTo(frameR - cut, frameB);
        PATH.lineTo(frameL + cut, frameB);
        PATH.lineTo(frameL, frameB - cut);
        PATH.lineTo(frameL, frameT + cut);
        PATH.close();

        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(0xFF0F172A);
        canvas.drawPath(PATH, PAINT);

        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(1.5f);
        STROKE.setColor(0xFFF59E0B); // Amber HUD
        canvas.drawPath(PATH, STROKE);

        // Main Cockpit Time inside Frame
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, min);

        PAINT.setTextSize(bh * 0.44f);
        PAINT.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");
        PAINT.setColor(Color.WHITE);
        canvas.drawText(timeStr, frameL + 18f, frameT + bh * 0.42f, PAINT);

        // Left Radar Arc Scanner
        float radarX = bx + bw * 0.14f;
        float radarY = by + bh * 0.45f;
        float radarR = bh * 0.32f;

        STROKE.setColor(0xFF06B6D4);
        STROKE.setStrokeWidth(1.0f);
        canvas.drawCircle(radarX, radarY, radarR, STROKE);
        canvas.drawCircle(radarX, radarY, radarR * 0.5f, STROKE);
        canvas.drawLine(radarX - radarR, radarY, radarX + radarR, radarY, STROKE);
        canvas.drawLine(radarX, radarY - radarR, radarX, radarY + radarR, STROKE);

        // Rotating radar sweep line
        int sec = now.get(Calendar.SECOND);
        float rad = (float) Math.toRadians(sec * 6.0);
        float sweepX = radarX + (float) Math.cos(rad) * radarR;
        float sweepY = radarY + (float) Math.sin(rad) * radarR;
        STROKE.setStrokeWidth(2.0f);
        STROKE.setColor(0xFFF59E0B);
        canvas.drawLine(radarX, radarY, sweepX, sweepY, STROKE);

        // Bottom Telemetry
        SimpleDateFormat df = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setFontFeatureSettings(null);
        PAINT.setTextSize(bh * 0.082f);
        PAINT.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        PAINT.setColor(0xFF38BDF8);
        canvas.drawText("SHIELD: 100% // ENG: NOMINAL", bx + bw * 0.06f, by + bh * 0.88f, PAINT);
        canvas.drawText(dateStr, bx + bw * 0.68f, by + bh * 0.88f, PAINT);
    }

    /**
     * 9. Vinyl Hi-Fi Turntable:
     * Full-screen centered rotating vinyl record in the background with center display
     * making both hours and minutes 100% visible, tonearm needle, and stereo VU meters.
     */
    private static void drawVinylTurntable(Canvas canvas, float bx, float by, float bw, float bh,
                                          Calendar now, boolean is24Hour, int color,
                                          boolean alarmSet, boolean locked) {
        float cx = bx + bw / 2f;
        float cy = by + bh / 2f;
        float recordR = bh * 0.88f;

        // Continuous vinyl rotation animation (33⅓ RPM)
        long t = System.currentTimeMillis();
        float angle = ((t % 7200) / 7200.0f) * 360.0f;

        // --- 1. ROTATING VINYL RECORD (BACKGROUND) ---
        canvas.save();
        canvas.rotate(angle, cx, cy);

        // Black Vinyl Platter Body
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(0xFF121214);
        canvas.drawCircle(cx, cy, recordR, PAINT);

        // Concentric Vinyl Micro-Grooves
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(1.2f);
        STROKE.setColor(0xFF1E1E22);
        canvas.drawCircle(cx, cy, recordR * 0.94f, STROKE);
        canvas.drawCircle(cx, cy, recordR * 0.84f, STROKE);
        canvas.drawCircle(cx, cy, recordR * 0.74f, STROKE);
        canvas.drawCircle(cx, cy, recordR * 0.64f, STROKE);
        canvas.drawCircle(cx, cy, recordR * 0.54f, STROKE);
        canvas.drawCircle(cx, cy, recordR * 0.44f, STROKE);

        // Vinyl Specular Light Reflection Bands (Shine that rotates with the record)
        STROKE.setStrokeWidth(recordR * 0.35f);
        STROKE.setColor(0x0CFFFFFF);
        canvas.drawLine(cx - recordR, cy, cx + recordR, cy, STROKE);
        canvas.drawLine(cx, cy - recordR, cx, cy + recordR, STROKE);

        // Rotating Crimson Center Album Label
        float labelR = bh * 0.38f;
        PAINT.setColor(0xFFB91C1C); // Crimson record label
        canvas.drawCircle(cx, cy, labelR, PAINT);

        // Label Gold Accent Ring
        STROKE.setStrokeWidth(2.0f);
        STROKE.setColor(0xFFD97706);
        canvas.drawCircle(cx, cy, labelR * 0.92f, STROKE);

        canvas.restore();

        // --- 2. TONEARM NEEDLE (TOP-RIGHT CORNER) ---
        float armPivotX = bx + bw * 0.92f;
        float armPivotY = by + bh * 0.08f;
        float needleX = cx + recordR * 0.60f;
        float needleY = cy - recordR * 0.35f;

        // Tonearm pivot base
        PAINT.setColor(0xFF3F3F46);
        canvas.drawCircle(armPivotX, armPivotY, 8f, PAINT);
        // Silver tonearm rod
        STROKE.setStrokeWidth(2.5f);
        STROKE.setColor(0xFFE4E4E7);
        canvas.drawLine(armPivotX, armPivotY, needleX, needleY, STROKE);
        // Cartridge / stylus head
        PAINT.setColor(0xFFEF4444);
        canvas.drawCircle(needleX, needleY, 3.5f, PAINT);

        // --- 3. HIGH-CONTRAST CENTER CLOCK (HOURS & MINUTES 100% VISIBLE) ---
        float hubR = bh * 0.34f;
        // Dark translucent acrylic center hub
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(0xE609090B);
        canvas.drawCircle(cx, cy, hubR, PAINT);
        STROKE.setStrokeWidth(1.5f);
        STROKE.setColor(0xFFF59E0B);
        canvas.drawCircle(cx, cy, hubR, STROKE);

        // Time Readout
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, min);

        // Top Header inside hub
        PAINT.setTextSize(bh * 0.075f);
        PAINT.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        PAINT.setFontFeatureSettings(null);
        PAINT.setColor(0xFFF59E0B);
        float hw = PAINT.measureText("HI-FI STEREO");
        canvas.drawText("HI-FI STEREO", cx - hw / 2f, cy - bh * 0.16f, PAINT);

        // Large Crisp Center Time (HH:MM)
        PAINT.setTextSize(bh * 0.34f);
        PAINT.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");
        PAINT.setColor(Color.WHITE);
        float tw = PAINT.measureText(timeStr);
        canvas.drawText(timeStr, cx - tw / 2f, cy + bh * 0.08f, PAINT);

        // Bottom Date inside hub
        SimpleDateFormat df = new SimpleDateFormat("EEE · d MMM", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setFontFeatureSettings(null);
        PAINT.setTextSize(bh * 0.075f);
        PAINT.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        PAINT.setColor(0xFFE2E8F0);
        float dw = PAINT.measureText(dateStr);
        canvas.drawText(dateStr, cx - dw / 2f, cy + bh * 0.22f, PAINT);

        // --- 4. STEREO VU METER (BOTTOM BAR) ---
        int bounceL = (int) (6 + Math.abs(Math.sin(t / 220.0)) * 4);
        int bounceR = (int) (5 + Math.abs(Math.cos(t / 260.0)) * 5);

        float vuStartX = bx + bw * 0.06f;
        float vuBarW = bw * 0.24f;
        float vuY = by + bh * 0.82f;
        int segs = 8;
        float segW = (vuBarW - (segs - 1) * 2f) / segs;

        for (int ch = 0; ch < 2; ch++) {
            float curBarY = vuY + ch * (bh * 0.07f);
            int activeCount = (ch == 0) ? bounceL : bounceR;

            for (int s = 0; s < segs; s++) {
                float sx = vuStartX + s * (segW + 2f);
                boolean on = s <= activeCount;

                if (s < 5) {
                    PAINT.setColor(on ? 0xFF10B981 : 0x2210B981); // Green
                } else if (s < 7) {
                    PAINT.setColor(on ? 0xFFF59E0B : 0x22F59E0B); // Amber
                } else {
                    PAINT.setColor(on ? 0xFFEF4444 : 0x22EF4444); // Red
                }
                canvas.drawRect(sx, curBarY, sx + segW, curBarY + bh * 0.045f, PAINT);
            }
        }
    }

    /**
     * 10. 80s Audio Cassette Tape:
     * Transparent vintage cassette body with dual spinning hubs, winding magnetic tape, and mixtape label.
     */
    private static void drawAudioCassette(Canvas canvas, float bx, float by, float bw, float bh,
                                         Calendar now, boolean is24Hour, int color,
                                         boolean alarmSet, boolean locked) {
        // Cassette Shell
        RectF shell = new RectF(bx + bw * 0.04f, by + bh * 0.06f, bx + bw * 0.96f, by + bh * 0.94f);
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(0xFF1C1917); // Dark vintage cassette shell
        canvas.drawRoundRect(shell, 12f, 12f, PAINT);

        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(1.5f);
        STROKE.setColor(0xFF44403C);
        canvas.drawRoundRect(shell, 12f, 12f, STROKE);

        // Cassette Top Label (Off-white sticker)
        RectF label = new RectF(shell.left + 14f, shell.top + 10f, shell.right - 14f, shell.top + bh * 0.28f);
        PAINT.setColor(0xFFF5F5F4);
        canvas.drawRoundRect(label, 6f, 6f, PAINT);

        SimpleDateFormat df = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
        String dateStr = df.format(now.getTime()).toUpperCase(Locale.getDefault());

        PAINT.setTextSize(bh * 0.082f);
        PAINT.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        PAINT.setFontFeatureSettings(null);
        PAINT.setColor(0xFF1C1917);
        canvas.drawText("MIX TAPE VOL. 1 // " + dateStr, label.left + 10f, label.centerY() + bh * 0.03f, PAINT);

        // Center Clear Tape Window
        RectF window = new RectF(shell.left + 24f, shell.top + bh * 0.34f, shell.right - 24f, shell.bottom - 12f);
        PAINT.setColor(0xFF0C0A09);
        canvas.drawRoundRect(window, 6f, 6f, PAINT);
        STROKE.setColor(0xFF57534E);
        canvas.drawRoundRect(window, 6f, 6f, STROKE);

        // Left & Right Tape Spool Hubs
        float hubL = window.left + bw * 0.12f;
        float hubR = window.right - bw * 0.12f;
        float hubY = window.centerY();
        float hubRadius = bh * 0.18f;

        PAINT.setColor(0xFFE7E5E4); // White spool hubs
        canvas.drawCircle(hubL, hubY, hubRadius, PAINT);
        canvas.drawCircle(hubR, hubY, hubRadius, PAINT);

        PAINT.setColor(0xFF0C0A09);
        canvas.drawCircle(hubL, hubY, hubRadius * 0.45f, PAINT);
        canvas.drawCircle(hubR, hubY, hubRadius * 0.45f, PAINT);

        // Digital Tape Counter Time in Center
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int min = now.get(Calendar.MINUTE);
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, min);

        PAINT.setTextSize(bh * 0.38f);
        PAINT.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        PAINT.setFontFeatureSettings("tnum");
        PAINT.setColor(0xFFEF4444); // Red digital tape counter
        float tw = PAINT.measureText(timeStr);
        canvas.drawText(timeStr, window.centerX() - tw / 2f, window.centerY() + bh * 0.13f, PAINT);
    }
}
