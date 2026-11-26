# RUDRA
**Responsive Utility, Dialogue & Robotic Assistant**

Current app version: **1.0.0**

RUDRA is an independent Android assistant. Its Android app, UI, settings and repository are separate from Shiva AI. Shiva is used as an AI service through its existing HTTPS API.

## Current features
- JARVIS-inspired dark chat interface
- Tap-to-talk speech recognition
- Optional app-open-only “Hey RUDRA” wake-word mode with a visible microphone/listening indicator and an in-app off switch
- Spoken AI answers with Android Text-to-Speech
- Typed chat with conversation context sent to Shiva
- Streamed Shiva AI responses displayed in the chat
- On-device chat history persistence (up to 200 non-empty messages)
- Clear conversation control with confirmation
- Local commands for time/date, listing and opening installed apps by displayed name, opening Wi-Fi/Bluetooth/display/sound settings, handing alarm/timer setup to Android Clock, scheduling local reminder notifications, opening web searches in the browser, searching destinations in Maps, creating calendar-event drafts, and opening email/message/phone composers
- GitHub Actions debug APK workflow

## Shiva AI integration
The Android app sends conversation messages to:
`POST https://shiva-bookstore.vercel.app/api/chat`

The inspected Shiva endpoint accepts AI SDK UI messages with `role` and text `parts`, and responds as a text-delta event stream. RUDRA reads that stream and displays the accumulated answer. The same final answer is spoken using Android Text-to-Speech.

The deployed server controls its own Groq, Gemini and Cloudflare provider credentials. No provider API key is embedded in this Android app. The base URL can be overridden at build time with:
`-PSHIVA_API_BASE_URL=https://your-shiva-domain.example`

## Important backend note
The inspected `/api/chat` handler currently validates message structure but does not require a BOOKStore ID token. This app therefore does not pretend to provide BOOKStore sign-in or per-user identity. Do not add private credentials to this repository or APK. Consider adding server-side rate limiting and authentication to the Shiva endpoint before wider public distribution.

## Reminders and Android permissions
RUDRA reminders use Android alarms and local notifications. Android 13 and newer require the user to grant notification permission. Reminders are scheduled as inexact alarms to avoid requesting unrestricted exact-alarm access; delivery may be delayed by Android power management. Scheduled reminders may be cleared if the device restarts.

## Build
Use JDK 17 and Android SDK 35. Open in Android Studio and sync Gradle, or use the repository's GitHub Actions workflow to build a debug APK.

## Wake-word mode and privacy
Wake-word mode is optional and only runs while the RUDRA activity is in the foreground. It stops when the app is minimized or closed, and can be switched off in the app. It uses Android `SpeechRecognizer` sessions to detect “Hey RUDRA”; this is a prototype, not a dedicated low-power keyword model. The configured recognition provider may process audio, so this feature is not advertised as fully offline. The microphone indicator remains visible while wake-word mode is enabled.

Chat messages are stored locally on the device using Android app preferences (up to 200 messages). Clearing app data or uninstalling RUDRA removes this local history.
