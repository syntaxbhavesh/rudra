# RUDRA
**Responsive Utility, Dialogue & Robotic Assistant**

An independent Android assistant project. This repository is separate from Shiva AI. Shiva may be used only as an optional AI service through a secure backend API.

## Starter features
- JARVIS-inspired dark Android interface (Jetpack Compose)
- Tap-to-talk speech recognition
- Spoken replies using Android Text-to-Speech
- Local commands to open YouTube, WhatsApp, Chrome and Android Settings
- GitHub Actions workflow to build a debug APK

## Build
Open this repository in Android Studio with JDK 17 and Android SDK 35, sync Gradle, then run the app. Or run `gradle assembleDebug` in a compatible Android build environment.

## Roadmap
1. Complete and test voice-first Android app.
2. Add a secure RUDRA-to-Shiva API adapter after confirming the deployed API contract.
3. Add optional wake-word activation using an on-device engine and an Android foreground service with clear microphone controls.
4. Add more user-approved Android intents and integrations.

## Safety and privacy
RUDRA does not secretly monitor the microphone. Tap-to-talk is the initial voice mode. Wake-word mode requires explicit setup, visible microphone status, and Android permissions. Do not put private API keys in the APK or repository.
