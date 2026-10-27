package com.ampyai.core;

import android.content.SharedPreferences;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Original on-device symbolic decision engine: routes intents and adjusts style
 * rankings from explicit user feedback. No pretrained neural model is involved.
 */
public final class FragBrain {
    public enum Kind { SEARCH, STYLE, HELP, NORMAL }
    public static final class Decision {
        public final Kind kind;
        public final String argument;
        public final FragStyle style;
        private Decision(Kind kind, String argument, FragStyle style) {
            this.kind = kind;
            this.argument = argument;
            this.style = style;
        }
    }

    private final SharedPreferences memory;
    public FragBrain(SharedPreferences memory) { this.memory = memory; }

    public Decision classify(String prompt) {
        String raw = prompt.trim();
        String low = raw.toLowerCase(Locale.ROOT);
        Matcher m = Pattern.compile("^(?:fråg[,:]?\\s*)?(?:cerca(?:\\s+online|\\s+sul\\s+web|\\s+su\\s+internet|\\s+su\\s+github)?|ricerca(?:\\s+online)?|trova\\s+(?:sul\\s+web|online)|fai\\s+una\\s+ricerca\\s+(?:su|di))\\s+(.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(raw);
        if (m.matches()) {
            return new Decision(Kind.SEARCH, m.group(1).trim(), null);
        }
        if (low.equals("fråg") || low.equals("chi sei") || low.equals("cosa sai fare")) {
            return new Decision(Kind.HELP, "", null);
        }
        FragStyle explicit = FragStyle.mentionedIn(raw);
        boolean styles = low.contains("stile") || low.contains("palette")
                || low.contains("look") || low.contains("tema")
                || low.contains("grafica") || low.contains("design");
        boolean proposal = low.contains("consigli") || low.contains("suggerisc")
                || low.contains("propon") || low.contains("sorprend")
                || low.contains("quale") || low.contains("idee");
        boolean request = low.matches(".*\\b(crea|fammi|genera|applica|cambia|metti|prova|voglio|usa|mostra)\\b.*");
        if ((styles && (proposal || request || explicit != null))
                || (explicit != null && request)
                || (low.contains("suggerisc") && low.contains("ui"))) {
            return new Decision(Kind.STYLE, raw, explicit);
        }
        return new Decision(Kind.NORMAL, raw, null);
    }

    public FragStyle propose(FragStyle explicit, String exceptId) {
        if (explicit != null && !explicit.id.equals(exceptId)) return explicit;
        FragStyle best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        List<FragStyle> proposals = FragStyle.PROPOSALS;
        for (int i = 0; i < proposals.size(); i++) {
            FragStyle candidate = proposals.get(i);
            if (candidate.id.equals(exceptId)) continue;
            // User votes change real persistent ranking; initial biases only break ties.
            int up = memory.getInt("frag_like_" + candidate.id, 0);
            int down = memory.getInt("frag_dislike_" + candidate.id, 0);
            double rank = (up * 3.0) - (down * 4.0) + (proposals.size() - i) * 0.01;
            if (rank > bestValue) {
                bestValue = rank;
                best = candidate;
            }
        }
        return best == null ? FragStyle.CYBERPUNK : best;
    }

    public void remember(FragStyle style, boolean liked) {
        String key = (liked ? "frag_like_" : "frag_dislike_") + style.id;
        memory.edit().putInt(key, memory.getInt(key, 0) + 1).apply();
        if (liked) memory.edit().putString("frag_favorite", style.id).apply();
    }

    public String preferenceExplanation(FragStyle style) {
        int likes = memory.getInt("frag_like_" + style.id, 0);
        int dislikes = memory.getInt("frag_dislike_" + style.id, 0);
        if (likes + dislikes == 0) return "Prima proposta: dimmi se ti piace e aggiornerò le preferenze locali.";
        return "Memoria locale: " + likes + " approvazioni e " + dislikes
                + " rifiuti per " + style.label + ".";
    }
}
