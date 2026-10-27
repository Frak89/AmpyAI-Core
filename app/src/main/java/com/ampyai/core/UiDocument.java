package com.ampyai.core;

import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

/** Minimal versioned JSON scene graph, intentionally independent of the visual renderer. */
public final class UiDocument {
    private final JSONObject data;

    private UiDocument(JSONObject data) {
        this.data = data;
        if (!data.has("nodes")) {
            try { data.put("nodes", new JSONArray()); } catch (JSONException ignored) {}
        }
    }

    public static UiDocument empty() {
        JSONObject root = new JSONObject();
        try {
            root.put("schema", 1);
            root.put("background", "#10131A");
            root.put("nodes", new JSONArray());
        } catch (JSONException ignored) {}
        return new UiDocument(root);
    }

    public static UiDocument parse(String value) {
        if (value == null || value.isEmpty()) return empty();
        try {
            JSONObject root = new JSONObject(value);
            if (root.optInt("schema", 0) != 1 || !(root.opt("nodes") instanceof JSONArray)) {
                return empty();
            }
            return new UiDocument(root);
        } catch (JSONException e) {
            return empty();
        }
    }

    public JSONArray nodes() {
        return data.optJSONArray("nodes");
    }

    public int background() {
        try { return Color.parseColor(data.optString("background", "#10131A")); }
        catch (IllegalArgumentException e) { return Color.rgb(16, 19, 26); }
    }

    public String style() {
        return data.optString("style", "default");
    }

    public void setStyle(String id) {
        try { data.put("style", id); } catch (JSONException ignored) {}
    }

    public void setBackground(int color) {
        String hex = String.format(Locale.US, "#%06X", color & 0xFFFFFF);
        try { data.put("background", hex); } catch (JSONException ignored) {}
    }

    public void add(String type, String title) {
        JSONObject element = new JSONObject();
        try {
            element.put("id", System.currentTimeMillis() + "-" + nodes().length());
            element.put("type", type);
            element.put("title", title);
            nodes().put(element);
        } catch (JSONException ignored) {}
    }

    /** A generated verified M/L/Z vector program, not script or bytecode. */
    public void addVector(String title,String program,int sideDp) {
        JSONObject item=new JSONObject();
        try {
            item.put("id",System.currentTimeMillis()+"-"+nodes().length());
            item.put("type","vector");
            item.put("title",title);
            item.put("program",program);
            item.put("sideDp",sideDp);
            nodes().put(item);
        } catch(JSONException e) {
            throw new IllegalStateException("Impossibile serializzare il programma.",e);
        }
    }

    public boolean removeLast() {
        JSONArray items = nodes();
        if (items.length() == 0) return false;
        items.remove(items.length() - 1);
        return true;
    }

    public void clear() {
        try { data.put("nodes", new JSONArray()); } catch (JSONException ignored) {}
    }

    public boolean removeByTitle(String title) {
        JSONArray items = nodes();
        for (int i = items.length() - 1; i >= 0; i--) {
            JSONObject item = items.optJSONObject(i);
            if (item != null && item.optString("title").equalsIgnoreCase(title.trim())) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean rename(String before, String after) {
        for (int i = 0; i < nodes().length(); i++) {
            JSONObject item = nodes().optJSONObject(i);
            if (item != null && item.optString("title").equalsIgnoreCase(before.trim())) {
                try { item.put("title", after.trim()); } catch (JSONException ignored) {}
                return true;
            }
        }
        return false;
    }

    public boolean setButtonMessage(String title, String message) {
        for (int i = 0; i < nodes().length(); i++) {
            JSONObject item = nodes().optJSONObject(i);
            if (item != null && item.optString("type").equals("button")
                    && item.optString("title").equalsIgnoreCase(title.trim())) {
                try { item.put("action", "toast"); item.put("message", message); }
                catch (JSONException ignored) {}
                return true;
            }
        }
        return false;
    }

    public String encode() {
        return data.toString();
    }
}
