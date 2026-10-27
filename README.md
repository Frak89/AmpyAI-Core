# AmpyAI Core — Fråg 🐸 Rev2

**Fråg** is an independently implemented on-device assistant inside the **AmpyDev** visual editor. It is a small symbolic AI/retrieval prototype, not a wrapper around an external LLM.

## Install / test

1. Open the project in Android Studio (Java 17, Android SDK 36, Gradle 8.13).
2. Build with `gradle :app:assembleDebug`, or open GitHub Actions > Android debug APK.
3. A successful workflow uploads `AmpyAI-Core-Frag-Rev2-debug`, a ZIP containing `app-debug.apk`.
4. Install the APK on Android 8+; integration on an actual phone still needs testing.
5. Tap **AmpyDev** at the top right to open Fråg's chat and activity feed.

## Try Fråg

- `crea dashboard stile cyberpunk`: the UI redraws immediately; tap **Mi piace**, **No**, or **Altro**.
- `proponi uno stile per fotografia`: Fråg ranks styles by feedback and learned word associations.
- `crea schermata login stile minimal`
- `cerca online material design`: makes actual requests to Wikipedia (Italian) and GitHub public repository search.
- `cerca online Siemens S7-1500`: searches those same two limited services; this is not a general web search engine.
- `crea pulsante Salva`, `aggiungi titolo Ciao`, `sfondo blu`, `annulla`, `aiuto`

## What works

Fråg shows real stages in a **LIVE** log while classifying a request, rendering previews, awaiting style approval, fetching sources, and saving local results. No fake progress timer is used. Asynchronous online results display clickable links and source names. Previous research snippets are stored with their original URL in local SQLite and can be reused if online fetching returns no results for the same query.

Approval and rejection update both style preference counters and a tiny online-trained bag-of-words association system persisted on the device in private SharedPreferences. This can alter later suggestions without requiring a network connection.

## Modules

- `FragBrain.java`: original symbolic intent router and local learned preference weights.
- `FragStyle.java`: visual style definitions used by the native canvas.
- `FragResearch.java`: fixed HTTPS public Wikipedia/GitHub source providers with limits and timeouts.
- `FragKnowledgeStore.java`: SQLite index of attributed research snippets.
- `AmpyDevChatView.java`: chat, activity log and functional style-feedback buttons.
- `UiDocument.java` / `AmpyCanvasView.java`: persistent JSON scene graph and real Android view renderer.
- `DevModule.java` / `BasicUiModule.java`: extensible command interface and existing Rev1 UI commands.
- `MainActivity.java`: orchestrator for immediate visual updates and asynchronous research.

## Honest limitations and privacy

Fråg Rev2 is an original **symbolic learning agent**, not a pretrained or newly trained Transformer/LLM. It cannot understand arbitrary language, perform broad web browsing, generate arbitrary app code, independently ingest PDF manuals, or train a large neural network. Those capabilities require future modules. Current style learning records explicit feedback and associations only.

Research occurs only after an explicit search command. It sends your query to the selected public services (Wikipedia and GitHub). Public source results can fail because of network restrictions, rate limits or API changes; Fråg reports those failures instead of inventing results. It never executes downloaded code. No account, cloud AI, tracking service or pretrained neural model is required.
