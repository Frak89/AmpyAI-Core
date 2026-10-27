package com.ampyai.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.function.Consumer;

/** Renders a declarative JSON scene; Android widgets stay fully functional. */
public final class AmpyCanvasView extends ScrollView {
    private final LinearLayout elements;
    private final Consumer<JSONObject> onTapped;
    private final Runnable onEdited;
    private boolean brightBackground;
    private FragStyle style = FragStyle.DEFAULT;

    public AmpyCanvasView(Context context, Consumer<JSONObject> onTapped, Runnable onEdited) {
        super(context);
        this.onTapped = onTapped;
        this.onEdited = onEdited;
        setFillViewport(true);
        setVerticalScrollBarEnabled(false);
        elements = new LinearLayout(context);
        elements.setOrientation(LinearLayout.VERTICAL);
        elements.setPadding(dp(18), dp(18), dp(18), dp(18));
        addView(elements, new ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void render(UiDocument document) {
        style = FragStyle.byId(document.style());
        int color = document.background();
        brightBackground = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000 > 170;
        setBackgroundColor(color);
        elements.removeAllViews();
        for (int i = 0; i < document.nodes().length(); i++) {
            JSONObject item = document.nodes().optJSONObject(i);
            if (item != null) addElement(item);
        }
        post(() -> fullScroll(FOCUS_DOWN));
    }

    private void addElement(JSONObject item) {
        String kind = item.optString("type", "text");
        String title = item.optString("title", "");
        View view;
        int height = LayoutParams.WRAP_CONTENT;
        switch (kind) {
            case "input": {
                EditText input = new EditText(getContext());
                input.setSingleLine(true);
                input.setHint(title);
                input.setHintTextColor(style.muted);
                input.setTextColor(style.foreground);
                input.setTextSize(16f);
                input.setPadding(dp(16), dp(12), dp(16), dp(12));
                input.setBackground(rounded(style.surface, style.radius, style.stroke));
                input.setText(item.optString("value", ""));
                input.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int st, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) {
                        try { item.put("value", s.toString()); } catch (JSONException ignored) {}
                        onEdited.run();
                    }
                });
                view = input;
                break;
            }
            case "button": {
                TextView button = label(title, 16, style.id.equals("minimal") ? 0xFFFFFFFF : style.foreground, Typeface.BOLD);
                button.setGravity(Gravity.CENTER);
                button.setMinHeight(dp(52));
                button.setPadding(dp(16), dp(13), dp(16), dp(13));
                button.setBackground(rounded(style.accent, style.radius, style.stroke));
                button.setClickable(true);
                button.setOnClickListener(v -> onTapped.accept(item));
                view = button;
                break;
            }
            case "title": {
                TextView heading = label(title, 29, style.foreground, Typeface.BOLD);
                heading.setPadding(0, dp(4), 0, dp(10));
                heading.setOnClickListener(v -> onTapped.accept(item));
                view = heading;
                break;
            }
            case "card": {
                TextView card = label(title, 18, style.foreground, Typeface.NORMAL);
                card.setPadding(dp(18), dp(22), dp(18), dp(22));
                card.setMinHeight(dp(92));
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setBackground(rounded(style.surface, style.radius, style.stroke));
                card.setOnClickListener(v -> onTapped.accept(item));
                view = card;
                break;
            }
            case "divider": {
                View line = new View(getContext());
                line.setBackgroundColor(style.stroke);
                view = line;
                height = dp(1);
                break;
            }
            default: {
                TextView text = label(title, 17, style.foreground, Typeface.NORMAL);
                text.setPadding(0, dp(7), 0, dp(7));
                text.setOnClickListener(v -> onTapped.accept(item));
                view = text;
            }
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, height);
        params.setMargins(0, dp(7), 0, dp(7));
        elements.addView(view, params);
    }

    private TextView label(String text, int size, int color, int style) {
        TextView label = new TextView(getContext());
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setTypeface(Typeface.DEFAULT, style);
        return label;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
