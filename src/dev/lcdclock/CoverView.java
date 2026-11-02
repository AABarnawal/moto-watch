package dev.lcdclock;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The cover-screen TIMER or ALARM page (one instance each), drawn in the clock face's LCD
 * style. The alarm is LCD Clock's own (AlarmController); tapping its bell turns it on or off, or
 * while snoozed (the bell blinks) ends the snooze.
 * Each page shows its own ringing: ALARM flashes the alarm time with SNOOZE and STOP, TIMER
 * flashes zero with STOP, and stopping one leaves the other ringing. Laid out for the
 * 368x194 outside screen; on any other shape it uses the largest box of that shape.
 */
final class CoverView extends View {
    private static final int NONE = -1;
    private static final int FIELD = 3; // FIELD + 0..2: hours, minutes, seconds
    private static final int MINUS = 7;
    private static final int PLUS = 8;
    private static final int START = 9;
    private static final int PAUSE = 10;
    private static final int GO = 11;
    private static final int RESET = 12;
    private static final int STOP = 13;
    private static final int SET = 14;
    private static final int BELL = 15;
    private static final int SNOOZE = 16;

    private static final int GREY = 0xFF7A7A7A;
    private static final int OUTLINE = 0xFFB4B4B4;
    private static final long REPEAT_DELAY_MS = 400;
    private static final long REPEAT_EVERY_MS = 90;
    /** AM/PM above the bell, after the alarm's minutes, in digit heights (as on the face). */
    private static final float COLUMN_GAP = 0.25f;
    private static final float AMPM_H = 0.28f;
    private static final float BELL_H = 0.32f;

    private final SegmentPainter seg;
    /** This instance is the ALARM tab; otherwise TIMER. */
    private final boolean alarmTab;
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Touch targets laid out by the last onDraw. */
    private final Map<Integer, RectF> hits = new LinkedHashMap<>();

    private int timerField = 1;
    private int alarmField = 0;
    private int pressed = NONE;
    /** The held − or + has started repeating, so letting go doesn't step again. */
    private boolean repeated;
    private String alarmMessage;
    private long alarmMessageUntil;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, nextTick());
        }
    };

    /**
     * Only as often as something on the page changes: the blink while ringing, the next second of a
     * running timer, otherwise once a second to catch changes made elsewhere (the notification).
     */
    private long nextTick() {
        Context context = getContext();
        if (CoverActivity.ringing(context)) {
            return 200;
        }
        if (!alarmTab && TimerController.state(context) == TimerController.State.RUNNING) {
            return TimerController.remaining(context) % 1000 + 10;
        }
        return 1000;
    }

    private final Runnable repeat = new Runnable() {
        @Override
        public void run() {
            if (pressed == MINUS || pressed == PLUS) {
                step(pressed == PLUS ? 1 : -1);
                repeated = true;
                handler.postDelayed(this, REPEAT_EVERY_MS);
            }
        }
    };

    CoverView(Context context, boolean alarmTab) {
        super(context);
        this.alarmTab = alarmTab;
        seg = new SegmentPainter(context);
        outline.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    // ---- drawing ----

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        hits.clear();
        Context context = getContext();
        int color = Prefs.color(context);
        TimerController.State timer = TimerController.state(context);
        boolean alarmRinging = alarmTab && AlarmController.state(context) == AlarmController.State.RINGING;

        // Largest 368:194 box, centred.
        float bw = getWidth();
        float bh = getHeight();
        if (bw / bh < 368f / 194f) {
            bh = bw * 194f / 368f;
        } else {
            bw = bh * 368f / 194f;
        }
        float bx = (getWidth() - bw) / 2;
        float by = (getHeight() - bh) / 2;
        float pad = bh * 0.05f;

        // The menu row above (tabs, or ALARM / TIME UP while ringing) is CoverTabs'.
        float tabBase = by + pad + bh * CoverTabs.TAB_H;

        float btnH = bh * 0.25f;
        float btnTop = by + bh - pad - btnH;
        float midTop = tabBase + pad;
        float midH = btnTop - pad * 0.8f - midTop;
        if (alarmRinging) {
            drawAlarm(canvas, color, bx, bw, pad, midTop, midH, true);
            buttons(canvas, color, bx, bw, pad, btnTop, btnH, new int[] {SNOOZE, STOP});
        } else if (alarmTab) {
            drawAlarm(canvas, color, bx, bw, pad, midTop, midH, false);
            buttons(canvas, color, bx, bw, pad, btnTop, btnH, new int[] {MINUS, PLUS, SET});
        } else {
            drawTimer(canvas, color, timer, bx, bw, pad, midTop, midH);
            int[] row;
            switch (timer) {
                case RUNNING:
                    row = new int[] {PAUSE, RESET};
                    break;
                case PAUSED:
                    row = new int[] {GO, RESET};
                    break;
                case RINGING:
                    row = new int[] {STOP};
                    break;
                default:
                    row = new int[] {MINUS, PLUS, START};
            }
            buttons(canvas, color, bx, bw, pad, btnTop, btnH, row);
        }
    }

    private void drawTimer(Canvas canvas, int color, TimerController.State state, float bx, float bw, float pad,
            float midTop, float midH) {
        boolean idle = state == TimerController.State.IDLE;
        long millis = TimerController.remaining(getContext());
        long secs = idle ? millis / 1000 : (millis + 999) / 1000;
        int[] values = {(int) (secs / 3600), (int) (secs / 60 % 60), (int) (secs % 60)};
        boolean lit = state != TimerController.State.RINGING || System.currentTimeMillis() / 400 % 2 == 0;

        float digitH = midH * 0.72f;
        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        timePaint.setTextSize(digitH);
        timePaint.setColor(lit ? (state == TimerController.State.RINGING ? 0xFFFF3B30 : Color.WHITE) : 0x44FFFFFF);

        String hStr = String.format(java.util.Locale.ROOT, "%02d", values[0]);
        String mStr = String.format(java.util.Locale.ROOT, "%02d", values[1]);
        String sStr = String.format(java.util.Locale.ROOT, "%02d", values[2]);
        float pairW = timePaint.measureText("00");
        float colonW = timePaint.measureText(":");
        float totalW = pairW * 3 + colonW * 2 + pad * 1.2f;

        float left = bx + (bw - totalW) / 2;
        float centerY = midTop + midH / 2;
        Paint.FontMetrics fm = timePaint.getFontMetrics();
        float baseline = centerY - (fm.ascent + fm.descent) / 2;

        float x = left;
        String[] parts = {hStr, mStr, sStr};
        for (int i = 0; i < 3; i++) {
            float pairLeft = x;
            float pairRight = x + pairW;
            RectF pairRect = new RectF(pairLeft - pad * 0.2f, centerY - digitH * 0.55f, pairRight + pad * 0.2f, centerY + digitH * 0.55f);

            if (idle && i == timerField) {
                fill.setColor(0x33000000 | (color & 0x00FFFFFF));
                canvas.drawRoundRect(pairRect, 8f, 8f, fill);
                outline.setStrokeWidth(1.2f);
                outline.setColor(color);
                canvas.drawRoundRect(pairRect, 8f, 8f, outline);
            }

            canvas.drawText(parts[i], pairLeft, baseline, timePaint);

            if (idle) {
                hits.put(FIELD + i, new RectF(pairLeft - pad * 0.3f, centerY - digitH * 0.6f, pairRight + pad * 0.3f, centerY + digitH * 0.6f));
            }
            x = pairRight;
            if (i < 2) {
                timePaint.setColor(0x88FFFFFF);
                canvas.drawText(":", x + (colonW + pad * 0.6f - colonW) / 2, baseline - digitH * 0.04f, timePaint);
                timePaint.setColor(lit ? Color.WHITE : 0x44FFFFFF);
                x += colonW + pad * 0.6f;
            }
        }
    }

    /** The alarm time with AM/PM above a bell; ringing flashes the time and locks the controls. */
    private void drawAlarm(Canvas canvas, int color, float bx, float bw, float pad, float midTop, float midH,
            boolean ringing) {
        Context context = getContext();
        boolean twelveHour = !DateFormat.is24HourFormat(context);
        int hour = AlarmController.hour(context);
        int minute = AlarmController.minute(context);
        int shownHour = twelveHour ? (hour % 12 == 0 ? 12 : hour % 12) : hour;
        boolean lit = !ringing || System.currentTimeMillis() / 400 % 2 == 0;

        float digitH = midH * 0.74f;
        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        timePaint.setTextSize(digitH);
        timePaint.setColor(lit ? (ringing ? 0xFFFF3B30 : Color.WHITE) : 0x44FFFFFF);

        String hStr = String.format(java.util.Locale.ROOT, twelveHour && shownHour < 10 ? "%d" : "%02d", shownHour);
        String mStr = String.format(java.util.Locale.ROOT, "%02d", minute);
        float hW = timePaint.measureText(hStr);
        float colonW = timePaint.measureText(":");
        float mW = timePaint.measureText("00");

        // AM/PM badge & Bell toggle
        float badgeH = digitH * 0.38f;
        float toggleH = digitH * 0.42f;
        float badgeW = twelveHour ? digitH * 0.65f : 0;
        float toggleW = digitH * 0.75f;
        float rightColW = Math.max(badgeW, toggleW);

        float totalW = hW + colonW + mW + pad * 1.5f + rightColW;
        float left = bx + (bw - totalW) / 2;
        float centerY = midTop + midH / 2;
        Paint.FontMetrics fm = timePaint.getFontMetrics();
        float baseline = centerY - (fm.ascent + fm.descent) / 2;

        // Hour
        RectF hourRect = new RectF(left - pad * 0.2f, centerY - digitH * 0.55f, left + hW + pad * 0.2f, centerY + digitH * 0.55f);
        if (!ringing && alarmField == 0) {
            fill.setColor(0x33000000 | (color & 0x00FFFFFF));
            canvas.drawRoundRect(hourRect, 8f, 8f, fill);
            outline.setStrokeWidth(1.2f);
            outline.setColor(color);
            canvas.drawRoundRect(hourRect, 8f, 8f, outline);
        }
        canvas.drawText(hStr, left, baseline, timePaint);
        if (!ringing) {
            hits.put(FIELD, hourRect);
        }

        // Colon
        float colonX = left + hW + pad * 0.2f;
        timePaint.setColor(0x88FFFFFF);
        canvas.drawText(":", colonX, baseline - digitH * 0.04f, timePaint);
        timePaint.setColor(lit ? Color.WHITE : 0x44FFFFFF);

        // Minute
        float minX = colonX + colonW + pad * 0.2f;
        RectF minRect = new RectF(minX - pad * 0.2f, centerY - digitH * 0.55f, minX + mW + pad * 0.2f, centerY + digitH * 0.55f);
        if (!ringing && alarmField == 1) {
            fill.setColor(0x33000000 | (color & 0x00FFFFFF));
            canvas.drawRoundRect(minRect, 8f, 8f, fill);
            outline.setStrokeWidth(1.2f);
            outline.setColor(color);
            canvas.drawRoundRect(minRect, 8f, 8f, outline);
        }
        canvas.drawText(mStr, minX, baseline, timePaint);
        if (!ringing) {
            hits.put(FIELD + 1, minRect);
        }

        // Right column: AM/PM + Bell Toggle
        float colX = minX + mW + pad * 1.2f;
        if (twelveHour) {
            Paint ampmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ampmPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
            ampmPaint.setTextSize(badgeH * 0.85f);
            ampmPaint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
            String ampm = hour < 12 ? "AM" : "PM";
            canvas.drawText(ampm, colX, centerY - digitH * 0.12f, ampmPaint);
        }

        // Modern Bell Toggle Button
        boolean on = AlarmController.isOn(context);
        boolean bellLit = AlarmController.bellLit(context);
        RectF bellRect = new RectF(colX, centerY + (twelveHour ? 0f : -toggleH / 2), colX + toggleW, centerY + (twelveHour ? toggleH : toggleH / 2));
        fill.setColor(on ? (0x33000000 | (color & 0x00FFFFFF)) : 0x2218181A);
        canvas.drawRoundRect(bellRect, 6f, 6f, fill);
        outline.setStrokeWidth(1.0f);
        outline.setColor(on ? color : DesignSystem.COLOR_BORDER);
        canvas.drawRoundRect(bellRect, 6f, 6f, outline);

        seg.bell(canvas, bellRect.centerX(), bellRect.centerY() + toggleH * 0.28f, toggleH * 0.65f, on && bellLit, on ? color : DesignSystem.COLOR_TEXT_TERTIARY);

        if (!ringing) {
            hits.put(BELL, bellRect);
        }
    }

    /** A row of modern elevated tactile pill buttons. */
    private void buttons(Canvas canvas, int color, float bx, float bw, float pad, float top, float h, int[] ids) {
        float weightSum = 0;
        for (int id : ids) {
            weightSum += weight(id);
        }
        float x = bx + pad;
        float unit = (bw - 2 * pad - pad * (ids.length - 1)) / weightSum;

        Paint btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnTextPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        btnTextPaint.setTextSize(h * 0.44f);

        for (int id : ids) {
            RectF r = new RectF(x, top, x + unit * weight(id), top + h);
            boolean primary = id == START || id == GO || id == SET || id == STOP;
            float radius = h * 0.36f;

            int btnBg;
            int btnBorder;
            int textColor;

            if (id == STOP || id == SNOOZE) {
                btnBg = pressed == id ? 0x66FF3B30 : 0x33FF3B30;
                btnBorder = 0xFFFF3B30;
                textColor = Color.WHITE;
            } else if (primary) {
                btnBg = pressed == id ? 0x66000000 | (color & 0x00FFFFFF) : 0x2E000000 | (color & 0x00FFFFFF);
                btnBorder = color;
                textColor = Color.WHITE;
            } else {
                btnBg = pressed == id ? 0x443A3A40 : DesignSystem.COLOR_CARD;
                btnBorder = DesignSystem.COLOR_BORDER;
                textColor = DesignSystem.COLOR_TEXT_PRIMARY;
            }

            fill.setColor(btnBg);
            canvas.drawRoundRect(r, radius, radius, fill);
            outline.setStrokeWidth(1.0f);
            outline.setColor(btnBorder);
            canvas.drawRoundRect(r, radius, radius, outline);

            String text = id == SET && alarmMessage != null && System.currentTimeMillis() < alarmMessageUntil
                    ? alarmMessage : buttonText(id);

            if (id == MINUS || id == PLUS) {
                float half = h * 0.20f;
                outline.setStrokeWidth(2.0f);
                outline.setColor(Color.WHITE);
                outline.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(r.centerX() - half, r.centerY(), r.centerX() + half, r.centerY(), outline);
                if (id == PLUS) {
                    canvas.drawLine(r.centerX(), r.centerY() - half, r.centerX(), r.centerY() + half, outline);
                }
                outline.setStrokeCap(Paint.Cap.BUTT);
            } else {
                btnTextPaint.setColor(textColor);
                Paint.FontMetrics fm = btnTextPaint.getFontMetrics();
                float textY = r.centerY() - (fm.ascent + fm.descent) / 2;
                float textW = btnTextPaint.measureText(text);
                canvas.drawText(text, r.centerX() - textW / 2, textY, btnTextPaint);
            }

            hits.put(id, r);
            x = r.right + pad;
        }
    }

    private static float weight(int id) {
        return id == MINUS || id == PLUS ? 1 : 2;
    }

    private static String buttonText(int id) {
        switch (id) {
            case MINUS: return "-";
            case PLUS: return "+";
            case START: return "START";
            case PAUSE: return "PAUSE";
            case GO: return "GO";
            case RESET: return "RESET";
            case STOP: return "STOP";
            case SET: return "SET";
            case SNOOZE: return "SNOOZE";
            default: return "";
        }
    }

    // ---- touch ----

    private int hit(float x, float y) {
        for (Map.Entry<Integer, RectF> e : hits.entrySet()) {
            if (e.getValue().contains(x, y)) {
                return e.getKey();
            }
        }
        return NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = hit(x, y);
                if (pressed == MINUS || pressed == PLUS) {
                    // Steps on release (or once held), so a page swipe starting here changes nothing.
                    repeated = false;
                    handler.postDelayed(repeat, REPEAT_DELAY_MS);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressed != NONE && hit(x, y) != pressed) {
                    handler.removeCallbacks(repeat);
                    pressed = NONE;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(repeat);
                int control = pressed;
                pressed = NONE;
                if ((control == MINUS || control == PLUS) && !repeated && hit(x, y) == control) {
                    step(control == PLUS ? 1 : -1);
                } else if (control != NONE && control != MINUS && control != PLUS && hit(x, y) == control) {
                    perform(control);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(repeat);
                pressed = NONE;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void perform(int control) {
        Context context = getContext();
        Log.d(Binders.TAG, "menu: " + control + " alarm=" + AlarmController.state(context) + " at "
                + AlarmController.hour(context) + ":" + AlarmController.minute(context));
        switch (control) {
            case FIELD:
            case FIELD + 1:
            case FIELD + 2:
                if (alarmTab) {
                    alarmField = control - FIELD;
                } else {
                    timerField = control - FIELD;
                }
                break;
            case BELL:
                if (AlarmController.state(context) == AlarmController.State.SNOOZED) {
                    AlarmController.stop(context); // ends the snooze; still on for tomorrow
                } else if (AlarmController.isOn(context)) {
                    AlarmController.turnOff(context);
                } else {
                    AlarmController.turnOn(context);
                }
                break;
            case SNOOZE:
                context.startService(new Intent(context, RingService.class).setAction(RingService.ACTION_SNOOZE));
                break;
            case START:
            case GO:
                TimerController.start(context);
                break;
            case PAUSE:
                TimerController.pause(context);
                break;
            case RESET:
                TimerController.reset(context);
                break;
            case STOP:
                if (alarmTab) {
                    context.startService(new Intent(context, RingService.class).setAction(RingService.ACTION_STOP)
                            .putExtra(RingService.EXTRA_KIND, RingService.KIND_ALARM));
                } else {
                    TimerController.stopRinging(context);
                }
                break;
            case SET:
                AlarmController.turnOn(context);
                alarmMessage = "DONE";
                alarmMessageUntil = System.currentTimeMillis() + 2500;
                break;
            default:
                break;
        }
    }

    /** − / + on the selected field; values wrap within the field. */
    private void step(int delta) {
        Context context = getContext();
        if (alarmTab) {
            int hour = AlarmController.hour(context);
            int minute = AlarmController.minute(context);
            if (alarmField == 0) {
                hour = Math.floorMod(hour + delta, 24);
            } else {
                minute = Math.floorMod(minute + delta, 60);
            }
            AlarmController.setTime(context, hour, minute);
        } else if (TimerController.state(context) == TimerController.State.IDLE) {
            long secs = TimerController.duration(context) / 1000;
            int h = (int) (secs / 3600);
            int m = (int) (secs / 60 % 60);
            int s = (int) (secs % 60);
            if (timerField == 0) {
                h = Math.floorMod(h + delta, 24);
            } else if (timerField == 1) {
                m = Math.floorMod(m + delta, 60);
            } else {
                s = Math.floorMod(s + delta, 60);
            }
            TimerController.setDuration(context, (h * 3600L + m * 60L + s) * 1000);
        }
        invalidate();
    }

}
