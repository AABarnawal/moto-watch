package dev.lcdclock;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Manages the AI voice session lifecycle:
 * Speech recognition -> Conversational history -> Gemini AI provider -> Text-to-Speech playback.
 */
public final class VoiceSessionManager {
    private static final String TAG = "VoiceSessionManager";
    private static final String UTTERANCE_ID = "assistant_reply";

    public enum State {
        IDLE,
        LISTENING,
        TRANSCRIBING,
        PROCESSING,
        SPEAKING,
        ERROR,
        PERMISSION_REQUIRED
    }

    public interface Listener {
        void onStateChanged(State state, String statusMessage);
        void onRmsChanged(float rmsDb);
        void onTranscriptionUpdated(String partialText);
        void onResponseReceived(String replyText);
    }

    private final Context context;
    private final AIProvider aiProvider;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<AIProvider.Message> history = new ArrayList<>();

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private Listener listener;
    private State currentState = State.IDLE;
    private String lastErrorMessage = "";

    public VoiceSessionManager(Context context, AIProvider provider) {
        this.context = context.getApplicationContext();
        this.aiProvider = provider;
        initTts();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public State getState() {
        return currentState;
    }

    public List<AIProvider.Message> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void clearHistory() {
        history.clear();
        stopSpeaking();
        setState(State.IDLE, "Session ended. Tap mic to start.");
    }

    private void setState(State state, String message) {
        this.currentState = state;
        if (state == State.ERROR) {
            this.lastErrorMessage = message;
        }
        if (listener != null) {
            mainHandler.post(() -> listener.onStateChanged(state, message));
        }
    }

    private void initTts() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        setState(State.SPEAKING, "Speaking...");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        setState(State.IDLE, "Tap mic to reply");
                    }

                    @Override
                    public void onError(String utteranceId) {
                        setState(State.IDLE, "Tap mic to ask");
                    }
                });
                ttsReady = true;
            } else {
                Log.w(TAG, "TextToSpeech initialization failed");
            }
        });
    }

    /**
     * Starts listening for speech (tap-to-talk).
     */
    public void startListening() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setState(State.PERMISSION_REQUIRED, "Microphone permission required.");
            return;
        }

        stopSpeaking();

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            setState(State.ERROR, "Speech recognition unavailable on device.");
            return;
        }

        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }
        }

        mainHandler.post(() -> {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        setState(State.LISTENING, "Listening... speak now");
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        setState(State.TRANSCRIBING, "Listening...");
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                        if (listener != null) {
                            listener.onRmsChanged(rmsdB);
                        }
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                        setState(State.PROCESSING, "Thinking...");
                    }

                    @Override
                    public void onError(int error) {
                        Log.w(TAG, "SpeechRecognizer error: " + error);
                        String msg = "Recognition error";
                        switch (error) {
                            case SpeechRecognizer.ERROR_AUDIO -> msg = "Audio recording error";
                            case SpeechRecognizer.ERROR_CLIENT -> msg = "Client error";
                            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> msg = "Microphone permission denied";
                            case SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> msg = "Network error";
                            case SpeechRecognizer.ERROR_NO_MATCH -> msg = "No speech detected. Speak closer to mic.";
                            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> msg = "Microphone busy, try again";
                            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> msg = "No speech heard. Try again.";
                        }
                        setState(State.ERROR, msg);
                    }

                    @Override
                    public void onResults(Bundle results) {
                        ArrayList<String> matches = results != null
                                ? results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) : null;
                        if (matches != null && !matches.isEmpty()) {
                            String query = matches.get(0);
                            if (listener != null) {
                                listener.onTranscriptionUpdated(query);
                            }
                            processUserQuery(query);
                        } else {
                            setState(State.ERROR, "Could not understand speech.");
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                        ArrayList<String> matches = partialResults != null
                                ? partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) : null;
                        if (matches != null && !matches.isEmpty() && listener != null) {
                            listener.onTranscriptionUpdated(matches.get(0));
                        }
                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {
                    }
                });

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                String lang = Locale.getDefault().toLanguageTag();
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);
                intent.putExtra("android.speech.extra.DICTATION_MODE", true);

                speechRecognizer.startListening(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to start speech recognizer", e);
                setState(State.ERROR, "Unable to activate microphone.");
            }
        });
    }

    /**
     * Cancels active speech recognition.
     */
    public void stopListening() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
            } catch (Exception ignored) {
            }
        }
        if (currentState == State.LISTENING || currentState == State.TRANSCRIBING) {
            setState(State.IDLE, "Cancelled");
        }
    }

    /**
     * Stops spoken TTS response.
     */
    public void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        if (currentState == State.SPEAKING) {
            setState(State.IDLE, "Tap mic to ask");
        }
    }

    private void processUserQuery(String query) {
        setState(State.PROCESSING, "Thinking...");
        history.add(new AIProvider.Message(AIProvider.Message.ROLE_USER, query));

        if (!aiProvider.isConfigured()) {
            String demoReply = "I heard: \"" + query + "\". To get real-time answers from Gemini, enter your Gemini API key in LCD Clock Settings.";
            history.add(new AIProvider.Message(AIProvider.Message.ROLE_MODEL, demoReply));
            if (listener != null) {
                listener.onResponseReceived(demoReply);
            }
            speakResponse(demoReply);
            return;
        }

        aiProvider.generateResponse(history, new AIProvider.Callback() {
            @Override
            public void onSuccess(String responseText) {
                history.add(new AIProvider.Message(AIProvider.Message.ROLE_MODEL, responseText));
                if (listener != null) {
                    listener.onResponseReceived(responseText);
                }
                speakResponse(responseText);
            }

            @Override
            public void onError(String errorMessage) {
                setState(State.ERROR, errorMessage);
            }
        });
    }

    private void speakResponse(String text) {
        if (ttsReady && textToSpeech != null && text != null && !text.isEmpty()) {
            setState(State.SPEAKING, "Speaking...");
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
        } else {
            setState(State.IDLE, "Tap mic to reply");
        }
    }

    /**
     * Releases all background resources (speech recognizer and TTS engine).
     */
    public void release() {
        stopListening();
        stopSpeaking();
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            try {
                textToSpeech.shutdown();
            } catch (Exception ignored) {
            }
            textToSpeech = null;
            ttsReady = false;
        }
    }
}
