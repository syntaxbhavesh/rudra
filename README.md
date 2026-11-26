# RUDRA 2.1
**Responsive Utility, Dialogue & Robotic Assistant**

Version: **2.1.0**

RUDRA is an independent Android assistant. Its app and repository remain separate from Shiva AI; Shiva provides conversational responses.

## Features
- Dark JARVIS-inspired animated dashboard, streamed Shiva AI chat, tap-to-talk, spoken replies and app-open-only “Hey RUDRA” mode
- Spoken-reply and quick-command visibility preferences persist locally between launches
- Dynamic installed-app discovery and launching
- Android handoffs for alarms, timers, reminders, Wi-Fi/Bluetooth/display/sound settings, web search, Maps, Calendar drafts, email, messages and dialer
- Local chat history and on-device notes: “remember that …”, “what do you remember?”, “forget my notes”
- Optional Android notification access: “open notification access”, enable RUDRA in Settings, then “read notifications”
- Clear local notification history with “clear notification history”
- Deterministic JVM tests for wake-phrase matching and safe reminder duration validation
- GitHub Actions debug APK build, unit tests, APK existence check and artifact upload

## Privacy
Notification access is disabled until the user enables it in Android settings. Captured notification content is retained locally, not sent to Shiva AI, and can be cleared. Notes and chat history are app-private local data, not cloud-synced. Wake-word listening only runs while RUDRA is foregrounded; the configured speech provider may process audio. RUDRA does not silently send messages, place calls, or automate other apps. Reminders use inexact alarms and may be delayed or cleared on device restart.

## Shiva AI
RUDRA sends chat messages to `POST https://shiva-bookstore.vercel.app/api/chat` and consumes the text-delta stream. Provider keys remain server-side. The inspected endpoint does not establish BOOKStore identity; there is no authenticated user sync or cloud memory.

## Build
GitHub Actions runs `gradle assembleDebug testDebugUnitTest`, verifies the APK, and uploads `rudra-2.1.0-debug-apk`. CI compilation is not a substitute for physical-device testing.


## Current scope and platform limits
RUDRA is a practical Android assistant, not unrestricted device automation. It hands supported actions to Android apps and requires user confirmation where appropriate. Web search and Maps are handoffs, not built-in web research. Shiva AI currently receives text chat; image/document understanding and image generation are not implemented in this client/backend contract. Notification access is opt-in and stays on-device. Wake-word detection is foreground-only and relies on the configured speech recognition service, not an always-on offline keyword model.
