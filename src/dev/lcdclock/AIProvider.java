package dev.lcdclock;

import java.util.List;

/**
 * Abstraction layer for AI conversational backends.
 */
public interface AIProvider {
    /**
     * A single message in a conversational session.
     */
    record Message(String role, String text) {
        public static final String ROLE_USER = "user";
        public static final String ROLE_MODEL = "model";
    }

    /**
     * Callback for asynchronous AI responses.
     */
    interface Callback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    /**
     * Sends conversation history to the AI provider.
     */
    void generateResponse(List<Message> history, Callback callback);

    /**
     * Provider identification name.
     */
    String getName();

    /**
     * Whether the provider is configured and ready (e.g. valid API key).
     */
    boolean isConfigured();
}
