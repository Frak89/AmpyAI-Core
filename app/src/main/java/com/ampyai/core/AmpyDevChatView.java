package com.ampyai.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Compact offline chat panel that never blocks immediate canvas rendering. */
public final class AmpyDevChatView extends LinearLayout {
    public interface OnMessage { void send(String prompt); }
    private final OnMessage onMessage;
    private final ScrollView scroll;
    private final LinearLayout messages;
    private final EditText input;
    private JSONArray history = new JSONArray();

    public AmpyDevChatView(Context context, OnMessage onMessage, Runnable onClose) {
        super(context);
        this.onMessage = onMessage;
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF171C27);
        setElevation(dp(12));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(7), dp(12), dp(7));
        TextView name = new TextView(context);
        name.setText("✦  AmpyDev  ·  LIVE UI");
        name.setTextColor(0xFFA9B9FF);
        name.setTextSize(15f);
        name.setTypeface(null, Typeface.BOLD);
        header.addView(name, new LinearLayout.LayoutParams(0, dp(38), 1f));
        name.setGravity(Gravity.CENTER_VERTICAL);
        TextView close = new TextView(context);
        close.setText("✕");
        close.setContentDescription("Chiudi AmpyDev");
        close.setTextSize(21f);
        close.setTextColor(Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> onClose.run());
        header.addView(close, new LinearLayout.LayoutParams(dp(40), dp(38)));
        addView(header);

        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        messages = new LinearLayout(context);
        messages.setOrientation(VERTICAL);
        messages.setPadding(dp(12), dp(5), dp(12), dp(9));
        scroll.addView(messages);
        addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout editor = new LinearLayout(context);
        editor.setPadding(dp(12), dp(7), dp(12), dp(11));
        editor.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(15f);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFF8D97AC);
        input.setHint("Descrivi una modifica UI...");
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

    public void restore(String stored) {
        try { history = stored == null ? new JSONArray() : new JSONArray(stored); }
        catch (JSONException ignored) { history = new JSONArray(); }
        messages.removeAllViews();
        if (history.length() == 0) {
            append("AmpyDev", "Canvas pronto. Prova: crea pulsante Salva oppure scrivi aiuto.");
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
        item.setText((user ? "Tu" : "AmpyDev") + "\n" + text);
        item.setTextSize(14f);
        item.setTextColor(Color.WHITE);
        item.setPadding(dp(12), dp(9), dp(12), dp(9));
        item.setBackground(rounded(user ? 0xFF34498F : 0xFF293344, 13));
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
