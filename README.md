# OpenChat App

OpenChat is a modern, modular, Android application powered by AI. It features multi-provider LLM support, memory capabilities, workspace editor with syntax highlighting, settings configuration, voice input/output (TTS/STT), dynamic artifact rendering, and a smooth animated modern UI built with Jetpack Compose.

## Features
- **Multi-Model Support:** Chat with various AI models.
- **Dynamic Artifacts:** Preview generated HTML, React, and SVG code directly in the app.
- **Workspace Editor:** Sidebar and full-fledged code editor for file management.
- **Memory & Settings:** Save preferences, system prompts, and toggle features via DataStore.
- **Voice Interactions:** built-in Text-To-Speech (TTS) and Speech-To-Text (STT) support.
- **Modern UI:** Built on Jetpack Compose with Material 3 design and smooth animations.

## Building the App

This project uses Gradle. To build the application:

### Debug Build
Run the following assembly command to generate a debug APK:
```bash
./gradlew assembleDebug
```
The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions
A CI pipeline is included in `.github/workflows/build.yml` which automatically builds the project and uploads the `app-debug.apk` on every push to the `main` or `master` branch.

## Setup

Make sure to install JDK 17 and set it as your default Java environment.
For local development, simply open the project in Android Studio.
