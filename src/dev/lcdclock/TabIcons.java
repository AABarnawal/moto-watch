package dev.lcdclock;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

/**
 * Modern minimalist vector tab icons for the external display.
 * Designed with smooth curves, high-contrast geometry, and clear active states.
 */
final class TabIcons {
    private static final int COLOR_INACTIVE = 0xFF71717A; // Sophisticated neutral gray
    private static final int COLOR_ACTIVE = 0xFFFFFFFF;   // Crisp white
    private static final int DIM_ALPHA = 120;
    private static final Map<String, Drawable> appIcons = new HashMap<>();

    private final Context context;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    TabIcons(Context context) {
        this.context = context;
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
    }

    /**
     * Draws a modern vector icon inside rect `r`.
     */
    void draw(Canvas canvas, String page, RectF r, boolean on) {
        int ink = on ? COLOR_ACTIVE : COLOR_INACTIVE;
        float s = r.width();
        float cx = r.centerX();
        float cy = r.centerY();
        float strokeW = Math.max(1.5f, s * 0.09f);

        stroke.setStrokeWidth(strokeW);
        stroke.setColor(ink);
        fill.setColor(ink);
        path.reset();

        switch (page) {
            case CoverActivity.PAGE_FACE: { // Modern smartwatch outline with 10:10 hands
                float radius = s * 0.40f;
                stroke.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(cx, cy, radius, stroke);
                // Hands
                stroke.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(cx, cy, cx + radius * 0.45f, cy - radius * 0.45f, stroke);
                canvas.drawLine(cx, cy, cx - radius * 0.35f, cy - radius * 0.20f, stroke);
                // Center point
                canvas.drawCircle(cx, cy, strokeW * 0.7f, fill);
                break;
            }
            case CoverActivity.PAGE_CLOCK: { // Stopwatch / Timer
                float radius = s * 0.36f;
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawCircle(cx, cy + s * 0.04f, radius, stroke);
                // Top pusher button
                canvas.drawLine(cx - s * 0.10f, cy - radius - s * 0.04f, cx + s * 0.10f, cy - radius - s * 0.04f, stroke);
                canvas.drawLine(cx, cy - radius - s * 0.04f, cx, cy - radius + s * 0.04f, stroke);
                // Stopwatch hand
                canvas.drawLine(cx, cy + s * 0.04f, cx + radius * 0.45f, cy + s * 0.04f - radius * 0.45f, stroke);
                break;
            }
            case CoverActivity.PAGE_ALARM: { // Minimalist Bell
                float w = s * 0.36f;
                float top = cy - s * 0.32f;
                float bottom = cy + s * 0.24f;
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                // Bell body
                path.moveTo(cx, top);
                path.cubicTo(cx - w * 0.6f, top + s * 0.1f, cx - w, bottom - s * 0.1f, cx - w, bottom);
                path.lineTo(cx + w, bottom);
                path.cubicTo(cx + w, bottom - s * 0.1f, cx + w * 0.6f, top + s * 0.1f, cx, top);
                canvas.drawPath(path, stroke);
                // Clapper
                canvas.drawCircle(cx, bottom + s * 0.08f, strokeW * 0.9f, fill);
                break;
            }
            case CoverActivity.PAGE_CALENDAR: { // Clean Calendar
                float cw = s * 0.38f;
                float ch = s * 0.36f;
                float rTop = cy - ch / 2f;
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                RectF calRect = new RectF(cx - cw, rTop, cx + cw, rTop + ch * 2f);
                canvas.drawRoundRect(calRect, strokeW * 1.5f, strokeW * 1.5f, stroke);
                // Header line
                canvas.drawLine(cx - cw, rTop + ch * 0.6f, cx + cw, rTop + ch * 0.6f, stroke);
                // Binder hooks
                canvas.drawLine(cx - cw * 0.5f, rTop - s * 0.08f, cx - cw * 0.5f, rTop + s * 0.02f, stroke);
                canvas.drawLine(cx + cw * 0.5f, rTop - s * 0.08f, cx + cw * 0.5f, rTop + s * 0.02f, stroke);
                break;
            }
            case CoverActivity.PAGE_WEATHER: { // Sun + Cloud
                float cloudX = cx + s * 0.06f;
                float cloudY = cy + s * 0.10f;
                float cloudR = s * 0.22f;
                // Sun crescent behind cloud
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawCircle(cx - s * 0.14f, cy - s * 0.12f, s * 0.16f, stroke);
                // Cloud base
                path.addRoundRect(cloudX - cloudR * 1.3f, cloudY - cloudR * 0.5f,
                        cloudX + cloudR * 1.2f, cloudY + cloudR * 0.6f, cloudR * 0.55f, cloudR * 0.55f, Path.Direction.CW);
                path.addCircle(cloudX - cloudR * 0.4f, cloudY - cloudR * 0.5f, cloudR * 0.55f, Path.Direction.CW);
                path.addCircle(cloudX + cloudR * 0.4f, cloudY - cloudR * 0.2f, cloudR * 0.45f, Path.Direction.CW);
                fill.setColor(0xFF000000); // punch out
                canvas.drawPath(path, fill);
                fill.setColor(ink);
                stroke.setStyle(Paint.Style.STROKE);
                canvas.drawPath(path, stroke);
                break;
            }
            case CoverActivity.PAGE_RECORDER: { // Modern Microphone
                float micW = s * 0.16f;
                float micH = s * 0.32f;
                float micY = cy - s * 0.06f;
                // Mic capsule
                RectF micCapsule = new RectF(cx - micW, micY - micH / 2f, cx + micW, micY + micH / 2f);
                fill.setColor(ink);
                canvas.drawRoundRect(micCapsule, micW, micW, fill);
                // U-bracket
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                path.arcTo(new RectF(cx - micW * 1.6f, micY - micH * 0.2f, cx + micW * 1.6f, micY + micH * 0.8f), 0, 180);
                canvas.drawPath(path, stroke);
                // Stem
                canvas.drawLine(cx, micY + micH * 0.8f, cx, micY + micH * 1.2f, stroke);
                // Red recording dot if recording
                if (RecorderService.isRecording()) {
                    fill.setColor(DesignSystem.COLOR_ACCENT_RED);
                    canvas.drawCircle(r.right - s * 0.08f, r.top + s * 0.08f, s * 0.12f, fill);
                }
                break;
            }
            case CoverActivity.PAGE_HOME: { // Smart Home
                if (drawApp(canvas, appIcon(context, HomeAssistant.PACKAGE), r, on)) {
                    return;
                }
                float hw = s * 0.36f;
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                stroke.setStrokeJoin(Paint.Join.ROUND);
                // Roof
                path.moveTo(cx - hw * 1.1f, cy - s * 0.02f);
                path.lineTo(cx, cy - s * 0.38f);
                path.lineTo(cx + hw * 1.1f, cy - s * 0.02f);
                // Walls
                path.lineTo(cx + hw * 0.85f, cy + s * 0.34f);
                path.lineTo(cx - hw * 0.85f, cy + s * 0.34f);
                path.close();
                canvas.drawPath(path, stroke);
                // Door
                canvas.drawRoundRect(cx - hw * 0.3f, cy + s * 0.10f, cx + hw * 0.3f, cy + s * 0.34f, strokeW, strokeW, stroke);
                break;
            }
            case CoverActivity.PAGE_APPS: { // 4 Modern Squircles (Grid)
                float sz = s * 0.28f;
                float sp = s * 0.07f;
                stroke.setStyle(Paint.Style.STROKE);
                canvas.drawRoundRect(cx - sz - sp, cy - sz - sp, cx - sp, cy - sp, strokeW * 1.5f, strokeW * 1.5f, stroke);
                canvas.drawRoundRect(cx + sp, cy - sz - sp, cx + sz + sp, cy - sp, strokeW * 1.5f, strokeW * 1.5f, stroke);
                canvas.drawRoundRect(cx - sz - sp, cy + sp, cx - sp, cy + sz + sp, strokeW * 1.5f, strokeW * 1.5f, stroke);
                canvas.drawRoundRect(cx + sp, cy + sp, cx + sz + sp, cy + sz + sp, strokeW * 1.5f, strokeW * 1.5f, stroke);
                break;
            }
            case CoverActivity.PAGE_ASSISTANT: { // Futuristic AI Sparkle Orb
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                stroke.setStrokeJoin(Paint.Join.ROUND);
                float starR = s * 0.42f;
                path.moveTo(cx, cy - starR);
                path.quadTo(cx, cy, cx + starR, cy);
                path.quadTo(cx, cy, cx, cy + starR);
                path.quadTo(cx, cy, cx - starR, cy);
                path.quadTo(cx, cy, cx, cy - starR);
                fill.setColor((ink & 0x00FFFFFF) | (on ? 0x33000000 : 0x15000000));
                canvas.drawPath(path, fill);
                canvas.drawPath(path, stroke);
                break;
            }
            default: {
                if (!CoverActivity.PAGE_MUSIC.equals(page)) {
                    String key = page.substring(CoverActivity.PAGE_MUSIC.length() + 1);
                    int slash = key.indexOf('/');
                    if (slash > 0 && drawApp(canvas, appIcon(context, key.substring(0, slash)), r, on)) {
                        return;
                    }
                }
                // Music notes / Sound wave
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                float barH = s * 0.36f;
                canvas.drawLine(cx - s * 0.28f, cy - barH * 0.3f, cx - s * 0.28f, cy + barH * 0.3f, stroke);
                canvas.drawLine(cx - s * 0.10f, cy - barH * 0.8f, cx - s * 0.10f, cy + barH * 0.8f, stroke);
                canvas.drawLine(cx + s * 0.08f, cy - barH * 1.0f, cx + s * 0.08f, cy + barH * 1.0f, stroke);
                canvas.drawLine(cx + s * 0.26f, cy - barH * 0.5f, cx + s * 0.26f, cy + barH * 0.5f, stroke);
                break;
            }
        }
    }

    private static boolean drawApp(Canvas canvas, Drawable icon, RectF r, boolean on) {
        if (icon == null) {
            return false;
        }
        icon.setBounds(Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom));
        icon.setAlpha(on ? 255 : DIM_ALPHA);
        icon.draw(canvas);
        return true;
    }

    static Drawable appIcon(Context context, String packageName) {
        if (!appIcons.containsKey(packageName)) {
            Drawable icon;
            try {
                icon = context.getPackageManager().getApplicationIcon(packageName).mutate();
            } catch (PackageManager.NameNotFoundException e) {
                icon = null;
            }
            appIcons.put(packageName, icon);
        }
        return appIcons.get(packageName);
    }
}
