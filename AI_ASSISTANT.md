# AI Voice Assistant: Architecture & Integration Guide

## 1. Problem Statement & Motivation

On the stock Motorola Razr 40, invoking Google Assistant or native Gemini while folded yields a frustrating experience:
1. The user activates "Hey Google".
2. The outer screen displays "How can I help?".
3. Any practical voice command prompts: *"Unfold to unlock more"*.
4. Voice interactions fail until the user physically opens the phone.

The custom AI Voice Assistant in this application provides an autonomous, fully integrated voice assistant designed specifically for the external display, utilizing official Google Gemini REST APIs and Android system voice capabilities without requiring the phone to be unfolded.

---

## 2. Technical Architecture

```
+-------------------------------------------------------------+
|                     EXTERNAL DISPLAY (368x194)              |
|                                                             |
|   +-----------------------------------------------------+   |
|   | AssistantView (CoverPage)                           |   |
|   | - Pulsing Mic Button & Real-time Waveform           |   |
|   | - Live Speech Transcription                         |   |
|   | - Multi-line AI Response (Auto-scrolling)           |   |
|   | - Action Controls (Stop, Ask Another, End Session)  |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|                              v                              |
|   +-----------------------------------------------------+   |
|   | VoiceSessionManager                                 |   |
|   |                                                     |   |
|   |   +--------------------+    +-------------------+   |   |
|   |   | SpeechRecognizer   |    | TextToSpeech      |   |   |
|   |   | (Android Speech-to-|    | (Spoken responses |   |   |
|   |   |  Text on Display 1)|    |  via speaker)     |   |   |
|   |   +---------+----------+    +---------^---------+   |   |
|   |             |                         |             |   |
|   |             v                         |             |   |
|   |   +-----------------------------------+---------+   |   |
|   |   | Conversational Context (Multi-turn History) |   |   |
|   |   +----------------------+----------------------+   |   |
|   +--------------------------|--------------------------+   |
|                              |                              |
|                              v (HTTPS POST)                 |
|   +-----------------------------------------------------+   |
|   | GeminiProvider (AIProvider)                         |   |
|   | - Target: generativelanguage.googleapis.com         |   |
|   | - Model: gemini-1.5-flash                           |   |
|   | - Secure API Key Storage (Private SharedPreferences)|   |
|   +-----------------------------------------------------+   |
+-------------------------------------------------------------+
```

---

## 3. Component Details

### 3.1 `VoiceSessionManager.java`
Controls the session state machine:
- `IDLE`: Awaiting tap-to-talk.
- `LISTENING`: `SpeechRecognizer.startListening()` active; updates RMS audio levels.
- `TRANSCRIBING`: Interim recognition results streamed to UI.
- `PROCESSING`: Query dispatched to `AIProvider`; animated spinner.
- `SPEAKING`: AI response returned, auto-scrolled, and spoken via `TextToSpeech`.
- `ERROR`: Network, API key, or audio permission failures cleanly reported.
- `PERMISSION_REQUIRED`: Prompts user to grant `RECORD_AUDIO`.

### 3.2 `GeminiProvider.java`
Implements `AIProvider` using standard `HttpURLConnection` on a single-thread background executor:
- **System Instruction**: Enforces concise responses: *"You are an AI voice assistant running on a Motorola Razr 40 cover screen (368x194 px). Provide extremely concise, helpful, and natural spoken answers (1-2 short sentences)."*
- **Max Output Tokens**: Set to 160 tokens to prevent lengthy text overfilling the 1.5-inch viewport.
- **Error Handling**: Explicit handling for HTTP 400 (Bad Request), 401/403 (Invalid Key), 429 (Rate Limit Exceeded), and network disconnects.

### 3.3 Security & Privacy Credentials
- API keys are **never hardcoded** and **never committed**.
- The key is input by the user on the inside screen via `PreviewActivity` and saved to private, credential-encrypted `SharedPreferences` (`assistant_prefs`).
- Speech recognition is strictly tap-to-talk; no background microphone listeners or covert recording exist.
- When the screen turns off or the phone unfolds, all recognition and TTS engines immediately shutdown.

---

## 4. Setup & Usage

1. Open the phone to the main inside screen and launch **LCD Clock**.
2. Scroll to the **Gemini Voice Assistant** card.
3. Enter your Google Gemini API key (obtainable freely from [Google AI Studio](https://aistudio.google.com/)).
4. Tap **Save Assistant Key**.
5. Close the phone, wake the cover display, and swipe to the **Gemini** tab (sparkle icon).
6. Tap the microphone button to ask any question; Gemini responds visually and speaks aloud.
