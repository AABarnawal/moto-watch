package dev.lcdclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Official Google Gemini REST API Provider.
 * Connects securely to generativelanguage.googleapis.com using configured API keys.
 */
public final class GeminiProvider implements AIProvider {
    private static final String TAG = "GeminiProvider";
    private static final String PREFS_NAME = "assistant_prefs";
    private static final String KEY_API_KEY = "gemini_api_key";
    private static final String KEY_MODEL = "gemini_model";

    public static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int TIMEOUT_MS = 12000;

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GeminiProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    public static String getApiKey(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_API_KEY, "");
    }

    public static void setApiKey(Context context, String apiKey) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_API_KEY, apiKey != null ? apiKey.trim() : "")
                .apply();
    }

    public static String getModel(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_MODEL, DEFAULT_MODEL);
    }

    public static void setModel(Context context, String model) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MODEL, model != null ? model.trim() : DEFAULT_MODEL)
                .apply();
    }

    @Override
    public String getName() {
        return "Google Gemini (" + getModel(context) + ")";
    }

    @Override
    public boolean isConfigured() {
        String key = getApiKey(context);
        return key != null && !key.trim().isEmpty();
    }

    @Override
    public void generateResponse(List<Message> history, Callback callback) {
        String apiKey = getApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(() -> callback.onError("Gemini API key not configured. Set it in LCD Clock on the main screen."));
            return;
        }

        String model = getModel(context);
        String endpoint = BASE_URL + model + ":generateContent?key=" + apiKey;

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject payload = buildPayload(history);
                byte[] postData = payload.toString().getBytes(StandardCharsets.UTF_8);

                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(postData.length);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseBody = readStream(is);

                if (code == 200) {
                    String answer = parseResponse(responseBody);
                    mainHandler.post(() -> callback.onSuccess(answer));
                } else if (code == 400) {
                    mainHandler.post(() -> callback.onError("Invalid request to Gemini. Check prompt."));
                } else if (code == 401 || code == 403) {
                    mainHandler.post(() -> callback.onError("API key invalid or unauthorized."));
                } else if (code == 429) {
                    mainHandler.post(() -> callback.onError("Gemini rate limit exceeded. Wait a moment."));
                } else {
                    mainHandler.post(() -> callback.onError("Gemini server error (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Gemini request failed", e);
                mainHandler.post(() -> callback.onError("Network connection failed."));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private JSONObject buildPayload(List<Message> history) throws JSONException {
        JSONObject root = new JSONObject();

        // System Instruction to keep answers concise for small 368x194 display & TTS
        JSONObject systemInstruction = new JSONObject();
        JSONObject systemPart = new JSONObject();
        systemPart.put("text", "You are an AI voice assistant running on a Motorola Razr 40 cover screen (368x194 px). "
                + "Provide extremely concise, helpful, and natural spoken answers. "
                + "Limit responses to 1-2 short sentences unless specifically asked for details.");
        systemInstruction.put("parts", new JSONArray().put(systemPart));
        root.put("systemInstruction", systemInstruction);

        JSONArray contents = new JSONArray();
        for (Message msg : history) {
            JSONObject content = new JSONObject();
            content.put("role", msg.role());
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", msg.text());
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
        }
        root.put("contents", contents);

        JSONObject config = new JSONObject();
        config.put("temperature", 0.7);
        config.put("maxOutputTokens", 160);
        root.put("generationConfig", config);

        return root;
    }

    private String parseResponse(String jsonString) throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates != null && candidates.length() > 0) {
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.optJSONObject("content");
            if (content != null) {
                JSONArray parts = content.optJSONArray("parts");
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "").trim();
                }
            }
        }
        return "No response generated.";
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
