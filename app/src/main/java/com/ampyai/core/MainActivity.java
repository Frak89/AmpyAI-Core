package com.ampyai.core;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fråg Rev2: live activity + feedback-controlled UI previews + sourced research.
 * Existing Rev1 canvas and DevModule contracts remain intact.
 */
public final class MainActivity extends Activity {
    private static final String PREFS = "ampyai_dev";
    private SharedPreferences preferences;
    private UiDocument document;
    private AmpyCanvasView canvas;
    private AmpyDevChatView chat;
    private LinearLayout root;
    private FragBrain brain;
    private FragKnowledgeStore knowledge;
    private FragTrainingStore trainingStore;
    private volatile FragNeuralTrainer.Model trainedModel;
    private static final int REQUEST_DATASET = 1027;
    private final ExecutorService trainerExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean training = new AtomicBoolean(false);
    private final FragResearch research = new FragResearch();
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final AtomicBoolean searching = new AtomicBoolean(false);
    private final ArrayDeque<String> undo = new ArrayDeque<>();
    private final List<DevModule> modules = new ArrayList<>();
    private FragStyle pendingStyle;
    private String pendingBefore;
    private String pendingRequest;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        document = UiDocument.parse(preferences.getString("canvas", null));
        brain = new FragBrain(preferences);
        knowledge = new FragKnowledgeStore(getApplicationContext());
        trainingStore = new FragTrainingStore(getApplicationContext());
        try {
            trainedModel = trainingStore.loadModel();
        } catch (IOException ignored) {
            trainedModel = null;
        }
        modules.add(new BasicUiModule());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0xFF10131A);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF10131A);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            root.setPadding(0, top, 0, bottom);
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(6), dp(14), dp(6));
        TextView title = new TextView(this);
        title.setText("AMPYAI  /  CANVAS  /  Fråg 🐸");
        title.setTextColor(0xFF8F9BB0);
        title.setTextSize(12f);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1));
        title.setGravity(Gravity.CENTER_VERTICAL);

        TextView developerButton = new TextView(this);
        developerButton.setText("AmpyDev");
        developerButton.setTextColor(Color.WHITE);
        developerButton.setTextSize(14f);
        developerButton.setGravity(Gravity.CENTER);
        developerButton.setTypeface(null, Typeface.BOLD);
        developerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ampydev, 0, 0, 0);
        developerButton.setCompoundDrawablePadding(dp(6));
        developerButton.setPadding(dp(12), 0, dp(12), 0);
        developerButton.setBackground(rounded(0xFF252E42, 13));
        developerButton.setContentDescription("Apri o chiudi Fråg");
        header.addView(developerButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(58)));

        canvas = new AmpyCanvasView(this, this::onNodeClicked, this::saveScene);
        root.addView(canvas, new LinearLayout.LayoutParams(-1, 0, 1f));
        chat = new AmpyDevChatView(this, this::onPrompt, this::onFeedback, this::closeChat);
        chat.restore(preferences.getString("chat", null));
        if (trainedModel != null) chat.logStage(
                "Neural Core caricato: " + trainedModel.examples + " esempi.");
        root.addView(chat, new LinearLayout.LayoutParams(-1, dp(366)));
        chat.setVisibility(View.GONE);
        developerButton.setOnClickListener(v -> {
            if (chat.getVisibility() == View.VISIBLE) closeChat();
            else chat.setVisibility(View.VISIBLE);
        });

        canvas.render(document);
        root.requestApplyInsets();
    }

    private void onPrompt(String prompt) {
        chat.append("Tu", prompt);
        saveChat();
        chat.logStage("Interpreto: " + shortText(prompt));
        if (handleTrainingRequest(prompt)) return;
        if (prompt.equalsIgnoreCase("annulla") || prompt.equalsIgnoreCase("undo")) {
            cancelPendingProposal();
            if (undo.isEmpty()) reply("Non ci sono modifiche da annullare.");
            else {
                document = UiDocument.parse(undo.pop());
                canvas.render(document);
                saveScene();
                chat.logStage("Canvas ripristinato.");
                reply("Modifica annullata.");
            }
            return;
        }

        FragBrain.Decision decision = brain.classify(prompt);
        switch (decision.kind) {
            case SEARCH:
                startResearch(decision.argument);
                break;
            case HELP:
                reply("Sono Fråg 🐸, una AI simbolica locale sperimentale: " +
                      "interpreto comandi, faccio anteprime, registro preferenze " +
                      "e cerco fonti online verificabili. Fråg V2 include anche una piccola " +
                      "rete neurale da addestrare sul dispositivo (non è un LLM). " +
                      "Prova: esempi base, allena Fråg, prova AI: crea un tasto, " +
                      "aiuto training, proponi uno stile, cerca online material design.");
                chat.logStage("Ho spiegato le capacità disponibili.");
                break;
            case STYLE:
                beginStyleProposal(decision);
                break;
            default:
                processBasicCommand(prompt);
        }
    }


    /**
     * The trainer is opt-in. All manual labels and imported datasets are stored
     * privately. There is no hidden scraping or remote training service.
     */
    private boolean handleTrainingRequest(String prompt) {
        String q = prompt.trim();
        String lower = q.toLowerCase(Locale.ROOT);
        if (lower.equals("aiuto training") || lower.equals("come ti alleno")
                || lower.equals("trainer") || lower.equals("addestramento")) {
            reply("🐸 Fråg Trainer V2 — vero addestramento neurale offline!\n\n" +
                "1. «esempi base» carica 70 esempi etichettati.\n" +
                "2. «insegna: metti un tasto Invia => pulsante» aggiunge un esempio.\n" +
                "3. «importa dataset» apre un JSON o JSONL dal telefono.\n" +
                "4. «allena Fråg» aggiorna i pesi e mostra epoche e perdita LIVE.\n" +
                "5. «prova AI: inserisci un bottone con scritto Conferma» testa senza cambiare UI.\n" +
                "6. «esegui AI: metti un tasto chiamato Invia» applica solo con alta confidenza.\n" +
                "7. «statistiche training» mostra esempi e accuratezza su dati esclusi dall'allenamento.\n" +
                "Etichette: pulsante, titolo, testo, campo, scheda, login, dashboard.\n" +
                "È un classificatore neurale, non un LLM.");
            chat.logStage("Istruzioni Trainer mostrate.");
            return true;
        }
        if (lower.equals("esempi base") || lower.equals("carica esempi base")
                || lower.equals("prepara esempi")) {
            int added = trainingStore.seed();
            reply("📘 Esempi di base: " + added + " nuovi. Archivio: "
                    + trainingStore.size() + " esempi. Ora scrivi «allena Fråg».");
            chat.logStage("Dataset iniziale aggiornato.");
            return true;
        }
        if (lower.equals("statistiche training") || lower.equals("stato training")
                || lower.equals("statistiche ai")) {
            FragNeuralTrainer.Model model = trainedModel;
            reply("📊 Fråg Neural Core\nEsempi archiviati: " + trainingStore.size()
                    + "\nModello addestrato: " + (model == null ? "no" : "sì")
                    + (model == null ? "" : "\nEsempi nell'ultimo training: " + model.examples
                    + "\nEpoche: " + model.epochs
                    + "\nAccuratezza di validazione: " +
                    String.format(Locale.ITALY, "%.1f%%", model.validationAccuracy * 100.0))
                    + "\nLa valutazione usa esempi separati dal training; non misura capacità linguistiche generali.");
            return true;
        }
        Matcher label = Pattern.compile("^insegna\\s*:\\s*(.+?)\\s*=>\\s*(\\S+)\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(q);
        if (label.matches()) {
            if (training.get()) {
                reply("Sto già allenando. Insegna nuovi esempi alla fine dell'addestramento.");
                return true;
            }
            int kind = FragNeuralTrainer.label(label.group(2));
            if (kind == -1) reply("Etichetta non valida. Usa pulsante, titolo, testo, campo, scheda, login o dashboard.");
            else {
                boolean inserted = trainingStore.add(label.group(1), label.group(2), "manual");
                reply(inserted ? "✅ Esempio aggiunto per «" +
                        FragNeuralTrainer.LABELS[kind] + "». Esempi totali: " +
                        trainingStore.size() + "." : "Esempio duplicato o troppo corto/lungo.");
                chat.logStage("Dataset locale aggiornato.");
            }
            return true;
        }
        if (lower.equals("importa dataset") || lower.equals("importa esempi")) {
            if (training.get()) {
                reply("Completa prima l'allenamento in corso.");
                return true;
            }
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.setType("*/*");
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"application/json", "application/x-ndjson",
                            "text/plain", "application/octet-stream"});
            try {
                startActivityForResult(picker, REQUEST_DATASET);
                chat.logStage("In attesa di un file JSON/JSONL scelto dall'utente.");
            } catch (Exception e) {
                reply("Selettore file non disponibile.");
            }
            return true;
        }
        if (lower.equals("allena fråg") || lower.equals("allena frag")
                || lower.equals("avvia training") || lower.equals("addestra fråg")) {
            launchTraining();
            return true;
        }
        Matcher test = Pattern.compile("^(prova ai|esegui ai)\\s*:\\s*(.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(q);
        if (test.matches()) {
            predictInstruction(test.group(2).trim(),
                    test.group(1).equalsIgnoreCase("esegui ai"));
            return true;
        }
        return false;
    }

    private void launchTraining() {
        if (!training.compareAndSet(false, true)) {
            reply("Fråg si sta già allenando.");
            return;
        }
        chat.logStage("Preparo dataset e split di validazione.");
        reply("🧠 Training neurale locale avviato. Osserva epoche e perdita nel pannello LIVE.");
        trainerExecutor.execute(() -> {
            try {
                List<FragNeuralTrainer.Example> examples = trainingStore.examples();
                FragNeuralTrainer.Model trained = FragNeuralTrainer.train(examples,
                        (epoch, total, loss) -> runOnUiThread(() -> {
                            if (!isDestroyed()) chat.logStage("Epoca " + epoch + "/" + total
                                    + " · loss " + String.format(Locale.US, "%.3f", loss));
                        }));
                trainingStore.saveModel(trained);
                trainedModel = trained;
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        chat.logStage("Pesi addestrati e salvati sul dispositivo.");
                        reply("✅ Fråg Neural Core addestrato: " + trained.examples +
                            " esempi, " + trained.epochs + " epoche. Accuratezza " +
                            "di validazione: " +
                            String.format(Locale.ITALY, "%.1f%%", trained.validationAccuracy * 100)
                            + ".\nProva ora «prova AI: ...». Questo punteggio vale " +
                            "solo per il piccolo dataset di richieste UI.");
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        chat.logStage("Allenamento non completato.");
                        reply("Training non riuscito: " + error.getMessage()
                            + "\nProva prima «esempi base».");
                    }
                });
            } finally {
                training.set(false);
            }
        });
    }

    private void predictInstruction(String instruction, boolean execute) {
        FragNeuralTrainer.Model model = trainedModel;
        if (model == null) {
            reply("Devo prima essere addestrato. Scrivi «esempi base» e «allena Fråg».");
            return;
        }
        FragNeuralTrainer.Prediction prediction = FragNeuralTrainer.predict(model, instruction);
        String score = String.format(Locale.ITALY, "%.1f%%", prediction.score * 100);
        chat.logStage("Rete neurale: " + prediction.label + " · " + score + ".");
        if (!execute) {
            reply("🐸 Classificazione neurale\nTesto: " + instruction
                + "\nIntento: " + prediction.label + "\nPunteggio softmax: " + score
                + "\nMargine sul secondo intento: "
                + String.format(Locale.ITALY, "%.1f%%", prediction.margin * 100)
                + "\nÈ una previsione sperimentale: può sbagliare.");
            return;
        }
        if (prediction.score < 0.75 || prediction.margin < 0.30) {
            reply("⚠ Fråg non è abbastanza sicuro per modificare il canvas (" + score +
                    "). Insegnami la frase con «insegna: ... => etichetta», poi riallenami.");
            return;
        }
        String command;
        if (prediction.label.equals("login")) command = "crea schermata login";
        else if (prediction.label.equals("dashboard")) command = "crea dashboard";
        else {
            String label = extractDisplayText(instruction);
            if (label.isEmpty()) {
                reply("Ho riconosciuto «" + prediction.label + "», ma manca il testo esatto. "
                    + "Prova «esegui AI: metti un pulsante con scritto Conferma».");
                return;
            }
            String type = prediction.label.equals("button") ? "pulsante" :
                prediction.label.equals("input") ? "campo" :
                prediction.label.equals("card") ? "scheda" :
                prediction.label.equals("title") ? "titolo" : "testo";
            command = "crea " + type + " " + label;
        }
        chat.logStage("Applico comando UI riconosciuto: " + shortText(command));
        processBasicCommand(command);
    }

    private String extractDisplayText(String instruction) {
        Matcher m = Pattern.compile(
                "(?:con\\s+scritto|chiamat[oa]|etichettat[oa]|dal\\s+titolo|con\\s+testo|nome)\\s+(.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(instruction);
        if (!m.find()) return "";
        return m.group(1).trim();
    }

    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_DATASET || resultCode != RESULT_OK ||
                data == null || data.getData() == null) return;
        Uri uri = data.getData();
        chat.logStage("Leggo dataset locale scelto dall'utente (max 1 MB).");
        trainerExecutor.execute(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (in == null) throw new IOException("File non accessibile.");
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (output.size() + read > 1024 * 1024)
                        throw new IOException("Dataset troppo grande (max 1 MB).");
                    output.write(buffer, 0, read);
                }
                String contents = new String(output.toByteArray(), StandardCharsets.UTF_8);
                int inserted = trainingStore.importExamples(contents);
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        chat.logStage("Importazione terminata: " + inserted + " nuovi esempi.");
                        reply("📚 Importati " + inserted + " esempi. Totale: "
                                + trainingStore.size() + ". Scrivi «allena Fråg».");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isDestroyed()) reply("Importazione non riuscita: " + e.getMessage());
                });
            }
        });
    }

    private void processBasicCommand(String prompt) {
        cancelPendingProposal();
        String before = document.encode();
        chat.logStage("Verifico comandi grafici locali.");
        DevModule.Result result = null;
        for (DevModule module : modules) {
            result = module.apply(prompt, document);
            if (result != null) break;
        }
        if (result == null) {
            reply("Per ora capisco comandi UI guidati, ricerche esplicite e stili. " +
                  "Scrivi «aiuto», «proponi uno stile» o «cerca online design system».");
            chat.logStage("Richiesta non riconosciuta: nessuna modifica.");
            return;
        }
        if (result.changed) {
            pushUndo(before);
            canvas.render(document);
            saveScene();
            chat.logStage("Canvas modificato e salvato.");
        } else chat.logStage("Nessuna modifica al canvas.");
        reply(result.reply);
    }

    private void beginStyleProposal(FragBrain.Decision decision) {
        if (pendingStyle != null && pendingBefore != null) {
            document = UiDocument.parse(pendingBefore);
            pendingStyle = null;
        }
        String before = document.encode();
        String low = decision.argument.toLowerCase(Locale.ROOT);
        chat.logStage("Valuto le preferenze grafiche memorizzate.");
        FragStyle proposed = brain.propose(decision.style, null, decision.argument);
        // A request can contain both a layout and a style.
        if (low.contains("dashboard")) modules.get(0).apply("crea dashboard", document);
        else if (low.contains("login")) modules.get(0).apply("crea schermata login", document);
        pendingBefore = before;
        pendingRequest = decision.argument;
        pushUndo(before);
        preview(proposed);
    }

    private void preview(FragStyle proposed) {
        pendingStyle = proposed;
        chat.logStage("Applico l'anteprima " + proposed.label + " sul canvas.");
        proposed.previewIn(document);
        canvas.render(document);
        saveScene();
        chat.showStyleFeedback(proposed.label);
        reply("✨ Anteprima " + proposed.label + ": " + proposed.description + "\n"
              + brain.preferenceExplanation(proposed) + "\n"
              + "La vedi sullo schermo. Ti piace? Scegli uno dei pulsanti qui sotto.");
        chat.logStage("Attendo il tuo giudizio: 👍 / 👎 / ✨.");
    }

    private void onFeedback(String answer) {
        if (pendingStyle == null) {
            chat.dismissStyleFeedback();
            return;
        }
        FragStyle prior = pendingStyle;
        chat.logStage("Registro il feedback per " + prior.label + ".");
        switch (answer) {
            case "like":
                brain.remember(prior, true, pendingRequest);
                pendingStyle = null;
                pendingBefore = null;
                pendingRequest = null;
                chat.dismissStyleFeedback();
                saveScene();
                reply("🐸 Mi piace che ti piaccia! Memorizzo «" + prior.label
                        + "» come preferenza locale. Stile confermato.");
                chat.logStage("Preferenza approvata e salvata offline.");
                break;
            case "dislike":
            case "alternative":
                if ("dislike".equals(answer)) {
                    brain.remember(prior, false, pendingRequest);
                    reply("Capito: «" + prior.label + "» non ti convince. Cerco un'alternativa.");
                } else reply("Provo una proposta grafica differente.");
                String baseline = pendingBefore;
                document = UiDocument.parse(baseline);
                FragStyle next = brain.propose(null, prior.id, pendingRequest);
                preview(next);
                break;
            default:
                reply("Scelta non riconosciuta.");
        }
    }

    private void cancelPendingProposal() {
        pendingStyle = null;
        pendingBefore = null;
        pendingRequest = null;
        chat.dismissStyleFeedback();
    }

    private void startResearch(String term) {
        if (term.length() < 2 || term.length() > 100) {
            reply("Scrivi una ricerca tra 2 e 100 caratteri.");
            return;
        }
        if (!searching.compareAndSet(false, true)) {
            reply("C'è già una ricerca in esecuzione. Riprova dopo il risultato.");
            return;
        }
        chat.logStage("Ricerca richiesta: " + shortText(term));
        reply("🔎 Cerco davvero online per «" + term + "». Mostrerò le fonti, non inventerò risultati.");
        network.execute(() -> {
            try {
                List<FragResearch.Hit> hits = research.find(term,
                    message -> runOnUiThread(() -> {
                        if (!isDestroyed()) chat.logStage(message);
                    }), knowledge);
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    if (hits.isEmpty()) {
                        reply("Nessun risultato disponibile. Controlla la connessione " +
                            "o prova termini diversi. Nessuna fonte inventata.");
                    } else {
                        StringBuilder summary = new StringBuilder("📚 Risultati per «" + term +
                            "» (Wikipedia e GitHub, oppure cache locale):");
                        for (FragResearch.Hit hit : hits) {
                            summary.append("\n\n[").append(hit.provider).append("] ")
                                .append(hit.title).append("\n").append(hit.snippet)
                                .append("\n").append(hit.url);
                        }
                        summary.append("\n\nLe fonti sono cliccabili. Se vuoi, chiedimi uno stile da provare sul canvas.");
                        reply(summary.toString());
                    }
                    chat.logStage("Ricerca conclusa: " + hits.size() + " fonti.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        chat.logStage("Ricerca interrotta: " + e.getClass().getSimpleName());
                        reply("Ricerca non completata. Nessuna informazione inventata.");
                    }
                });
            } finally {
                searching.set(false);
            }
        });
    }

    private void pushUndo(String previous) {
        undo.push(previous);
        while (undo.size() > 20) undo.removeLast();
    }

    private static String shortText(String input) {
        return input.length() <= 48 ? input : input.substring(0, 48) + "…";
    }

    private void reply(String message) {
        chat.append("Fråg", message);
        saveChat();
    }

    private void saveChat() {
        preferences.edit().putString("chat", chat.exportHistory()).apply();
    }

    private void onNodeClicked(JSONObject node) {
        String type = node.optString("type");
        String title = node.optString("title");
        if ("button".equals(type) && "toast".equals(node.optString("action"))) {
            Toast.makeText(this, node.optString("message", "Clic!"), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Clic: " + (title.isEmpty() ? "elemento" : title),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveScene() {
        preferences.edit().putString("canvas", document.encode()).apply();
    }

    private void closeChat() {
        chat.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }

    @Override @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (chat != null && chat.getVisibility() == View.VISIBLE) closeChat();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        network.shutdownNow();
        trainerExecutor.shutdownNow();
        trainingStore.close();
        knowledge.close();
        super.onDestroy();
    }

    private GradientDrawable rounded(int fill, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
