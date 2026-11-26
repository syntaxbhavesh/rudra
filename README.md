# RUDRA
**Responsive Utility, Dialogue & Robotic Assistant**

RUDRA is an independent Android assistant. Its Android app, UI, settings and repository are separate from Shiva AI. Shiva is used as an AI service through its existing HTTPS API.

## Current features
- JARVIS-inspired dark chat interface
- Tap-to-talk speech recognition
- Spoken AI answers with Android Text-to-Speech
- Typed chat with conversation context sent to Shiva
- Streamed Shiva AI responses displayed in the chat
- Local commands for time/date and opening YouTube, WhatsApp, Chrome or Android Settings
- GitHub Actions debug APK workflow

## Shiva AI integration
The Android app sends conversation messages to:
`POST https://shiva-bookstore.vercel.app/api/chat`

The inspected Shiva endpoint accepts AI SDK UI messages with `role` and text `parts`, and responds as a text-delta event stream. RUDRA reads that stream and displays the accumulated answer. The same final answer is spoken using Android Text-to-Speech.

The deployed server controls its own Groq, Gemini and Cloudflare provider credentials. No provider API key is embedded in this Android app. The base URL can be overridden at build time with:
`-PSHIVA_API_BASE_URL=https://your-shiva-domain.example`

## Important backend note
The inspected `/api/chat` handler currently validates message structure but does not require a BOOKStore ID token. This app therefore does not pretend to provide BOOKStore sign-in or per-user identity. Do not add private credentials to this repository or APK. Consider adding server-side rate limiting and authentication to the Shiva endpoint before wider public distribution.

## Build
Use JDK 17 and Android SDK 35. Open in Android Studio and sync Gradle, or use the repository's GitHub Actions workflow to build a debug APK.

## Roadmap
1. Verify the AI connection and build on a physical Android device.
2. Add optional on-device wake-word activation with a visible foreground-service microphone indicator and explicit controls.
3. Add persistent local conversation history and user-controlled integrations.

RUDRA does not secretly monitor the microphone. The initial voice mode is tap-to-talk.
