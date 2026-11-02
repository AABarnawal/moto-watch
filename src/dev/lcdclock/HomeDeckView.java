package dev.lcdclock;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;
import java.util.List;

/**
 * Modern Redesigned External-Display Home Screen Deck.
 * Integrates:
 * - Selected Clock Face style (via ClockFaceEngine)
 * - Battery percentage and status pill
 * - Glanceable bottom widgets (Weather, Media playback, Notifications)
 * - Touch dispatch for widget shortcuts.
 */
public final class HomeDeckView extends View {
    public interface Listener {
        void onOpenPage(String page);
    }

    private static final int HIT_WEATHER = 1;
    private static final int HIT_MUSIC = 2;
    private static final int HIT_NOTICES = 3;

    private final Listener listener;
    private final SegmentPainter seg;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF weatherBounds = new RectF();
    private final RectF musicBounds = new RectF();
    private final RectF noticesBounds = new RectF();

    private Calendar now = Calendar.getInstance();
    private int batteryPercent = 100;
    private boolean isCharging = false;
    private boolean alarmSet = false;
    private int pressedHit = 0;
    private long lastTapTime = 0;
    private float downX = 0f;
    private float downY = 0f;

    private final Runnable secondTick = new Runnable() {
        @Override
        public void run() {
            now = Calendar.getInstance();
            invalidate();
            postDelayed(this, 1000 - (System.currentTimeMillis() % 1000));
        }
    };

    public HomeDeckView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        this.seg = new SegmentPainter(context);
        updateBattery();
        updateAlarm();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateBattery();
        updateAlarm();
        removeCallbacks(secondTick);
        post(secondTick);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(secondTick);
        super.onDetachedFromWindow();
    }

    private void updateBattery() {
        Intent intent = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent != null) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPercent = (level >= 0 && scale > 0) ? Math.round((level * 100f) / scale) : 100;
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        }
    }

    private void updateAlarm() {
        alarmSet = getContext().getSystemService(AlarmManager.class).getNextAlarmClock() != null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Keep 368:194 aspect ratio centered
        float aspect = DesignSystem.REF_WIDTH / DesignSystem.REF_HEIGHT;
        float bw = w;
        float bh = bw / aspect;
        if (bh > h) {
            bh = h;
            bw = bh * aspect;
        }
        float bx = (w - bw) / 2f;
        float by = (h - bh) / 2f;

        // 1. Top Status Row: Battery Pill & Lock Icon
        drawTopStatusBar(canvas, bx, by, bw, bh);

        // 2. Primary Clock Area (60% of viewport)
        String style = Prefs.clockStyle(getContext());
        int color = Prefs.color(getContext());
        boolean is24h = DateFormat.is24HourFormat(getContext());
        boolean locked = Lock.locked(getContext());

        ClockFaceEngine.drawFace(canvas, getContext(), style, bx, by + bh * 0.05f, bw, bh * 0.70f,
                now, is24h, color, alarmSet, locked, seg);

        // 3. Bottom Glanceable Widgets Bar
        drawGlanceableWidgets(canvas, bx, by + bh * 0.74f, bw, bh * 0.22f);
    }

    private void drawTopStatusBar(Canvas canvas, float bx, float by, float bw, float bh) {
        float statusY = by + bh * 0.12f;

        // Battery pill on top right
        String batText = batteryPercent + "%" + (isCharging ? " ⚡" : "");
        paint.setTextSize(bh * 0.075f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        float batW = paint.measureText(batText);

        float padH = bh * 0.04f;
        float pillW = batW + padH * 2f;
        float pillH = bh * 0.11f;
        float pillX = bx + bw - pillW - padH;
        float pillY = statusY - pillH * 0.75f;

        int batColor = batteryPercent <= 20 ? DesignSystem.COLOR_ACCENT_RED : DesignSystem.COLOR_TEXT_PRIMARY;
        DesignSystem.drawPill(canvas, pillX, pillY, pillX + pillW, pillY + pillH,
                DesignSystem.COLOR_CARD, DesignSystem.COLOR_BORDER_SUBTLE);

        paint.setColor(batColor);
        canvas.drawText(batText, pillX + padH, statusY, paint);

        // Notification dot indicator on top left if unread notifications exist
        if (Notices.count(getContext()) > 0) {
            paint.setColor(DesignSystem.COLOR_ACCENT_AQUA);
            canvas.drawCircle(bx + padH * 2f, statusY - pillH * 0.25f, bh * 0.025f, paint);
        }
    }

    private void drawGlanceableWidgets(Canvas canvas, float bx, float y, float bw, float h) {
        float pad = bw * 0.03f;
        float curX = bx + pad;
        float maxW = bw - pad * 2f;

        // Widget 1: Weather Glance (if cached)
        Weather weather = Weather.get(getContext(), w -> invalidate());
        if (weather != null && weather.place != null) {
            String wText = weather.temp + "° " + weather.place;
            paint.setTextSize(h * 0.40f);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            float tw = paint.measureText(wText);
            float chipW = Math.min(maxW * 0.48f, tw + pad * 1.5f);

            weatherBounds.set(curX, y, curX + chipW, y + h);
            int bg = pressedHit == HIT_WEATHER ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_CARD;
            DesignSystem.drawPill(canvas, weatherBounds.left, weatherBounds.top, weatherBounds.right, weatherBounds.bottom,
                    bg, DesignSystem.COLOR_BORDER_SUBTLE);

            paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);
            String truncated = DesignSystem.ellipsize(wText, paint, chipW - pad);
            canvas.drawText(truncated, curX + pad * 0.6f, y + h * 0.65f, paint);

            curX += chipW + pad * 0.6f;
        }

        // Widget 2: Active Music Track Glance (if playing)
        MediaController mc = Media.current(getContext());
        if (mc != null) {
            MediaMetadata md = mc.getMetadata();
            String title = (md != null && md.getString(MediaMetadata.METADATA_KEY_TITLE) != null)
                    ? md.getString(MediaMetadata.METADATA_KEY_TITLE) : "Music Playing";
            String mText = "🎵 " + title;

            paint.setTextSize(h * 0.40f);
            float mw = paint.measureText(mText);
            float avail = bx + bw - pad - curX;
            float chipW = Math.max(pad * 3f, Math.min(avail, mw + pad * 1.5f));

            if (chipW > pad * 2f) {
                musicBounds.set(curX, y, curX + chipW, y + h);
                int bg = pressedHit == HIT_MUSIC ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_CARD;
                DesignSystem.drawPill(canvas, musicBounds.left, musicBounds.top, musicBounds.right, musicBounds.bottom,
                        bg, DesignSystem.COLOR_BORDER_SUBTLE);

                paint.setColor(DesignSystem.COLOR_ACCENT_AQUA);
                String truncated = DesignSystem.ellipsize(mText, paint, chipW - pad);
                canvas.drawText(truncated, curX + pad * 0.6f, y + h * 0.65f, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                downX = x;
                downY = y;
                if (weatherBounds.contains(x, y)) {
                    pressedHit = HIT_WEATHER;
                    invalidate();
                    return true;
                } else if (musicBounds.contains(x, y)) {
                    pressedHit = HIT_MUSIC;
                    invalidate();
                    return true;
                }
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                if (pressedHit == HIT_WEATHER && weatherBounds.contains(x, y)) {
                    if (listener != null) listener.onOpenPage(CoverActivity.PAGE_WEATHER);
                } else if (pressedHit == HIT_MUSIC && musicBounds.contains(x, y)) {
                    if (listener != null) listener.onOpenPage(CoverActivity.PAGE_MUSIC);
                } else if (pressedHit == 0 && Math.abs(x - downX) < 20 && Math.abs(y - downY) < 20) {
                    long nowMs = System.currentTimeMillis();
                    if (nowMs - lastTapTime < 380) {
                        cycleClockStyle();
                        lastTapTime = 0;
                        pressedHit = 0;
                        return true;
                    }
                    lastTapTime = nowMs;
                }
                pressedHit = 0;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL -> {
                pressedHit = 0;
                invalidate();
            }
        }
        return super.onTouchEvent(event);
    }

    private void cycleClockStyle() {
        String current = Prefs.clockStyle(getContext());
        int idx = 0;
        for (int i = 0; i < ClockFaceEngine.ALL_STYLES.length; i++) {
            if (ClockFaceEngine.ALL_STYLES[i].equals(current)) {
                idx = (i + 1) % ClockFaceEngine.ALL_STYLES.length;
                break;
            }
        }
        Prefs.setClockStyle(getContext(), ClockFaceEngine.ALL_STYLES[idx]);
        invalidate();
    }
}
