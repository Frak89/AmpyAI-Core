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

## Fråg V2 — Trainer neurale originale (funzionante offline)

Apri AmpyDev. I comandi di addestramento sono intercettati prima del normale editor grafico.

1. **`aiuto training`** — mostra tutte le istruzioni.
2. **`esempi base`** — inserisce 70 esempi locali già etichettati (10 per classe).
3. **`insegna: metti un tasto con scritto Prova => pulsante`** — aggiunge il tuo esempio supervisionato.
4. **`importa dataset`** — scegli un file JSON/JSONL locale, massimo 1 MB / 5000 esempi.
5. **`allena Fråg`** — avvia 65 epoche di vera retropropagazione su una piccola rete originale; il log LIVE mostra avanzamento e perdita.
6. **`statistiche training`** — numero di esempi, epoche, accuratezza su holdout stratificato.
7. **`prova AI: inserisci un bottone con scritto Conferma`** — classificazione soltanto, non modifica il canvas.
8. **`esegui AI: metti un pulsante con scritto Conferma`** — applica al canvas soltanto se la previsione supera le soglie di confidenza e il testo da inserire è esplicito.

Etichette consentite: `pulsante`, `titolo`, `testo`, `campo`, `scheda`, `login`, `dashboard`. È supportata anche la forma inglese canonica di ogni etichetta.

**Formato JSONL** (una riga JSON per esempio):

```jsonl
{"text":"fammi un bottone con scritto Salva","label":"button"}
{"text":"aggiungi un titolo con scritto Benvenuto","label":"title"}
{"text":"crea una pagina di accesso","label":"login"}
```

Puoi importare anche un array JSON di oggetti con campi `text` e `label`. Gli esempi vengono conservati nel database privato `frag_training_v2.db`; il modello addestrato viene salvato in `frag_ui_model_v2.bin` e ricaricato ai successivi avvii.

### Dettagli tecnici e limiti

Il Neural Core è un classificatore sviluppato in Java puro senza pesi o modelli esterni: 384 caratteristiche hash di parole e sottoparole, strato nascosto ReLU da 32 unità, output softmax di 7 classi e aggiornamento SGD con backpropagation. Un sottoinsieme stratificato degli esempi viene escluso dalla retropropagazione e usato per la validazione. La percentuale ottenuta è valida solo per quel piccolo dataset e non indica una capacità generale di comprensione. L'apprendimento da nuovi esempi richiede di eseguire nuovamente `allena Fråg`. L'importazione di PDF e l'addestramento di LLM generativi **non** sono inclusi nella V2.
