package dev.lcdclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.view.View;

import java.util.List;

/**
 * Dedicated AI Voice Assistant page designed specifically for the 368 x 194 external display.
 * Provides tap-to-talk speech input, real-time waveform animation, live transcription,
 * scrollable AI response presentation, and spoken playback controls.
 */
public final class AssistantView extends CoverPage {
    private static final int HIT_MIC = 1;
    private static final int HIT_STOP_SPEAKING = 2;
    private static final int HIT_RESET = 3;

    private final VoiceSessionManager session;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF micBounds = new RectF();

    private VoiceSessionManager.State state = VoiceSessionManager.State.IDLE;
    private String statusText = "Tap mic to speak";
    private String transcription = "";
    private String responseText = "";
    private float currentRms = 0f;
    private long animStart = SystemClock.uptimeMillis();

    // Redraw loop for animated waveform & pulsing mic
    private final Runnable waveAnim = new Runnable() {
        @Override
        public void run() {
            if (state == VoiceSessionManager.State.LISTENING || state == VoiceSessionManager.State.TRANSCRIBING
                    || state == VoiceSessionManager.State.PROCESSING) {
                invalidate();
                postDelayed(this, 50);
            }
        }
    };

    public AssistantView(Context context) {
        super(context);
        AIProvider provider = new GeminiProvider(context);
        this.session = new VoiceSessionManager(context, provider);

        this.session.setListener(new VoiceSessionManager.Listener() {
            @Override
            public void onStateChanged(VoiceSessionManager.State newState, String message) {
                state = newState;
                statusText = message;
                if (state == VoiceSessionManager.State.LISTENING) {
                    animStart = SystemClock.uptimeMillis();
                    removeCallbacks(waveAnim);
                    post(waveAnim);
                } else if (state == VoiceSessionManager.State.PROCESSING) {
                    removeCallbacks(waveAnim);
                    post(waveAnim);
                }
                invalidate();
            }

            @Override
            public void onRmsChanged(float rmsDb) {
                currentRms = Math.max(0f, rmsDb);
                invalidate();
            }

            @Override
            public void onTranscriptionUpdated(String partialText) {
                transcription = partialText;
                invalidate();
            }

            @Override
            public void onResponseReceived(String replyText) {
                responseText = replyText;
                invalidate();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        session.release();
        removeCallbacks(waveAnim);
        super.onDetachedFromWindow();
    }

    @Override
    void drawPage(Canvas canvas, int color) {
        // Content area is between top (under tabs) and by + bh
        float contentTop = top + pad * 0.5f;
        float contentBottom = by + bh - pad * 0.5f;
        float contentH = contentBottom - contentTop;

        hits.clear();

        // Top Status Header (State & Provider)
        drawHeader(canvas, bx + pad, contentTop, bw - pad * 2f);

        // Center / Main Content Area depends on current state
        if (state == VoiceSessionManager.State.IDLE) {
            drawIdleState(canvas, bx + pad, contentTop + pad * 1.8f, bw - pad * 2f, contentH - pad * 2f);
        } else if (state == VoiceSessionManager.State.LISTENING || state == VoiceSessionManager.State.TRANSCRIBING) {
            drawListeningState(canvas, bx + pad, contentTop + pad * 1.8f, bw - pad * 2f, contentH - pad * 2f);
        } else if (state == VoiceSessionManager.State.PROCESSING) {
            drawProcessingState(canvas, bx + pad, contentTop + pad * 1.8f, bw - pad * 2f, contentH - pad * 2f);
        } else if (state == VoiceSessionManager.State.SPEAKING || !responseText.isEmpty()) {
            drawSpeakingOrResponseState(canvas, bx + pad, contentTop + pad * 1.8f, bw - pad * 2f, contentH - pad * 2f);
        } else if (state == VoiceSessionManager.State.ERROR || state == VoiceSessionManager.State.PERMISSION_REQUIRED) {
            drawErrorState(canvas, bx + pad, contentTop + pad * 1.8f, bw - pad * 2f, contentH - pad * 2f);
        }
    }

    private void drawHeader(Canvas canvas, float x, float y, float w) {
        paint.setTextSize(bh * 0.08f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        paint.setColor(DesignSystem.COLOR_ACCENT_PURPLE);
        canvas.drawText("GEMINI ASSISTANT", x, y + bh * 0.08f, paint);

        // Status text on right
        paint.setTextSize(bh * 0.07f);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setColor(state == VoiceSessionManager.State.ERROR ? DesignSystem.COLOR_ACCENT_RED : DesignSystem.COLOR_TEXT_SECONDARY);
        String st = DesignSystem.ellipsize(statusText, paint, w * 0.55f);
        float sw = paint.measureText(st);
        canvas.drawText(st, x + w - sw, y + bh * 0.08f, paint);
    }

    private void drawIdleState(Canvas canvas, float x, float y, float w, float h) {
        // Large central Microphone button
        float micSize = h * 0.60f;
        float cx = x + w / 2f;
        float cy = y + h * 0.45f;
        micBounds.set(cx - micSize / 2f, cy - micSize / 2f, cx + micSize / 2f, cy + micSize / 2f);
        hits.put(HIT_MIC, new RectF(micBounds));

        // Circular card background
        int fill = pressed == HIT_MIC ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_CARD_ELEVATED;
        DesignSystem.drawCard(canvas, micBounds.left, micBounds.top, micBounds.right, micBounds.bottom,
                micSize / 2f, fill, DesignSystem.COLOR_ACCENT_PURPLE);

        // Mic icon
        paint.setTextSize(micSize * 0.45f);
        paint.setColor(DesignSystem.COLOR_ACCENT_PURPLE);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🎙️", cx, cy + micSize * 0.16f, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Hint subtitle
        paint.setTextSize(bh * 0.075f);
        paint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Tap to ask a question", cx, y + h * 0.90f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawListeningState(Canvas canvas, float x, float y, float w, float h) {
        // Pulsing circular mic on left
        float micSize = h * 0.50f;
        float cx = x + micSize * 0.7f;
        float cy = y + h * 0.45f;
        micBounds.set(cx - micSize / 2f, cy - micSize / 2f, cx + micSize / 2f, cy + micSize / 2f);
        hits.put(HIT_MIC, new RectF(micBounds));

        // Pulsing outer ripple
        long elapsed = SystemClock.uptimeMillis() - animStart;
        float pulse = (float) ((Math.sin(elapsed / 200.0) + 1.0) / 2.0);
        float rippleRadius = (micSize / 2f) + (pulse * 8f) + (currentRms * 1.5f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor((DesignSystem.COLOR_ACCENT_PURPLE & 0x00FFFFFF) | 0x66000000);
        canvas.drawCircle(cx, cy, rippleRadius, paint);

        // Mic circle
        DesignSystem.drawCard(canvas, micBounds.left, micBounds.top, micBounds.right, micBounds.bottom,
                micSize / 2f, DesignSystem.COLOR_ACCENT_PURPLE, 0);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(micSize * 0.40f);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("■", cx, cy + micSize * 0.15f, paint); // Stop symbol
        paint.setTextAlign(Paint.Align.LEFT);

        // Live transcription area on right
        float textX = cx + micSize * 0.7f;
        float textW = x + w - textX;

        paint.setTextSize(bh * 0.08f);
        paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));

        String displayTranscription = transcription.isEmpty() ? "Listening for your voice..." : transcription;
        String t1 = DesignSystem.ellipsize(displayTranscription, paint, textW);
        canvas.drawText(t1, textX, cy - bh * 0.05f, paint);

        // Audio waveform bars on right bottom
        drawWaveform(canvas, textX, cy + bh * 0.15f, textW * 0.8f, bh * 0.18f);
    }

    private void drawWaveform(Canvas canvas, float x, float y, float w, float h) {
        int bars = 10;
        float barW = w / (bars * 1.8f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DesignSystem.COLOR_ACCENT_PURPLE);

        long now = SystemClock.uptimeMillis();
        for (int i = 0; i < bars; i++) {
            float phase = (float) Math.sin((now / 150.0) + (i * 0.7));
            float barH = Math.max(4f, (h * 0.3f) + (phase * h * 0.3f) + (currentRms * 2.5f));
            float bx = x + i * (barW * 1.8f);
            float by = y + (h - barH) / 2f;
            canvas.drawRoundRect(bx, by, bx + barW, by + barH, barW / 2f, barW / 2f, paint);
        }
    }

    private void drawProcessingState(Canvas canvas, float x, float y, float w, float h) {
        float cx = x + w / 2f;
        float cy = y + h * 0.40f;

        paint.setTextSize(bh * 0.09f);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setColor(DesignSystem.COLOR_ACCENT_PURPLE);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Consulting Gemini...", cx, cy, paint);

        // Query text
        if (!transcription.isEmpty()) {
            paint.setTextSize(bh * 0.075f);
            paint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
            String q = "“" + DesignSystem.ellipsize(transcription, paint, w * 0.85f) + "”";
            canvas.drawText(q, cx, cy + bh * 0.18f, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSpeakingOrResponseState(Canvas canvas, float x, float y, float w, float h) {
        // AI Response Card (scrollable text)
        float cardH = h * 0.68f;
        DesignSystem.drawCard(canvas, x, y, x + w, y + cardH, DesignSystem.RADIUS_MEDIUM,
                DesignSystem.COLOR_CARD_ELEVATED, DesignSystem.COLOR_BORDER_SUBTLE);

        paint.setTextSize(bh * 0.078f);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);

        // Display multiple lines (wrap response)
        float lineY = y + pad * 1.2f;
        float lineMaxW = w - pad * 2f;
        String[] words = responseText.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        int linesDrawn = 0;
        for (String word : words) {
            String test = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (paint.measureText(test) < lineMaxW) {
                currentLine = new StringBuilder(test);
            } else {
                if (linesDrawn < 3) {
                    canvas.drawText(currentLine.toString(), x + pad, lineY, paint);
                    lineY += bh * 0.11f;
                    linesDrawn++;
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0 && linesDrawn < 3) {
            canvas.drawText(currentLine.toString(), x + pad, lineY, paint);
        }

        // Action Buttons at bottom
        float btnY = y + cardH + pad * 0.4f;
        float btnH = h - cardH - pad * 0.4f;
        float btnW = (w - pad) / 2f;

        // Button 1: Ask Another / Mic
        RectF micBtn = new RectF(x, btnY, x + btnW, btnY + btnH);
        hits.put(HIT_MIC, micBtn);
        DesignSystem.drawPill(canvas, micBtn.left, micBtn.top, micBtn.right, micBtn.bottom,
                pressed == HIT_MIC ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_ACCENT_PURPLE, 0);

        paint.setTextSize(bh * 0.075f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🎙️ Ask Another", micBtn.centerX(), micBtn.centerY() + bh * 0.025f, paint);

        // Button 2: Stop / End Session
        RectF stopBtn = new RectF(x + btnW + pad, btnY, x + w, btnY + btnH);
        hits.put(HIT_RESET, stopBtn);
        DesignSystem.drawPill(canvas, stopBtn.left, stopBtn.top, stopBtn.right, stopBtn.bottom,
                pressed == HIT_RESET ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_CARD,
                DesignSystem.COLOR_BORDER);

        paint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
        canvas.drawText("End Session", stopBtn.centerX(), stopBtn.centerY() + bh * 0.025f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawErrorState(Canvas canvas, float x, float y, float w, float h) {
        float cx = x + w / 2f;
        float cy = y + h * 0.35f;

        paint.setTextSize(bh * 0.14f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("⚠️", cx, cy, paint);

        paint.setTextSize(bh * 0.08f);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        paint.setColor(DesignSystem.COLOR_ACCENT_RED);
        canvas.drawText("Assistant Notice", cx, cy + bh * 0.16f, paint);

        paint.setTextSize(bh * 0.07f);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
        String err = DesignSystem.ellipsize(statusText, paint, w * 0.90f);
        canvas.drawText(err, cx, cy + bh * 0.28f, paint);

        // Retry / Settings pill button
        float btnW = w * 0.50f;
        float btnH = h * 0.28f;
        RectF retryRect = new RectF(cx - btnW / 2f, y + h * 0.70f, cx + btnW / 2f, y + h * 0.70f + btnH);
        hits.put(HIT_MIC, retryRect);
        DesignSystem.drawPill(canvas, retryRect.left, retryRect.top, retryRect.right, retryRect.bottom,
                pressed == HIT_MIC ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_CARD_ELEVATED,
                DesignSystem.COLOR_BORDER);

        paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);
        paint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        canvas.drawText("Try Again", cx, retryRect.centerY() + bh * 0.025f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected boolean perform(int control) {
        if (control == HIT_MIC) {
            if (state == VoiceSessionManager.State.LISTENING || state == VoiceSessionManager.State.TRANSCRIBING) {
                session.stopListening();
            } else {
                transcription = "";
                session.startListening();
            }
            return true;
        } else if (control == HIT_RESET) {
            session.clearHistory();
            responseText = "";
            transcription = "";
            invalidate();
            return true;
        }
        return false;
    }
}
