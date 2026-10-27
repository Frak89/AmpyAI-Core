package com.ampyai.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayDeque;

/**
 * Fråg live activity panel + persistent chat + real style feedback actions.
 * AmpyDev remains the UI editor and entry button.
 */
public final class AmpyDevChatView extends LinearLayout {
    public interface OnMessage { void send(String prompt); }
    public interface OnStyleFeedback { void submit(String value); }
    private final OnMessage onMessage;
    private final OnStyleFeedback onFeedback;
    private final ScrollView scroll;
    private final LinearLayout messages;
    private final EditText input;
    private final TextView status;
    private final TextView log;
    private final TextView question;
    private final LinearLayout feedbackBar;
    private final ArrayDeque<String> stages = new ArrayDeque<>();
    private JSONArray history = new JSONArray();

    public AmpyDevChatView(Context context, OnMessage onMessage,
                           OnStyleFeedback onFeedback, Runnable onClose) {
        super(context);
        this.onMessage = onMessage;
        this.onFeedback = onFeedback;
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF171C27);
        setElevation(dp(12));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(7), dp(12), dp(7));
        TextView name = new TextView(context);
        name.setText("🐸  Fråg  /  AmpyDev  ·  LIVE");
        name.setTextColor(0xFFA9E6D2);
        name.setTextSize(15f);
        name.setTypeface(null, Typeface.BOLD);
        header.addView(name, new LinearLayout.LayoutParams(0, dp(38), 1f));
        name.setGravity(Gravity.CENTER_VERTICAL);
        TextView close = new TextView(context);
        close.setText("✕");
        close.setContentDescription("Chiudi chat Fråg");
        close.setTextSize(21f);
        close.setTextColor(Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> onClose.run());
        header.addView(close, new LinearLayout.LayoutParams(dp(40), dp(38)));
        addView(header);

        LinearLayout live = new LinearLayout(context);
        live.setOrientation(VERTICAL);
        live.setPadding(dp(14), dp(8), dp(14), dp(8));
        live.setBackground(rounded(0xFF1E2931, 0));
        status = new TextView(context);
        status.setText("● Fråg pronta · locale");
        status.setTextColor(0xFF90E5BC);
        status.setTextSize(12f);
        status.setTypeface(null, Typeface.BOLD);
        live.addView(status);
        log = new TextView(context);
        log.setText("Apri un progetto grafico, chiedi uno stile o cerca sul web.");
        log.setTextColor(0xFFBBC9D3);
        log.setTextSize(11f);
        log.setMaxLines(5);
        log.setPadding(0, dp(4), 0, 0);
        live.addView(log);
        addView(live);

        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        messages = new LinearLayout(context);
        messages.setOrientation(VERTICAL);
        messages.setPadding(dp(12), dp(5), dp(12), dp(9));
        scroll.addView(messages);
        addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        feedbackBar = new LinearLayout(context);
        feedbackBar.setOrientation(VERTICAL);
        feedbackBar.setPadding(dp(12), dp(6), dp(12), dp(4));
        feedbackBar.setBackgroundColor(0xFF26352D);
        question = new TextView(context);
        question.setTextColor(0xFFE1FFEF);
        question.setTextSize(13f);
        question.setPadding(dp(2), 0, 0, dp(4));
        feedbackBar.addView(question);
        LinearLayout voteButtons = new LinearLayout(context);
        voteButtons.setGravity(Gravity.CENTER_VERTICAL);
        addFeedbackButton(voteButtons, "👍 Mi piace", "like");
        addFeedbackButton(voteButtons, "👎 No", "dislike");
        addFeedbackButton(voteButtons, "✨ Altro", "alternative");
        feedbackBar.addView(voteButtons);
        addView(feedbackBar);
        feedbackBar.setVisibility(View.GONE);

        LinearLayout editor = new LinearLayout(context);
        editor.setPadding(dp(12), dp(7), dp(12), dp(11));
        editor.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(15f);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFF8D97AC);
        input.setHint("Chiedi a Fråg una modifica...");
        input.setPadding(dp(13), dp(11), dp(13), dp(11));
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setBackground(rounded(0xFF252D3C, 13));
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit();
                return true;
            }
            return false;
        });
        editor.addView(input, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView send = new TextView(context);
        send.setText("Invia");
        send.setTextColor(Color.WHITE);
        send.setTypeface(null, Typeface.BOLD);
        send.setGravity(Gravity.CENTER);
        send.setBackground(rounded(0xFF4B65CE, 13));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(64), dp(46));
        sendParams.leftMargin = dp(8);
        editor.addView(send, sendParams);
        send.setOnClickListener(v -> submit());
        addView(editor);
    }

    private void addFeedbackButton(LinearLayout row, String label, String value) {
        TextView button = new TextView(getContext());
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12f);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(36));
        button.setPadding(dp(5), dp(6), dp(5), dp(6));
        button.setBackground(rounded(0xFF38574A, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
        button.setOnClickListener(v -> onFeedback.submit(value));
    }

    public void showStyleFeedback(String styleName) {
        question.setText("Fråg: ti piace lo stile «" + styleName + "»?");
        feedbackBar.setVisibility(View.VISIBLE);
    }

    public void dismissStyleFeedback() {
        feedbackBar.setVisibility(View.GONE);
    }

    /** Called on the Android UI thread when a real operation starts or completes. */
    public void logStage(String message) {
        stages.addLast(message);
        while (stages.size() > 5) stages.removeFirst();
        StringBuilder text = new StringBuilder();
        for (String stage : stages) {
            if (text.length() > 0) text.append('\n');
            text.append("• ").append(stage);
        }
        log.setText(text.toString());
        status.setText("● " + message);
    }

    public void restore(String stored) {
        try { history = stored == null ? new JSONArray() : new JSONArray(stored); }
        catch (JSONException ignored) { history = new JSONArray(); }
        messages.removeAllViews();
        if (history.length() == 0) {
            append("Fråg", "Ciao! Sono Fråg 🐸, un motore locale sperimentale. " +
                    "Posso modificare la UI, proporre uno stile e cercare fonti pubbliche. " +
                    "Prova «proponi uno stile» o «cerca online material design».");
        } else {
            for (int i = 0; i < history.length(); i++) {
                JSONObject row = history.optJSONObject(i);
                if (row != null) bubble(row.optString("role"), row.optString("text"));
            }
        }
    }

    public void append(String role, String text) {
        JSONObject row = new JSONObject();
        try {
            row.put("role", role);
            row.put("text", text);
            history.put(row);
            if (history.length() > 80) {
                JSONArray recent = new JSONArray();
                for (int i = history.length() - 80; i < history.length(); i++) recent.put(history.opt(i));
                history = recent;
            }
        } catch (JSONException ignored) {}
        bubble(role, text);
    }

    public String exportHistory() { return history.toString(); }

    private void bubble(String role, String text) {
        boolean user = "Tu".equals(role);
        TextView item = new TextView(getContext());
        item.setText((user ? "Tu" : "🐸 Fråg") + "\n" + text);
        item.setTextSize(14f);
        item.setTextColor(Color.WHITE);
        item.setPadding(dp(12), dp(9), dp(12), dp(9));
        item.setBackground(rounded(user ? 0xFF34498F : 0xFF293344, 13));
        // Source URLs from the research providers remain clickable.
        Linkify.addLinks(item, Linkify.WEB_URLS);
        item.setLinkTextColor(0xFF9AE7FF);
        item.setMovementMethod(LinkMovementMethod.getInstance());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(user ? dp(40) : 0, dp(4), user ? 0 : dp(40), dp(4));
        params.gravity = user ? Gravity.END : Gravity.START;
        messages.addView(item, params);
        scroll.post(() -> scroll.fullScroll(FOCUS_DOWN));
    }

    private void submit() {
        String prompt = input.getText().toString().trim();
        if (prompt.isEmpty()) return;
        input.setText("");
        onMessage.send(prompt);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
