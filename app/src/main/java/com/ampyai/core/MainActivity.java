package com.ampyai.core;

import android.app.Activity;
import android.content.SharedPreferences;
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
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rev1: an empty Android canvas, an AmpyDev chat, and live rule-driven UI editing.
 * The module registry makes room for future local AI without coupling UI to inference.
 */
public final class MainActivity extends Activity {
    private static final String PREFS = "ampyai_dev";
    private SharedPreferences preferences;
    private UiDocument document;
    private AmpyCanvasView canvas;
    private AmpyDevChatView chat;
    private LinearLayout root;
    private final ArrayDeque<String> undo = new ArrayDeque<>();
    private final List<DevModule> modules = new ArrayList<>();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        document = UiDocument.parse(preferences.getString("canvas", null));
        modules.add(new BasicUiModule());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0xFF10131A);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF10131A);
        setContentView(root);

        // Android 15+/target 35+ draw behind system bars; keep clickable UI visible.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets system = insets.getInsets(WindowInsets.Type.systemBars());
                top = system.top;
                bottom = system.bottom;
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
        title.setText("AMPYAI  /  CANVAS");
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
        developerButton.setContentDescription("Apri o chiudi chat AmpyDev");
        header.addView(developerButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(58)));

        canvas = new AmpyCanvasView(this, this::onNodeClicked, this::saveScene);
        root.addView(canvas, new LinearLayout.LayoutParams(-1, 0, 1f));

        chat = new AmpyDevChatView(this, this::onPrompt, this::closeChat);
        chat.restore(preferences.getString("chat", null));
        root.addView(chat, new LinearLayout.LayoutParams(-1, dp(330)));
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
        String snapshot = document.encode();
        DevModule.Result outcome;
        if (prompt.equalsIgnoreCase("annulla") || prompt.equalsIgnoreCase("undo")) {
            if (undo.isEmpty()) outcome = new DevModule.Result(false, "Non ci sono modifiche da annullare.");
            else {
                document = UiDocument.parse(undo.pop());
                outcome = new DevModule.Result(true, "Ultima modifica annullata.");
            }
        } else {
            outcome = null;
            for (DevModule module : modules) {
                outcome = module.apply(prompt, document);
                if (outcome != null) break;
            }
            if (outcome == null) {
                outcome = new DevModule.Result(false,
                        "Questa richiesta non è ancora supportata dal motore Rev1. " +
                        "Posso aggiungere titoli, testo, pulsanti, campi, schede, " +
                        "cambiare sfondo, collegare messaggi ai clic e creare template. Scrivi aiuto.");
            } else if (outcome.changed) {
                undo.push(snapshot);
                while (undo.size() > 20) undo.removeLast();
            }
        }
        if (outcome.changed) {
            saveScene();
            canvas.render(document);
        }
        chat.append("AmpyDev", outcome.reply);
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

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (chat != null && chat.getVisibility() == View.VISIBLE) closeChat();
        else super.onBackPressed();
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
