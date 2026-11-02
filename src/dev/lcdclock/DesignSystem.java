package dev.lcdclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Unified Dark AMOLED Design System for the 368 x 194 external display.
 * Provides consistent typography, colors, layout metrics, card backgrounds,
 * buttons, and badges.
 */
public final class DesignSystem {
    // Design reference viewport
    public static final float REF_WIDTH = 368f;
    public static final float REF_HEIGHT = 194f;

    // Premium Dark AMOLED Palette
    public static final int COLOR_BG = 0xFF000000;
    public static final int COLOR_CARD = 0xFF121214;
    public static final int COLOR_CARD_ELEVATED = 0xFF1C1C20;
    public static final int COLOR_CARD_PRESSED = 0xFF2A2A30;
    public static final int COLOR_BORDER = 0xFF2A2A2E;
    public static final int COLOR_BORDER_SUBTLE = 0xFF1E1E22;
    public static final int COLOR_BORDER_FOCUS = 0xFF3A3A44;

    // Text & Content Colors
    public static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int COLOR_TEXT_SECONDARY = 0xFF9E9EA4;
    public static final int COLOR_TEXT_TERTIARY = 0xFF636366;
    public static final int COLOR_TEXT_MUTED = 0xFF444448;

    // Accent Colors
    public static final int COLOR_ACCENT = 0xFF00E5FF;
    public static final int COLOR_ACCENT_CYAN = 0xFF00E5FF;
    public static final int COLOR_ACCENT_AQUA = 0xFF00D2FF;
    public static final int COLOR_ACCENT_GREEN = 0xFF30D158;
    public static final int COLOR_ACCENT_AMBER = 0xFFFF9F0A;
    public static final int COLOR_ACCENT_RED = 0xFFFF453A;
    public static final int COLOR_ACCENT_PURPLE = 0xFFA855F7;
    public static final int COLOR_ACCENT_BLUE = 0xFF0A84FF;

    // Corner Radii (relative to 194px height, scaled by bh / REF_HEIGHT)
    public static final float RADIUS_SMALL = 6f;
    public static final float RADIUS_MEDIUM = 10f;
    public static final float RADIUS_LARGE = 14f;
    public static final float RADIUS_PILL = 24f;

    // Shared Paint Objects for efficiency
    private static final Paint FILL_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint STROKE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint TEXT_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF RECT_BUFFER = new RectF();
    private static final Rect TEXT_BOUNDS = new Rect();

    private DesignSystem() {
    }

    /**
     * Draws a modern rounded surface card.
     */
    public static void drawCard(Canvas canvas, float left, float top, float right, float bottom,
                                float radius, int fillColor, int borderColor) {
        RECT_BUFFER.set(left, top, right, bottom);
        FILL_PAINT.setStyle(Paint.Style.FILL);
        FILL_PAINT.setColor(fillColor);
        canvas.drawRoundRect(RECT_BUFFER, radius, radius, FILL_PAINT);

        if (borderColor != 0) {
            STROKE_PAINT.setStyle(Paint.Style.STROKE);
            STROKE_PAINT.setStrokeWidth(1f);
            STROKE_PAINT.setColor(borderColor);
            canvas.drawRoundRect(RECT_BUFFER, radius, radius, STROKE_PAINT);
        }
    }

    /**
     * Draws an interactive pill button (e.g. for action triggers or chips).
     */
    public static void drawPill(Canvas canvas, float left, float top, float right, float bottom,
                                int fillColor, int borderColor) {
        float r = (bottom - top) / 2f;
        drawCard(canvas, left, top, right, bottom, r, fillColor, borderColor);
    }

    /**
     * Draws a status badge / chip with an optional icon indicator.
     */
    public static void drawBadge(Canvas canvas, float x, float y, String label, int accentColor, float fontSize) {
        TEXT_PAINT.setTextSize(fontSize);
        TEXT_PAINT.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        TEXT_PAINT.getTextBounds(label, 0, label.length(), TEXT_BOUNDS);

        float textW = TEXT_PAINT.measureText(label);
        float padH = fontSize * 0.6f;
        float padV = fontSize * 0.35f;

        float left = x;
        float top = y - TEXT_BOUNDS.height() - padV;
        float right = left + textW + padH * 2f;
        float bottom = y + padV;
        float r = (bottom - top) / 2f;

        // Dim background matching accent color
        int bg = (accentColor & 0x00FFFFFF) | 0x26000000;
        drawCard(canvas, left, top, right, bottom, r, bg, (accentColor & 0x00FFFFFF) | 0x55000000);

        // Text
        TEXT_PAINT.setColor(accentColor);
        canvas.drawText(label, left + padH, y, TEXT_PAINT);
    }

    /**
     * Formats truncated text with ellipsis.
     */
    public static String ellipsize(String text, Paint paint, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        float ellipsisW = paint.measureText(ellipsis);
        if (ellipsisW >= maxWidth) {
            return "";
        }
        int len = text.length();
        while (len > 0 && paint.measureText(text.substring(0, len)) + ellipsisW > maxWidth) {
            len--;
        }
        return text.substring(0, len) + ellipsis;
    }
}
