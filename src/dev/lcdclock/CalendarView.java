package dev.lcdclock;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The cover screen's CALENDAR page: events from now to a week ahead from the phone's visible
 * calendars, two rows to the screen: a bar in the calendar's colour, the start time in segment
 * digits (lit while it's on), the title (scrolling when it's too long), and the day and place
 * underneath. Drag up or down to scroll.
 */
final class CalendarView extends CoverPage {
    private static final long WEEK_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long RELOAD_MS = 60_000;
    /** Events to the screen; everything in a row scales with its height. */
    private static final int ROWS = 2;
    private static final ExecutorService loader = Executors.newSingleThreadExecutor();

    private static final class Event {
        String title;
        long begin;
        long end;
        boolean allDay;
        int color;
        String place;
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private List<Event> events = Collections.emptyList();
    private boolean loaded;
    /** Rows scrolled past, in px, and the drag that's moving them. */
    private float scroll;
    private float dragY;
    private float rowH;
    private float maxScroll;

    private final Runnable reload = new Runnable() {
        @Override
        public void run() {
            load();
            postDelayed(this, RELOAD_MS);
        }
    };

    CalendarView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(reload);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(reload);
        super.onDetachedFromWindow();
    }

    private boolean allowed() {
        return getContext().checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private void load() {
        if (!allowed()) {
            invalidate();
            return;
        }
        Context context = getContext().getApplicationContext();
        loader.execute(() -> {
            List<Event> found = query(context);
            main.post(() -> {
                events = found;
                loaded = true;
                invalidate();
            });
        });
    }

    private static List<Event> query(Context context) {
        long now = System.currentTimeMillis();
        Uri.Builder uri = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uri, now - 24L * 60 * 60 * 1000); // all-day events of today start at midnight UTC
        ContentUris.appendId(uri, now + WEEK_MS);
        String[] columns = {CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR, CalendarContract.Instances.EVENT_LOCATION};
        List<Event> events = new ArrayList<>();
        try (Cursor c = context.getContentResolver().query(uri.build(), columns,
                CalendarContract.Instances.VISIBLE + " = 1", null, CalendarContract.Instances.BEGIN + " ASC")) {
            while (c != null && c.moveToNext()) {
                Event e = new Event();
                e.title = c.getString(0) == null || c.getString(0).isEmpty() ? "(No title)" : c.getString(0);
                e.begin = c.getLong(1);
                e.end = c.getLong(2);
                e.allDay = c.getInt(3) != 0;
                e.color = c.getInt(4) | 0xFF000000;
                e.place = c.getString(5);
                if (e.allDay) {
                    // Stored as UTC midnights; read them as local dates.
                    e.begin = localMidnight(e.begin);
                    e.end = localMidnight(e.end);
                }
                if (e.end > now) {
                    events.add(e);
                }
            }
        } catch (RuntimeException ex) {
            Log.w(Binders.TAG, "can't read calendars", ex);
        }
        events.sort((a, b) -> Long.compare(a.begin, b.begin)); // all-day dates moved to local time
        return events;
    }

    private static long localMidnight(long utcMidnight) {
        Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnight);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local.getTimeInMillis();
    }

    @Override
    void drawPage(Canvas canvas, int color) {
        Context context = getContext();
        float left = bx + pad * 1.0f;
        float right = bx + bw - pad * 1.0f;
        float bottom = by + bh - pad * 0.8f;

        Calendar today = Calendar.getInstance();
        String todayStr = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(today.getTime());

        if (!allowed() || (loaded && events.isEmpty())) {
            // Prominent modern empty calendar card with today's date
            RectF emptyCard = new RectF(left, top + pad * 0.5f, right, bottom);
            fill.setColor(DesignSystem.COLOR_CARD);
            canvas.drawRoundRect(emptyCard, 12f, 12f, fill);
            outline.setStrokeWidth(1.2f);
            outline.setColor(DesignSystem.COLOR_BORDER);
            canvas.drawRoundRect(emptyCard, 12f, 12f, outline);

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
            p.setTextSize(bh * 0.16f);
            p.setColor(Color.WHITE);
            canvas.drawText(todayStr, emptyCard.left + pad * 1.2f, emptyCard.top + bh * 0.24f, p);

            p.setTextSize(bh * 0.12f);
            p.setColor(allowed() ? DesignSystem.COLOR_TEXT_SECONDARY : 0xFFFF453A);
            String msg = !allowed() ? context.getString(R.string.calendar_needs_access) : "No upcoming events scheduled";
            canvas.drawText(msg, emptyCard.left + pad * 1.2f, emptyCard.top + bh * 0.44f, p);
            return;
        }

        rowH = (bottom - top) / ROWS;
        maxScroll = Math.max(0, events.size() * rowH - (bottom - top));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        long now = System.currentTimeMillis();
        boolean twelveHour = !DateFormat.is24HourFormat(context);
        boolean scrolling = false;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        canvas.save();
        canvas.clipRect(bx, top, bx + bw, bottom);
        for (int i = 0; i < events.size(); i++) {
            float rowTop = top + i * rowH - scroll;
            if (rowTop > bottom || rowTop + rowH < top) {
                continue;
            }
            Event e = events.get(i);
            boolean on = e.begin <= now && now < e.end;

            // Elevated event card
            RectF card = new RectF(left, rowTop + 2.5f, right, rowTop + rowH - 2.5f);
            int cardBg = on ? (0x33000000 | (e.color & 0x00FFFFFF)) : DesignSystem.COLOR_CARD;
            fill.setColor(cardBg);
            canvas.drawRoundRect(card, 10f, 10f, fill);
            outline.setStrokeWidth(1.2f);
            outline.setColor(on ? e.color : DesignSystem.COLOR_BORDER);
            canvas.drawRoundRect(card, 10f, 10f, outline);

            // Left Date Badge: Weekday on top, bold day number below
            Calendar at = Calendar.getInstance();
            at.setTimeInMillis(e.begin);
            String dayOfWeek = new SimpleDateFormat("EEE", Locale.getDefault()).format(at.getTime()).toUpperCase(Locale.ROOT);
            String dayNum = String.valueOf(at.get(Calendar.DAY_OF_MONTH));

            float badgeW = rowH * 0.78f;
            RectF dateBadge = new RectF(card.left + pad * 0.6f, card.top + pad * 0.5f, card.left + pad * 0.6f + badgeW, card.bottom - pad * 0.5f);
            fill.setColor(on ? (0x44000000 | (e.color & 0x00FFFFFF)) : 0x221E1E24);
            canvas.drawRoundRect(dateBadge, 8f, 8f, fill);
            outline.setStrokeWidth(1.0f);
            outline.setColor(on ? e.color : DesignSystem.COLOR_BORDER_SUBTLE);
            canvas.drawRoundRect(dateBadge, 8f, 8f, outline);

            // Draw Weekday in badge
            textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
            textPaint.setTextSize(dateBadge.height() * 0.32f);
            textPaint.setColor(on ? e.color : DesignSystem.COLOR_ACCENT_CYAN);
            float dowW = textPaint.measureText(dayOfWeek);
            canvas.drawText(dayOfWeek, dateBadge.centerX() - dowW / 2, dateBadge.top + dateBadge.height() * 0.38f, textPaint);

            // Draw Day number in badge
            textPaint.setTextSize(dateBadge.height() * 0.48f);
            textPaint.setColor(Color.WHITE);
            float dNumW = textPaint.measureText(dayNum);
            canvas.drawText(dayNum, dateBadge.centerX() - dNumW / 2, dateBadge.bottom - dateBadge.height() * 0.14f, textPaint);

            // Right Event Content
            float contentX = dateBadge.right + pad * 0.8f;
            float contentMaxW = card.right - pad * 0.8f - contentX;

            // Line 1: Bold Event Title (Large, 16px+)
            float titleSize = rowH * 0.36f;
            textPaint.setTextSize(titleSize);
            textPaint.setColor(Color.WHITE);
            scrolling |= marquee(canvas, e.title, contentX, card.top + rowH * 0.42f, titleSize, contentMaxW, Color.WHITE);

            // Line 2: Time Range & Location
            String timeRange;
            if (e.allDay) {
                timeRange = "ALL DAY";
            } else {
                int hour = at.get(Calendar.HOUR_OF_DAY);
                int shown = twelveHour ? (hour % 12 == 0 ? 12 : hour % 12) : hour;
                String startStr = String.format(Locale.ROOT, twelveHour ? "%d:%02d" : "%02d:%02d", shown, at.get(Calendar.MINUTE));
                if (twelveHour) {
                    startStr += (hour < 12 ? " AM" : " PM");
                }
                timeRange = startStr;
            }
            String detail = (e.place == null || e.place.isEmpty()) ? timeRange : timeRange + " · " + e.place;
            line(canvas, detail, contentX, card.bottom - rowH * 0.16f, rowH * 0.28f, contentMaxW,
                    on ? e.color : DesignSystem.COLOR_TEXT_SECONDARY);
        }
        canvas.restore();
        if (scrolling) {
            postInvalidateOnAnimation();
        }

        // Modern sleek scroll bar
        scrollBar(canvas, top, bottom, events.size() * rowH, color);
    }

    private static String amPm(long when) {
        Calendar at = Calendar.getInstance();
        at.setTimeInMillis(when);
        return at.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
    }

    /** "Today", "Tomorrow", or "Mon 6 Oct", with AM/PM in 12-hour mode. */
    private String dayLabel(long when, long now, String amPm) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(when);
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(now);
        int days = (int) Math.round((startOfDay(day) - startOfDay(today)) / 86_400_000.0);
        String name;
        if (days <= 0) {
            name = getContext().getString(R.string.calendar_today);
        } else if (days == 1) {
            name = getContext().getString(R.string.calendar_tomorrow);
        } else {
            name = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM"),
                    Locale.getDefault()).format(day.getTime());
        }
        return amPm.isEmpty() ? name : name + " " + amPm;
    }

    private static long startOfDay(Calendar c) {
        Calendar d = (Calendar) c.clone();
        d.set(Calendar.HOUR_OF_DAY, 0);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        return d.getTimeInMillis();
    }

    /** Vertical drags scroll the list; sideways ones are SwipeFrame's. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                scroll = Math.max(0, Math.min(maxScroll, scroll - (event.getY() - dragY)));
                dragY = event.getY();
                invalidate();
                return true;
            default:
                return true;
        }
    }
}
