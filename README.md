# AmpyAI Core — AmpyDev Rev1

A small, native and extensible Android UI designer. The app starts with an empty canvas and one AmpyDev icon. Tap AmpyDev to open a local chat. Enter a recognized visual instruction and the Android screen updates immediately. The elements are real Android Views, not screenshots, and button clicks are interactive.

## First run

1. Open the project folder in Android Studio (JDK 17, Android SDK 36).
2. Sync with Gradle 8.13 and Android Gradle Plugin 8.13.2.
3. Run the app on a device or emulator (Android 8.0+).
4. Tap AmpyDev in the upper-right corner; the chat is at the bottom of the canvas.

If your local build requires the Gradle wrapper, run "gradle wrapper --gradle-version 8.13" once from the root folder, or use a local Gradle 8.13 installation. CI builds use setup-gradle and do not require a checked-in wrapper binary.

The GitHub Actions workflow "Android debug APK" also builds a debug APK and uploads it as an Actions artifact after successful compilation. No release signing is configured.

## AmpyDev commands (Rev1)

- aggiungi titolo Benvenuto
- aggiungi testo Ciao mondo
- crea pulsante Salva
- crea campo Email
- aggiungi scheda Riepilogo
- aggiungi separatore
- sfondo blu
- sfondo #202840
- al clic su Salva mostra messaggio Dati salvati
- rinomina Salva in Conferma
- rimuovi Conferma
- rimuovi ultimo
- crea schermata login
- crea dashboard
- annulla
- svuota
- reset
- aiuto

Changes appear immediately without recompiling. The JSON scene and chat are persisted locally in private SharedPreferences. Inputs remember their text. Tapping a button displays the assigned message (or a simple click confirmation). Undo retains up to 20 edits while the app process remains active.

## Architecture

- UiDocument: versioned JSON scene model.
- AmpyCanvasView: renders JSON nodes as native Android widgets.
- AmpyDevChatView: message input, chat history and close button.
- DevModule: interface for independently registered command processors.
- BasicUiModule: deliberately small, offline, rule-driven command parser.
- MainActivity: composes the widgets, persists state and executes commands.
- .github/workflows/android-debug.yml: builds a debug APK on push and PR.

No Internet permission, login, cloud service or third-party AI models are necessary for Rev1.

## Important scope limitation

AmpyDev Rev1 is **not yet a trained AI model** and does **not** understand arbitrary graphic requests, install modules automatically or ingest databases/PDFs. It supports the documented commands. Future modules may introduce local language processing, visual editing, PDF ingestion and a verified source-backed knowledge engine without replacing the canvas or the in-app chat.

## Developer environment

- Java 17
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- compileSdk/targetSdk 36, minSdk 26
- package: com.ampyai.core
- zero third-party Android runtime dependencies

## Security

The UI renderer uses a fixed whitelist of native widget types. Text commands cannot execute arbitrary downloaded code. Extensible modules should be signed/validated and bound to declared capabilities before installation.
