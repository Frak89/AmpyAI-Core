package com.ampyai.core;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Context;
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
                      "e cerco fonti online verificabili. Non sono ancora un modello linguistico neurale. " +
                      "Prova: proponi uno stile, crea dashboard stile cyberpunk, " +
                      "cerca online material design, crea pulsante Salva, aiuto.");
                chat.logStage("Ho spiegato le capacità disponibili.");
                break;
            case STYLE:
                beginStyleProposal(decision);
                break;
            default:
                processBasicCommand(prompt);
        }
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
        FragStyle proposed = brain.propose(decision.style, null);
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
                brain.remember(prior, true);
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
                    brain.remember(prior, false);
                    reply("Capito: «" + prior.label + "» non ti convince. Cerco un'alternativa.");
                } else reply("Provo una proposta grafica differente.");
                String baseline = pendingBefore;
                document = UiDocument.parse(baseline);
                FragStyle next = brain.propose(null, prior.id);
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
