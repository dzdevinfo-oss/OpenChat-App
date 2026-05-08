# OpenChat App

OpenChat is a modern, modular, Android application powered by AI. It features multi-provider LLM support, memory capabilities, workspace editor with syntax highlighting, settings configuration, voice input/output (TTS/STT), dynamic artifact rendering, and a smooth animated modern UI built with Jetpack Compose.

## Features
- **Multi-Model Support:** Chat with various AI models.
- **Dynamic Artifacts:** Preview generated HTML, React, and SVG code directly in the app.
- **Workspace Editor:** Sidebar and full-fledged code editor for file management.
- **Memory & Settings:** Save preferences, system prompts, and toggle features via DataStore.
- **Voice Interactions:** built-in Text-To-Speech (TTS) and Speech-To-Text (STT) support.
- **Modern UI:** Built on Jetpack Compose with Material 3 design and smooth animations.

## Building and Releasing

This project uses Gradle.

### Local Development
To build the application, run:
```bash
./gradlew assembleDebug
```
The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`. 

Make sure to install JDK 17.

### API Keys
All API keys are handled in-app via the Settings screen — no secrets are hardcoded in the codebase.

### GitHub Actions (APK Signing & Release)
GitHub Actions are configured to automatically build and sign releases.

1. **GitHub Secrets:** Add these secrets to your repository:
   - `KEYSTORE_BASE64`: Your keystore binary as a base64 string. Generate with: 
     `keytool -genkey -v -keystore openchat.jks -keyalg RSA -keysize 2048 -validity 10000 -alias openchat`
     Then encode: `base64 -w 0 openchat.jks`
   - `KEYSTORE_PASSWORD`: Your keystore password.
   - `KEY_ALIAS`: openchat
   - `KEY_PASSWORD`: Your key password.

2. **Trigger a Release:** Push a Git tag starting with `v`:
   ```bash
   git tag v1.0.0
   git push --tags
   ```
   The pipeline will build the signed APK and create a GitHub Release automatically.

3. **Installation:** Download the APK from the latest GitHub Release and install on your device (ensure "Install from Unknown Sources" is enabled).
