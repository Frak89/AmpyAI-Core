package com.ampyai.core;

import android.graphics.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The first original Fråg visual knowledge module: a versioned collection of
 * locally defined palettes. No online model or inference dependency.
 */
public final class FragStyle {
    public final String id;
    public final String label;
    public final String description;
    public final int background;
    public final int surface;
    public final int accent;
    public final int foreground;
    public final int muted;
    public final int stroke;
    public final int radius;

    private FragStyle(String id, String label, String description, String background,
                      String surface, String accent, String foreground, String muted,
                      String stroke, int radius) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.background = Color.parseColor(background);
        this.surface = Color.parseColor(surface);
        this.accent = Color.parseColor(accent);
        this.foreground = Color.parseColor(foreground);
        this.muted = Color.parseColor(muted);
        this.stroke = Color.parseColor(stroke);
        this.radius = radius;
    }

    public static final FragStyle DEFAULT = new FragStyle("default", "Dark",
        "Neutro, adatto a partire da uno schermo vuoto.",
        "#10131A", "#252B36", "#4B65CE", "#FFFFFF", "#9CA6B8", "#424958", 13);
    public static final FragStyle CYBERPUNK = new FragStyle("cyberpunk", "Cyberpunk",
        "Contrasti scuri, neon viola e accenti magenta.",
        "#100B20", "#28203B", "#BC42D9", "#F8EEFF", "#C2AECF", "#725084", 6);
    public static final FragStyle MINIMAL = new FragStyle("minimal", "Minimal",
        "Spazio chiaro, linee sottili e tipografia pulita.",
        "#F7F8FB", "#FFFFFF", "#3D57CC", "#152038", "#647085", "#DCE1E9", 18);
    public static final FragStyle SCIFI = new FragStyle("scifi", "Sci-Fi",
        "Pannelli spaziali blu notte e dettagli ciano.",
        "#091823", "#153344", "#00AFC3", "#F0FBFF", "#9BC5CE", "#377080", 3);
    public static final FragStyle RETRO = new FragStyle("retro", "Retro",
        "Atmosfera vintage, toni caldi e dettagli ambrati.",
        "#2A211D", "#443329", "#CE944B", "#FFF3DF", "#C6B29A", "#765C43", 12);
    public static final FragStyle ORGANIC = new FragStyle("organic", "Organic",
        "Tonalità naturali, superfici morbide e curve generose.",
        "#101E19", "#203A31", "#64B598", "#F0FFF6", "#A4C9B8", "#386857", 24);

    public static final List<FragStyle> PROPOSALS =
        Arrays.asList(CYBERPUNK, MINIMAL, SCIFI, RETRO, ORGANIC);

    public static FragStyle byId(String name) {
        if (name == null) return DEFAULT;
        String key = name.toLowerCase(Locale.ROOT).trim();
        for (FragStyle style : PROPOSALS) {
            if (style.id.equals(key)) return style;
        }
        return DEFAULT;
    }

    public static FragStyle mentionedIn(String input) {
        String v = input.toLowerCase(Locale.ROOT);
        if (v.contains("cyberpunk") || v.contains("neon") || v.contains("tokyo 2077")) return CYBERPUNK;
        if (v.contains("minimal") || v.contains("pulito") || v.contains("essenziale")) return MINIMAL;
        if (v.contains("sci-fi") || v.contains("sci fi") || v.contains("fantascien")
                || v.contains("futurist") || v.contains("spaziale")) return SCIFI;
        if (v.contains("retro") || v.contains("vintage") || v.contains("anni 90")) return RETRO;
        if (v.contains("organic") || v.contains("organico") || v.contains("naturale")) return ORGANIC;
        return null;
    }

    public void previewIn(UiDocument scene) {
        scene.setStyle(id);
        scene.setBackground(background);
        if (scene.nodes().length() == 0) {
            scene.add("title", "Anteprima " + label);
            scene.add("text", description);
            scene.add("card", "Scheda di esempio");
            scene.add("button", "Prova pulsante");
        }
    }
}
