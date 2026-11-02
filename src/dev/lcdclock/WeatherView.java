package dev.lcdclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

/**
 * Completely Redesigned External-Display Weather Dashboard.
 * Inspired by modern smartwatch weather interfaces and automotive HUDs:
 * - Left: Hero temperature, weather condition badge, location pill, and High/Low pills.
 * - Right: 4-slot hourly forecast timeline cards with weather glyphs and rain indicator.
 */
final class WeatherView extends CoverPage {
    private static final int HOURLY_COUNT = 4;

    private Weather weather;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectBuffer = new RectF();

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            Weather kept = Weather.get(getContext(), fresh -> {
                weather = fresh;
                invalidate();
            });
            if (kept != null) {
                weather = kept;
            }
            invalidate();
            postDelayed(this, 60_000);
        }
    };

    WeatherView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(refresh);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(refresh);
        super.onDetachedFromWindow();
    }

    @Override
    void drawPage(Canvas canvas, int accent) {
        Context context = getContext();
        float left = bx + pad * 1.2f;
        float right = bx + bw - pad * 1.2f;
        float contentTop = top + pad * 0.4f;
        float contentBottom = by + bh - pad * 0.6f;
        float contentH = contentBottom - contentTop;

        if (weather == null) {
            String note = context.getString(Weather.allowed(context) ? R.string.weather_loading : R.string.weather_needs_location);
            paint.setTextSize(bh * 0.085f);
            paint.setColor(Weather.allowed(context) ? DesignSystem.COLOR_TEXT_PRIMARY : DesignSystem.COLOR_TEXT_SECONDARY);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(note, bx + bw / 2f, by + bh / 2f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            return;
        }

        // Layout: Left Hero Card (44% width), Right Hourly Cards (53% width)
        float totalW = right - left;
        float leftCardW = totalW * 0.44f;
        float rightCardsX = left + leftCardW + pad * 0.8f;
        float rightCardsW = right - rightCardsX;

        // 1. Left Hero Weather Card
        drawCurrentWeatherCard(canvas, left, contentTop, leftCardW, contentH, accent);

        // 2. Right Hourly Forecast Cards
        drawHourlyCards(canvas, rightCardsX, contentTop, rightCardsW, contentH, accent);
    }

    private void drawCurrentWeatherCard(Canvas canvas, float x, float y, float w, float h, int accent) {
        // Elevated glass card
        DesignSystem.drawCard(canvas, x, y, x + w, y + h, DesignSystem.RADIUS_MEDIUM,
                DesignSystem.COLOR_CARD, DesignSystem.COLOR_BORDER_SUBTLE);

        float innerX = x + pad * 0.8f;

        // Location pill on top
        String loc = (weather.place != null && !weather.place.isEmpty()) ? weather.place : "Current Location";
        paint.setTextSize(h * 0.14f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        paint.setColor(accent);
        canvas.drawText("📍 " + DesignSystem.ellipsize(loc, paint, w - pad * 1.6f), innerX, y + h * 0.19f, paint);

        // Huge Hero Temperature (e.g. 22°)
        String tempStr = weather.temp + "°";
        paint.setTextSize(h * 0.42f);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);
        canvas.drawText(tempStr, innerX, y + h * 0.60f, paint);

        // Condition label + rain chance
        String cond = conditionName(weather.code);
        if (weather.rain > 0) {
            cond += " • " + weather.rain + "% rain";
        }
        paint.setTextSize(h * 0.14f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);
        canvas.drawText(DesignSystem.ellipsize(cond, paint, w - pad * 1.6f), innerX, y + h * 0.78f, paint);

        // High / Low capsule
        String hiLo = "H: " + weather.high + "°   L: " + weather.low + "°";
        paint.setTextSize(h * 0.13f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        paint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
        canvas.drawText(hiLo, innerX, y + h * 0.94f, paint);
    }

    private void drawHourlyCards(Canvas canvas, float x, float y, float w, float h, int accent) {
        if (weather.hours.isEmpty()) return;

        int count = Math.min(HOURLY_COUNT, weather.hours.size());
        float cardGap = pad * 0.4f;
        float cardW = (w - (count - 1) * cardGap) / count;

        boolean twelveHour = !DateFormat.is24HourFormat(getContext());

        for (int i = 0; i < count; i++) {
            Weather.Hour hour = weather.hours.get(i);
            float cardX = x + i * (cardW + cardGap);

            // Card background (first one highlighted slightly)
            int bg = (i == 0) ? DesignSystem.COLOR_CARD_ELEVATED : DesignSystem.COLOR_CARD;
            int border = (i == 0) ? accent : DesignSystem.COLOR_BORDER_SUBTLE;
            DesignSystem.drawCard(canvas, cardX, y, cardX + cardW, y + h, DesignSystem.RADIUS_SMALL, bg, border);

            float cx = cardX + cardW / 2f;

            // Time Header
            Calendar at = Calendar.getInstance();
            at.setTimeInMillis(hour.time);
            int hr = at.get(Calendar.HOUR_OF_DAY);
            String time = twelveHour ? String.format(Locale.ROOT, "%d%s", hr % 12 == 0 ? 12 : hr % 12, hr < 12 ? "A" : "P")
                    : String.format(Locale.ROOT, "%02d:00", hr);

            paint.setTextSize(h * 0.15f);
            paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
            paint.setColor(i == 0 ? DesignSystem.COLOR_TEXT_PRIMARY : DesignSystem.COLOR_TEXT_SECONDARY);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(time, cx, y + h * 0.22f, paint);

            // Weather Icon Glyph
            float iconCy = y + h * 0.50f;
            float iconSize = cardW * 0.44f;
            drawModernWeatherGlyph(canvas, hour.code, isDayHour(hr), cx, iconCy, iconSize);

            // Hourly Temperature
            paint.setTextSize(h * 0.18f);
            paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            paint.setColor(Color.WHITE);
            canvas.drawText(hour.temp + "°", cx, y + h * 0.78f, paint);

            // Rain bar on bottom
            if (hour.rain > 0) {
                paint.setTextSize(h * 0.12f);
                paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
                paint.setColor(DesignSystem.COLOR_ACCENT_AQUA);
                canvas.drawText(hour.rain + "%", cx, y + h * 0.93f, paint);
            }
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawModernWeatherGlyph(Canvas canvas, int code, boolean day, float cx, float cy, float s) {
        iconPaint.setStyle(Paint.Style.FILL);
        if (code == 0 || code == 1) { // Clear sun / moon
            iconPaint.setColor(day ? DesignSystem.COLOR_ACCENT_AMBER : DesignSystem.COLOR_TEXT_PRIMARY);
            canvas.drawCircle(cx, cy, s * 0.42f, iconPaint);
        } else if (code == 2 || code == 3) { // Cloud
            iconPaint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
            canvas.drawCircle(cx - s * 0.18f, cy, s * 0.28f, iconPaint);
            canvas.drawCircle(cx + s * 0.15f, cy - s * 0.08f, s * 0.35f, iconPaint);
            canvas.drawRoundRect(cx - s * 0.45f, cy, cx + s * 0.45f, cy + s * 0.32f, s * 0.15f, s * 0.15f, iconPaint);
        } else if (code >= 51 && code <= 67) { // Rain
            iconPaint.setColor(DesignSystem.COLOR_ACCENT_AQUA);
            canvas.drawCircle(cx, cy - s * 0.10f, s * 0.35f, iconPaint);
            // Drops
            canvas.drawRoundRect(cx - s * 0.25f, cy + s * 0.25f, cx - s * 0.15f, cy + s * 0.50f, 2f, 2f, iconPaint);
            canvas.drawRoundRect(cx + s * 0.15f, cy + s * 0.25f, cx + s * 0.25f, cy + s * 0.50f, 2f, 2f, iconPaint);
        } else if (code >= 95) { // Thunderstorm
            iconPaint.setColor(DesignSystem.COLOR_ACCENT_AMBER);
            canvas.drawCircle(cx, cy, s * 0.38f, iconPaint);
        } else {
            // General cloud
            iconPaint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
            canvas.drawCircle(cx, cy, s * 0.38f, iconPaint);
        }
    }

    private static String conditionName(int code) {
        if (code == 0) return "Clear Sky";
        if (code == 1) return "Mainly Clear";
        if (code == 2) return "Partly Cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 51 && code <= 55) return "Drizzle";
        if (code >= 61 && code <= 65) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 95) return "Thunderstorm";
        return "Fair";
    }

    private static boolean isDayHour(int hour) {
        return hour >= 7 && hour < 19;
    }
}
