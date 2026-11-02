package dev.lcdclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Completely Redesigned External-Display Music Player.
 * Features:
 * - Left: High-resolution album artwork card with smooth 12px rounded corners and glass contour.
 * - Right: Player app badge, marquee track title, artist name, and modern 3px progress track with scrub knob.
 * - Bottom: Modern floating dock with tactile pill controls (⏮, -5s, Play/Pause circle, +5s, ⏭).
 */
final class MusicView extends View {
    private static final int NONE = -1;
    private static final int PREV = 2;
    private static final int BACK = 3;
    private static final int PLAY = 4;
    private static final int FORWARD = 5;
    private static final int NEXT = 6;
    private static final int[] ROW = {PREV, BACK, PLAY, FORWARD, NEXT};
    private static final int BAR = 7;

    private static final long SEEK_MS = 5000;
    private static final long TICK_MS = 250;
    private static final long MARQUEE_HOLD_MS = 1500;

    private final String key;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint artPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path path = new Path();
    private final Map<Integer, RectF> hits = new LinkedHashMap<>();

    private int pressed = NONE;
    private long scrubTo = -1;
    private float barLeft;
    private float barRight;

    private MediaController controller;
    private PlaybackState state;
    private MediaMetadata metadata;
    private boolean allowed;
    private String title = "";
    private String artist = "";
    private float boxH;
    private long titleSince;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            invalidate();
            postDelayed(this, TICK_MS);
        }
    };

    private final Runnable changed = () -> {
        refresh();
        invalidate();
    };

    MusicView(Context context, String key) {
        super(context);
        this.key = key;
        refresh();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Media.listen(changed);
        post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        Media.unlisten(changed);
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    private void refresh() {
        Context context = getContext();
        controller = key == null ? Media.current(context) : Media.session(context, key);
        state = controller == null ? null : controller.getPlaybackState();
        metadata = controller == null ? null : controller.getMetadata();
        allowed = controller != null || Media.allowed(context);

        String nowTitle;
        String nowArtist = "";
        if (!allowed) {
            nowTitle = "Grant notification access";
        } else if (controller == null) {
            nowTitle = "No music playing";
        } else {
            nowTitle = Media.title(metadata);
            if (nowTitle.isEmpty()) {
                nowTitle = "Unknown track";
            }
            if (metadata != null) {
                String a = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                if (a != null) nowArtist = a;
            }
        }

        if (!nowTitle.equals(title)) {
            title = nowTitle;
            titleSince = SystemClock.uptimeMillis();
        }
        artist = nowArtist;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(DesignSystem.COLOR_BG);
        hits.clear();
        int accent = Prefs.color(getContext());

        float bw = getWidth();
        float bh = getHeight();
        if (bw / bh < DesignSystem.REF_WIDTH / DesignSystem.REF_HEIGHT) {
            bh = bw * DesignSystem.REF_HEIGHT / DesignSystem.REF_WIDTH;
        } else {
            bw = bh * DesignSystem.REF_WIDTH / DesignSystem.REF_HEIGHT;
        }
        float bx = (getWidth() - bw) / 2f;
        float by = (getHeight() - bh) / 2f;
        float pad = bh * 0.05f;
        boxH = bh;

        // Top tab header area
        float tabBase = by + pad + bh * CoverTabs.TAB_H;

        // Bottom controls dock
        float btnH = bh * 0.26f;
        float btnTop = by + bh - pad - btnH;

        // Middle content area
        float midTop = tabBase + pad * 0.8f;
        float midH = btnTop - pad * 0.6f - midTop;

        // 1. Left side: Album Art Card (Square)
        float artSize = midH;
        RectF artRect = new RectF(bx + pad * 1.2f, midTop, bx + pad * 1.2f + artSize, midTop + artSize);
        drawAlbumArt(canvas, artRect, accent);

        // 2. Right side: Track Info, Progress Bar & Timestamps
        float infoLeft = artRect.right + pad * 1.4f;
        float infoRight = bx + bw - pad * 1.2f;
        drawTrackInfo(canvas, infoLeft, infoRight, midTop, midH, accent);

        // 3. Bottom Dock Controls
        drawControlsDock(canvas, bx + pad * 1.2f, btnTop, bw - pad * 2.4f, btnH, accent);
    }

    private void drawAlbumArt(Canvas canvas, RectF r, int accent) {
        Bitmap art = Media.art(metadata);
        float radius = DesignSystem.RADIUS_MEDIUM;

        if (art != null) {
            canvas.save();
            path.reset();
            path.addRoundRect(r, radius, radius, Path.Direction.CW);
            canvas.clipPath(path);
            int w = art.getWidth();
            int h = art.getHeight();
            int side = Math.min(w, h);
            canvas.drawBitmap(art, new Rect((w - side) / 2, (h - side) / 2, (w + side) / 2, (h + side) / 2), r, artPaint);
            canvas.restore();

            // 1px subtle contour
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(DesignSystem.COLOR_BORDER);
            canvas.drawRoundRect(r, radius, radius, paint);
        } else {
            // Placeholder: Elegant dark vinyl surface card
            DesignSystem.drawCard(canvas, r.left, r.top, r.right, r.bottom, radius,
                    DesignSystem.COLOR_CARD_ELEVATED, DesignSystem.COLOR_BORDER_SUBTLE);

            // Vinyl center circles
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(DesignSystem.COLOR_BORDER_FOCUS);
            canvas.drawCircle(r.centerX(), r.centerY(), r.width() * 0.32f, paint);
            canvas.drawCircle(r.centerX(), r.centerY(), r.width() * 0.18f, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(accent);
            canvas.drawCircle(r.centerX(), r.centerY(), r.width() * 0.08f, paint);
        }
    }

    private void drawTrackInfo(Canvas canvas, float left, float right, float top, float h, int accent) {
        float availW = right - left;

        // App badge or player title
        String appName = controller != null ? controller.getPackageName() : "MUSIC";
        if (appName.contains(".")) {
            appName = appName.substring(appName.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        }
        paint.setTextSize(h * 0.13f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        paint.setColor(accent);
        canvas.drawText(appName, left, top + h * 0.16f, paint);

        // Track Title (bold white, smooth marquee if long)
        paint.setTextSize(h * 0.22f);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);

        float textW = paint.measureText(title);
        float titleY = top + h * 0.44f;
        if (textW <= availW) {
            canvas.drawText(title, left, titleY, paint);
        } else {
            // Marquee
            canvas.save();
            canvas.clipRect(left, top, right, top + h * 0.55f);
            long elapsed = SystemClock.uptimeMillis() - titleSince;
            float shift = (elapsed < MARQUEE_HOLD_MS) ? 0 : ((elapsed - MARQUEE_HOLD_MS) / 1000f) * boxH * 0.35f;
            float loopW = textW + availW * 0.4f;
            float off = shift % loopW;
            canvas.drawText(title, left - off, titleY, paint);
            canvas.drawText(title, left - off + loopW, titleY, paint);
            canvas.restore();
        }

        // Artist Name (secondary gray)
        paint.setTextSize(h * 0.14f);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
        String dispArtist = artist.isEmpty() ? "Audio Session" : artist;
        canvas.drawText(DesignSystem.ellipsize(dispArtist, paint, availW), left, top + h * 0.64f, paint);

        // 3. Progress Track & Timestamps
        long duration = Media.duration(metadata);
        long position = scrubTo >= 0 ? scrubTo : controller == null ? 0 : Media.position(controller, state);
        float trackY = top + h * 0.82f;
        float trackH = 3.5f;

        // Background Track
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DesignSystem.COLOR_BORDER_SUBTLE);
        canvas.drawRoundRect(left, trackY - trackH / 2f, right, trackY + trackH / 2f, trackH / 2f, trackH / 2f, paint);

        // Active Track
        if (duration > 0 && position >= 0) {
            float progress = Math.min(1f, (float) position / duration);
            float activeRight = left + availW * progress;
            paint.setColor(accent);
            canvas.drawRoundRect(left, trackY - trackH / 2f, activeRight, trackY + trackH / 2f, trackH / 2f, trackH / 2f, paint);

            // Scrub Knob
            canvas.drawCircle(activeRight, trackY, (scrubTo >= 0 ? 6.5f : 4.5f), paint);

            barLeft = left;
            barRight = right;
            hits.put(BAR, new RectF(left - 8f, trackY - 14f, right + 8f, trackY + 14f));
        }

        // Timestamps underneath
        paint.setTextSize(h * 0.11f);
        paint.setColor(DesignSystem.COLOR_TEXT_TERTIARY);
        String posStr = formatTime(position);
        String durStr = formatTime(duration);
        canvas.drawText(posStr, left, top + h * 0.98f, paint);

        float durW = paint.measureText(durStr);
        canvas.drawText(durStr, right - durW, top + h * 0.98f, paint);
    }

    private void drawControlsDock(Canvas canvas, float x, float top, float w, float h, int accent) {
        int n = ROW.length;
        float gap = w * 0.025f;
        float totalGaps = gap * (n - 1);
        float unitW = (w - totalGaps) / (n + 0.4f); // play button is slightly wider

        float curX = x;
        for (int id : ROW) {
            float btnW = (id == PLAY) ? unitW * 1.4f : unitW;
            RectF r = new RectF(curX, top, curX + btnW, top + h);
            boolean isPlay = id == PLAY;
            boolean enabled = enabled(id);

            int bg;
            int border;
            if (isPlay) {
                bg = pressed == id ? DesignSystem.COLOR_CARD_PRESSED : accent;
                border = 0;
            } else {
                bg = pressed == id ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_CARD;
                border = DesignSystem.COLOR_BORDER_SUBTLE;
            }

            DesignSystem.drawPill(canvas, r.left, r.top, r.right, r.bottom, bg, border);

            // Draw icon
            drawDockIcon(canvas, id, r, isPlay ? Color.BLACK : enabled ? Color.WHITE : DesignSystem.COLOR_TEXT_MUTED);

            if (enabled) {
                hits.put(id, r);
            }
            curX += btnW + gap;
        }
    }

    private void drawDockIcon(Canvas canvas, int id, RectF r, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        float cx = r.centerX();
        float cy = r.centerY();
        float sz = r.height() * 0.32f;

        path.reset();
        switch (id) {
            case PREV -> {
                // |◀
                canvas.drawRect(cx - sz * 0.7f, cy - sz * 0.6f, cx - sz * 0.45f, cy + sz * 0.6f, paint);
                path.moveTo(cx + sz * 0.6f, cy - sz * 0.6f);
                path.lineTo(cx - sz * 0.35f, cy);
                path.lineTo(cx + sz * 0.6f, cy + sz * 0.6f);
                path.close();
                canvas.drawPath(path, paint);
            }
            case NEXT -> {
                // ▶|
                canvas.drawRect(cx + sz * 0.45f, cy - sz * 0.6f, cx + sz * 0.7f, cy + sz * 0.6f, paint);
                path.moveTo(cx - sz * 0.6f, cy - sz * 0.6f);
                path.lineTo(cx + sz * 0.35f, cy);
                path.lineTo(cx - sz * 0.6f, cy + sz * 0.6f);
                path.close();
                canvas.drawPath(path, paint);
            }
            case BACK -> {
                paint.setTextSize(sz * 0.85f);
                paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("-5", cx, cy + sz * 0.30f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
            case FORWARD -> {
                paint.setTextSize(sz * 0.85f);
                paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("+5", cx, cy + sz * 0.30f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
            case PLAY -> {
                if (Media.isActive(state)) {
                    // Modern pause bars
                    float barW = sz * 0.28f;
                    float space = sz * 0.25f;
                    canvas.drawRoundRect(cx - space / 2f - barW, cy - sz * 0.6f, cx - space / 2f, cy + sz * 0.6f, 2f, 2f, paint);
                    canvas.drawRoundRect(cx + space / 2f, cy - sz * 0.6f, cx + space / 2f + barW, cy + sz * 0.6f, 2f, 2f, paint);
                } else {
                    // Modern play triangle
                    path.moveTo(cx - sz * 0.4f, cy - sz * 0.6f);
                    path.lineTo(cx + sz * 0.6f, cy);
                    path.lineTo(cx - sz * 0.4f, cy + sz * 0.6f);
                    path.close();
                    canvas.drawPath(path, paint);
                }
            }
        }
    }

    private static String formatTime(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms / 1000;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private boolean enabled(int id) {
        if (controller == null) {
            return id == PLAY && allowed;
        }
        long actions = state == null ? 0 : state.getActions();
        long needed = switch (id) {
            case PREV -> PlaybackState.ACTION_SKIP_TO_PREVIOUS;
            case NEXT -> PlaybackState.ACTION_SKIP_TO_NEXT;
            case BACK, FORWARD -> PlaybackState.ACTION_SEEK_TO;
            default -> PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE;
        };
        return actions == 0 || (actions & needed) != 0;
    }

    private int hit(float x, float y) {
        for (Map.Entry<Integer, RectF> e : hits.entrySet()) {
            if (e.getKey() != BAR && e.getValue().contains(x, y)) {
                return e.getKey();
            }
        }
        RectF bar = hits.get(BAR);
        return bar != null && bar.contains(x, y) ? BAR : NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                pressed = hit(event.getX(), event.getY());
                if (pressed == BAR) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    scrubTo = scrubTime(event.getX());
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (pressed == BAR) {
                    scrubTo = scrubTime(event.getX());
                    invalidate();
                } else if (pressed != NONE && hit(event.getX(), event.getY()) != pressed) {
                    pressed = NONE;
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                int control = pressed;
                pressed = NONE;
                if (control == BAR) {
                    long target = scrubTime(event.getX());
                    scrubTo = -1;
                    if (controller != null) {
                        Media.seekTo(controller, metadata, target);
                    }
                } else if (control != NONE && hit(event.getX(), event.getY()) == control) {
                    perform(control);
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL -> {
                pressed = NONE;
                scrubTo = -1;
                invalidate();
                return true;
            }
            default -> {
                return super.onTouchEvent(event);
            }
        }
    }

    private long scrubTime(float x) {
        float f = Math.max(0, Math.min(1, (x - barLeft) / Math.max(1, barRight - barLeft)));
        return (long) (f * Media.duration(metadata));
    }

    private void perform(int control) {
        Context context = getContext();
        switch (control) {
            case PLAY -> Media.playPause(context, controller, state);
            case PREV -> controller.getTransportControls().skipToPrevious();
            case NEXT -> controller.getTransportControls().skipToNext();
            case BACK, FORWARD -> Media.seekBy(controller, state, metadata, control == FORWARD ? SEEK_MS : -SEEK_MS);
            default -> {
                return;
            }
        }
        context.getSystemService(VibratorManager.class).getDefaultVibrator()
                .vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
    }
}
