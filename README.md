# Fråg 🐸 V3 — cervello ispezionabile e modellabile

Fråg è un esperimento di IA ibrida locale per Android: editor AmpyDev, classificatore neurale supervisionato sviluppato in Java, memoria grafica/di ricerca con fonti e visualizzazione effettiva del modello. Non è un LLM.

## Comandi nuovi di V3

| Comando | Funzione |
|---|---|
| `Cervello` | Apre la visualizzazione dello stato reale della rete, architettura e training |
| `Psicofråg` | Microscopio neurale: ingressi selezionati, neuroni nascosti, 7 output, probabilità e pesi reali campionati |
| `Lista cervello` | Tutte le funzionalità effettivamente installate e rispettivi comandi |
| `Approfondisci pulsante` | Cosa sa fare su una categoria + numero di esempi effettivamente memorizzati |
| `Approfondisci modella cervello` | Spiegazione tecnica di quella funzione con limiti |
| `Approfondisci Android` | Solo vere fonti già salvate se presenti; altrimenti dichiarazione esplicita che non lo conosce |
| `Modella cervello 48 100` | Prepara una rete con 48 neuroni nascosti e 100 epoche per il prossimo training |
| `esempi base` | Importa 70 esempi etichettati |
| `insegna: metti un bottone Conferma => pulsante` | Aggiunge un esempio supervisionato |
| `importa dataset` | Apre JSON o JSONL di esempi dal telefono |
| `allena Fråg` | Addestra offline con SGD e backprop, aggiorna log e grafico di Psicofråg |
| `statistiche training` | Numero esempi e accuratezza holdout del modello |
| `prova AI: inserisci un pulsante` | Classificazione non esecutiva |
| `esegui AI: metti un pulsante con scritto Prova` | Classificazione + comando UI, solo se soglia di confidenza alta |

## Psicofråg

La vista mostra le **dimensioni effettive** 384 → N → 7 del file pesi caricato, la situazione "NON ADDESTRATO" se non esiste un modello, e la richiesta di architettura per il prossimo addestramento. Quando esiste il modello, i nodi disegnati rappresentano un **sottoinsieme numerato di neuroni reali**, scelto dalle attivazioni calcolate sulla frase che si può cambiare nell'editor. Le linee sono un campione dei **pesi reali**. Non disegna un numero fittizio di neuroni e non pretende di mostrare processi di pensiero.

Durante l'allenamento col dialogo aperto, il grafico aggiorna i campioni di attivazioni e la perdita dalle epoche effettive. La progressione è visibile anche nel log LIVE dell'editor. Un training nuovo inizializza un nuovo modello con il numero di neuroni scelto: **non si tratta di una crescita neurale organica spontanea** e la modifica dei neuroni non modifica i pesi salvati finché non si completa il nuovo training.

## Compatibilità e persistenza

La database degli esempi `frag_training_v2.db` si conserva in Rev3. Fråg legge `frag_ui_model_v2.bin` quando ancora non esiste il nuovo file `frag_ui_model_v3.bin`; il successivo training salva dinamicamente il nuovo formato. Come nella V2, le capacità neuronali sono limitate a 7 classi di interfaccia. La lunghezza della finestra di testo di input è limitata a 384 caratteristiche hash numeriche indipendentemente dai neuroni scelti.

Strumenti della V2 ancora presenti: anteprime live di stile, feedback, ricerca esplicita (Wikipedia + repository GitHub pubbliche), cache locale delle fonti, creazione di layout semplici e comandi di base.

## Build

Java 17, Gradle 8.13, Android SDK 36; usa Android Studio o il workflow GitHub **Android debug APK** nella branch `feature/frag-rev3`. Il workflow esegue `testDebugUnitTest` e `assembleDebug`, quindi pubblica `AmpyAI-Core-Frag-Rev3-debug` con `app-debug.apk`.

**Limiti reali:** ancora niente apprendimento libero da PDF, riconoscimento universale, ragionamento generale, acquisizione spontanea di nuovi comandi operativi né auto-modifica del codice. Il comando `Approfondisci` è fondato solo su capacità nel registro, dataset etichettati e fonti già archiviate: non inventa comprensione. Il grafico mostra valori matematici veri, non attività di coscienza.


## Fråg V3 — Primo nucleo generativo autonomo (sperimentale)

Il progetto include ora **due reti neurali originali e distinte**:

1. **Neural UI Core:** precedente classificatore 384 → N → 7 classi grafiche.
2. **Language Core:** piccolo modello linguistico **autoregressivo neurale**, scritto in Java senza pesi preaddestrati. Codifica un contesto degli **ultimi 4 caratteri**, usa 64 neuroni ReLU, e predice il carattere successivo con una distribuzione softmax. Genera nuovo testo tramite campionamento. Non è un *large* language model: sa solo produrre continuazioni statistiche, frequentemente senza senso.

### Comandi da inviare uno per volta nella chat AmpyDev

- `inizia linguaggio` — salva un breve corpus originale italiano se il database è vuoto, crea e addestra da zero il piccolo modello.
- `parla: ciao` — ottiene un nuovo testo **veramente generato** dalla rete.
- `insegna lingua: un sensore misura una grandezza fisica` — salva nuovo materiale per il training, senza fingere di averlo già imparato.
- `allena linguaggio` — addestramento **incrementale**, continua dai pesi esistenti su un corpus conservato in locale.
- `studia parola: memoria` — richiesta esplicita HTTPS a **Wikizionario in italiano**, estrazione prudente di definizioni e URL fonte, salvataggio offline, poi aggiornamento incrementale del Language Core. Se la definizione non può essere ricavata in modo affidabile, non inventa nulla.
- `esplora dizionario` — studia **quattro** parole predefinite, non è un crawler senza limiti. Non prosegue né ricerca autonomamente a chat chiusa.
- `definisci memoria` — riporta la definizione e la fonte conservate, distinguendole dal testo generato.
- `Psicofråg` — mostra **due** viste: il classificatore UI e un microscopio del Language Core con input/uscite e pesi reali campionati, oltre a numero definizioni, testi, parametri e sessioni di addestramento.

Il dizionario usa l'API ufficiale MediaWiki `action=parse&prop=wikitext`; la fonte resta in memoria e viene mostrata con attribuzione al **Wikizionario (licenza CC BY-SA)**. Le risorse online sono consultate solo dopo un comando esplicito; richieste sequenziali e limitate, timeout e limite di byte. **Non è self-improvement generalista:** il numero di neuroni del Language Core è fisso; crescono corpus, definizioni consultabili e aggiornamenti dei pesi, non capacità programmabili. Restano necessari test con un telefono e con la connettività del dizionario.

Per evitare risposte inventate, `definisci X` usa soltanto definizioni veramente memorizzate. `parla: X` e richieste sconosciute indicano chiaramente che il testo è generato da un modello primordiale e non affidabile.

### Build e verifica

Il ramo `feature/frag-v3-language` estende `feature/frag-rev3` in modo separato. GitHub Actions compila e produce l'artifact `AmpyAI-Core-Frag-Rev3-Language-debug`; include anche test di apprendimento dei pesi e parser di definizioni con esempi sintetici. Nessuna telemetria esterna né API a pagamento.

### Piccolo passo di esplorazione autonoma, ma circoscritta

`cammina` (o `esplora dizionario`) esamina definizioni già salvate, sceglie **al massimo quattro nuove parole non ancora presenti** nel dizionario locale e tenta di recuperarne le definizioni dal Wikizionario. Al primo avvio usa parole iniziali neutre. Se ottiene definizioni nuove o cambiate, le salva con URL fonte e avvia addestramento incrementale del Language Core. Quando ripeti il comando, il candidato successivo può derivare dalle definizioni apprese precedentemente. Si tratta di una selezione euristica verificabile, **non** di comprensione o apprendimento autonomo illimitato. Non può navigare continuamente senza una nuova richiesta.
