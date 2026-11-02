package dev.lcdclock;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * The cover screen's RECORDER page, like a pocket recorder's LCD: the time in segment digits
 * (recording, playing, or the last recording's length) with a blinking REC or PLAY beside it, a
 * level meter of segments, and REC/STOP, play/pause for the last recording and LIST. LIST shows
 * every recording in Recordings, newest first, two to the screen with its date and length; tap one
 * to play or pause it, BACK to return. Recording works while the phone is locked, but playing and
 * the list need it unlocked: their buttons carry a padlock, and a tap asks for the fingerprint
 * first. RecorderService (LCD Clock's own, no other app needed) does the recording; the app's
 * Recordings screen renames, shares and deletes them.
 */
final class RecorderView extends CoverPage {
    private static final int RECORD = 1;
    private static final int PLAY = 2;
    private static final int LIST = 3;
    private static final int BACK = 4;
    /** ROW + index: a recording in the list. */
    private static final int ROW = 100;
    private static final int METER_BARS = 24;
    private static final int RED = 0xFFFF2020;
    /** Quietest level the meter shows, in dB below full scale. */
    private static final float METER_FLOOR_DB = 50f;

    private MediaPlayer player;
    /** What the player has loaded. */
    private Uri playingUri;
    private boolean listing;
    private List<Recordings.Item> recordings = Collections.emptyList();
    /** Smoothed meter level, 0..1. */
    private float level;
    private static final long REFUSED_MS = 700;
    /** The locked button tapped last, and when its padlock stops being lit. */
    private int refused;
    private long refusedUntil;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            float now = RecorderService.isRecording() ? toLevel(RecorderService.amplitude()) : 0;
            level = Math.max(now, level * 0.75f);
            invalidate();
            postDelayed(this, RecorderService.isRecording() || player != null ? 80 : 500);
        }
    };

    RecorderView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        forgetDeletedLast();
        post(tick);
    }

    /** The last recording, if it has since been deleted (in the app, Files or another app), is forgotten. */
    private void forgetDeletedLast() {
        Uri last = lastRecording();
        if (last == null) {
            return;
        }
        try (Cursor c = getContext().getContentResolver().query(last, new String[] {MediaStore.Audio.Media._ID},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return;
            }
        } catch (RuntimeException e) {
            // unreadable counts as gone
        }
        Prefs.get(getContext()).edit().remove(Prefs.LAST_RECORDING).remove(Prefs.LAST_RECORDING_MS).apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        stopPlaying();
        super.onDetachedFromWindow();
    }

    private static float toLevel(int amplitude) {
        if (amplitude <= 0) {
            return 0;
        }
        float db = 20f * (float) Math.log10(amplitude / 32767f);
        return Math.max(0, Math.min(1, 1 + db / METER_FLOOR_DB));
    }

    @Override
    void drawPage(Canvas canvas, int color) {
        Context context = getContext();
        boolean locked = Lock.locked(context);
        if (listing && locked) {
            listing = false; // the recordings are for an unlocked phone only
            stopPlaying();
        }
        if (listing) {
            drawList(canvas, color);
            return;
        }
        boolean allowed = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean recording = RecorderService.isRecording();
        Uri last = lastRecording();
        boolean playing = player != null && player.isPlaying();

        float left = bx + pad * 1.2f;
        float right = bx + bw - pad * 1.2f;
        float btnTop = buttonTop();
        float h = btnTop - pad * 0.6f - top;

        // Time: recording, playing, else the last recording's length.
        long ms = recording ? RecorderService.elapsed()
                : player != null ? player.getCurrentPosition() : Prefs.get(context).getLong(Prefs.LAST_RECORDING_MS, 0);
        long secs = ms / 1000;
        boolean hours = secs >= 3600;
        String timeStr = hours
                ? String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", secs / 3600, secs / 60 % 60, secs % 60)
                : String.format(java.util.Locale.ROOT, "%02d:%02d", secs / 60, secs % 60);

        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        float digitH = h * 0.44f;
        timePaint.setTextSize(digitH);
        timePaint.setColor(recording ? 0xFFFF453A : Color.WHITE);
        Paint.FontMetrics fm = timePaint.getFontMetrics();
        float timeY = top + digitH * 0.9f;
        canvas.drawText(timeStr, left, timeY, timePaint);

        // Modern status capsule pill next to digits
        float timeW = timePaint.measureText(timeStr);
        float pillH = digitH * 0.58f;
        float pillY = timeY - digitH * 0.72f;
        float pillX = left + timeW + pad * 1.0f;
        String tag = recording ? "REC" : (playing ? "PLAYING" : "READY");
        int tagColor = recording ? 0xFFFF3B30 : (playing ? color : 0xFF8E8E93);
        int tagBg = recording ? 0x33FF3B30 : (playing ? (0x28000000 | (color & 0x00FFFFFF)) : 0x221C1C1E);

        Paint tagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tagPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        tagPaint.setTextSize(pillH * 0.56f);
        float tagTextW = tagPaint.measureText(tag);
        float pillW = tagTextW + (recording ? pillH * 0.7f : 0f) + pillH * 0.8f;
        RectF tagRect = new RectF(pillX, pillY, pillX + pillW, pillY + pillH);

        fill.setColor(tagBg);
        canvas.drawRoundRect(tagRect, pillH * 0.5f, pillH * 0.5f, fill);
        outline.setStrokeWidth(1.0f);
        outline.setColor(tagColor);
        canvas.drawRoundRect(tagRect, pillH * 0.5f, pillH * 0.5f, outline);

        if (recording) {
            boolean dotOn = System.currentTimeMillis() / 400 % 2 == 0;
            fill.setColor(dotOn ? 0xFFFF3B30 : 0x44FF3B30);
            canvas.drawCircle(tagRect.left + pillH * 0.45f, tagRect.centerY(), pillH * 0.18f, fill);
        }

        tagPaint.setColor(Color.WHITE);
        Paint.FontMetrics tfm = tagPaint.getFontMetrics();
        float textBaseline = tagRect.centerY() - (tfm.ascent + tfm.descent) / 2;
        float textX = tagRect.left + (recording ? pillH * 0.75f : pillH * 0.4f);
        canvas.drawText(tag, textX, textBaseline, tagPaint);

        // Modern 24-bar dynamic audio spectrum visualizer
        float meterTop = top + digitH + pad * 0.5f;
        float meterH = h * 0.22f;
        float totalMeterW = right - left;
        float slot = totalMeterW / METER_BARS;
        float barW = Math.max(2.5f, slot * 0.65f);
        int lit = Math.round(level * METER_BARS);

        for (int i = 0; i < METER_BARS; i++) {
            float bx1 = left + i * slot + (slot - barW) / 2;
            int barColor = i >= METER_BARS - 3 ? 0xFFFF3B30 : (i >= METER_BARS - 7 ? 0xFFFF9F0A : color);
            float barHeight = i < lit ? meterH : Math.max(2.5f, meterH * 0.2f);
            float curTop = meterTop + (meterH - barHeight);
            RectF barRect = new RectF(bx1, curTop, bx1 + barW, meterTop + meterH);

            fill.setColor(i < lit ? barColor : 0x22FFFFFF);
            canvas.drawRoundRect(barRect, barW / 2, barW / 2, fill);
        }

        // Subtitle status line
        String note = !allowed ? context.getString(R.string.recorder_needs_mic)
                : recording ? context.getString(R.string.recorder_recording)
                : last != null ? context.getString(R.string.recorder_last)
                : context.getString(R.string.recorder_empty);
        line(canvas, note, left, top + h, h * 0.16f, right - left, DesignSystem.COLOR_TEXT_SECONDARY);

        // Modern Bottom Tactile Deck Buttons
        float bh2 = buttonHeight();
        float gap = pad * 0.8f;
        float unit = (bw - 2 * pad * 1.2f - 2 * gap) / 4.4f;

        RectF rec = button(canvas, RECORD, new RectF(left, btnTop, left + unit * 2, btnTop + bh2), true,
                allowed, recording ? 0xFFFF3B30 : color);
        RectF play = button(canvas, PLAY, new RectF(rec.right + gap, btnTop, rec.right + gap + unit * 1.2f,
                btnTop + bh2), false, allowed && !recording && last != null, color);
        RectF list = button(canvas, LIST, new RectF(play.right + gap, btnTop, right, btnTop + bh2), false,
                !recording, color);

        drawRecordIcon(canvas, rec, recording, allowed ? (recording ? 0xFFFF3B30 : Color.WHITE) : GREY);
        drawPlayIcon(canvas, play, playing && last != null && last.equals(playingUri),
                allowed && !recording && last != null ? Color.WHITE : GREY);
        drawListIcon(canvas, list, recording ? GREY : Color.WHITE);

        if (locked) {
            long now = System.currentTimeMillis();
            if (allowed && !recording && last != null) {
                lockBadge(canvas, play, PLAY, now, color);
            }
            if (!recording) {
                lockBadge(canvas, list, LIST, now, color);
            }
        }
    }

    private void lockBadge(Canvas canvas, RectF r, int id, long now, int color) {
        float s = r.height() * 0.3f;
        float inset = r.height() * 0.13f;
        Lock.drawBadge(canvas, r.right - inset - Lock.badgeWidth(s), r.top + inset, s,
                id == refused && now < refusedUntil ? color : Lock.BADGE);
    }

    /** Every recording, two to the screen: modern elevated cards with play knob, date/name and length. */
    private void drawList(Canvas canvas, int color) {
        Context context = getContext();
        float left = bx + pad * 1.2f;
        float right = bx + bw - pad * 1.2f;
        float bottom = by + bh - pad;
        float headH = bh * 0.08f;
        float backY = top + pad * 0.3f;
        float backH = bh * 0.14f;
        float listTop = backY + backH + pad * 0.5f;
        float backRight = backButton(canvas, BACK, backY, backH, top, listTop, color);
        label(canvas, "RECORDINGS", backRight + pad * 1.2f, backY + backH / 2 + headH * 0.35f, headH, Color.WHITE);

        if (recordings.isEmpty()) {
            line(canvas, context.getString(R.string.recordings_none_cover), left, listTop + bh * 0.14f, bh * 0.085f,
                    right - left, GREY);
            return;
        }

        float rowH = (bottom - listTop) / 2;
        maxScroll = Math.max(0, recordings.size() * rowH - (bottom - listTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        boolean scrolling = false;
        canvas.save();
        canvas.clipRect(bx, listTop, bx + bw, bottom);
        for (int i = 0; i < recordings.size(); i++) {
            float rowTop = listTop + i * rowH - scroll;
            if (rowTop > bottom || rowTop + rowH < listTop) {
                continue;
            }
            Recordings.Item item = recordings.get(i);
            boolean loaded = item.uri.equals(playingUri) && player != null;
            boolean playing = loaded && player.isPlaying();

            // Modern squircle card container for each recording item
            RectF itemRect = new RectF(left, rowTop + 2.5f, right, rowTop + rowH - 2.5f);
            fill.setColor(loaded ? (0x2A000000 | (color & 0x00FFFFFF)) : DesignSystem.COLOR_CARD);
            canvas.drawRoundRect(itemRect, 8f, 8f, fill);
            outline.setStrokeWidth(1.0f);
            outline.setColor(loaded ? color : DesignSystem.COLOR_BORDER);
            canvas.drawRoundRect(itemRect, 8f, 8f, outline);

            float s = rowH * 0.55f;
            RectF knob = new RectF(left + pad * 0.6f, rowTop + (rowH - s) / 2, left + pad * 0.6f + s, rowTop + (rowH + s) / 2);
            button(canvas, ROW + i, knob, loaded, true, color);
            drawPlayIcon(canvas, knob, playing, loaded ? color : Color.WHITE);
            hits.put(ROW + i, itemRect);

            float textX = knob.right + pad * 0.8f;
            scrolling |= marquee(canvas, Recordings.title(context, item), textX, rowTop + rowH * 0.44f, rowH * 0.32f,
                    right - pad * 0.8f - textX, loaded ? color : Color.WHITE);
            String length = loaded ? Recordings.length(player.getCurrentPosition()) + " / " + Recordings.length(item.length)
                    : Recordings.length(item.length);
            line(canvas, length, textX, rowTop + rowH * 0.80f, rowH * 0.25f, right - pad * 0.8f - textX, DesignSystem.COLOR_TEXT_SECONDARY);
        }
        canvas.restore();
        scrollBar(canvas, listTop, bottom, recordings.size() * rowH, color);
        if (scrolling) {
            postInvalidateOnAnimation();
        }
    }

    /** Three lines with dots: the list. */
    private void drawListIcon(Canvas canvas, RectF r, int ink) {
        float s = r.height() * 0.36f;
        float cx = r.centerX();
        float cy = r.centerY();
        fill.setColor(ink);
        for (int i = -1; i <= 1; i++) {
            float y = cy + i * s * 0.4f;
            canvas.drawCircle(cx - s * 0.55f, y, s * 0.09f, fill);
            canvas.drawRect(cx - s * 0.35f, y - s * 0.07f, cx + s * 0.6f, y + s * 0.07f, fill);
        }
    }

    private void drawRecordIcon(Canvas canvas, RectF r, boolean recording, int ink) {
        float s = r.height() * 0.34f;
        fill.setColor(ink);
        if (recording) {
            canvas.drawRect(r.centerX() - s / 2, r.centerY() - s / 2, r.centerX() + s / 2, r.centerY() + s / 2, fill);
        } else {
            canvas.drawCircle(r.centerX(), r.centerY(), s * 0.55f, fill);
        }
    }

    private void drawPlayIcon(Canvas canvas, RectF r, boolean playing, int ink) {
        float s = r.height() * 0.36f;
        float cx = r.centerX();
        float cy = r.centerY();
        fill.setColor(ink);
        if (playing) {
            float bar = s * 0.3f;
            float space = s * 0.24f;
            canvas.drawRect(cx - space / 2 - bar, cy - s / 2, cx - space / 2, cy + s / 2, fill);
            canvas.drawRect(cx + space / 2, cy - s / 2, cx + space / 2 + bar, cy + s / 2, fill);
        } else {
            path.reset();
            path.moveTo(cx - s * 0.4f, cy - s / 2);
            path.lineTo(cx + s * 0.5f, cy);
            path.lineTo(cx - s * 0.4f, cy + s / 2);
            path.close();
            canvas.drawPath(path, fill);
        }
    }

    /** What PLAY plays: LCD Clock's last recording. */
    private Uri lastRecording() {
        String uri = Prefs.get(getContext()).getString(Prefs.LAST_RECORDING, null);
        return uri == null ? null : Uri.parse(uri);
    }

    @Override
    boolean perform(int id) {
        Context context = getContext();
        if ((id == PLAY || id == LIST) && Lock.locked(context)) {
            refused = id;
            refusedUntil = System.currentTimeMillis() + REFUSED_MS;
            Lock.refuse(context);
            invalidate();
            Lock.requestUnlock(context, () -> perform(id));
            return false;
        }
        if (id == RECORD) {
            if (RecorderService.isRecording()) {
                RecorderService.stop(context);
            } else {
                stopPlaying();
                RecorderService.start(context);
            }
            return true;
        }
        if (id == PLAY) {
            toggle(lastRecording());
            return true;
        }
        if (id == LIST) {
            recordings = Recordings.list(context);
            listing = true;
            scroll = 0;
            return true;
        }
        if (id == BACK) {
            listing = false;
            maxScroll = 0;
            return true;
        }
        int row = id - ROW;
        if (row >= 0 && row < recordings.size()) {
            toggle(recordings.get(row).uri);
            return true;
        }
        return false;
    }

    /** Plays `uri`, or pauses or resumes it if it's the one loaded. */
    private void toggle(Uri uri) {
        if (uri == null) {
            return;
        }
        if (player != null && uri.equals(playingUri)) {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.start();
            }
            return;
        }
        stopPlaying();
        MediaPlayer p = new MediaPlayer();
        try {
            p.setDataSource(getContext(), uri);
            p.prepare();
            p.setOnCompletionListener(done -> stopPlaying());
            p.start();
            player = p;
            playingUri = uri;
        } catch (IOException | RuntimeException e) {
            Log.w(Binders.TAG, "can't play " + uri, e);
            p.release();
        }
    }

    private void stopPlaying() {
        if (player != null) {
            player.release();
            player = null;
        }
        playingUri = null;
    }
}
