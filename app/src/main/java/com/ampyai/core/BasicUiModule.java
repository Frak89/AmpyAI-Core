package com.ampyai.core;

import android.graphics.Color;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First offline designer module.
 * Deliberately deterministic: no LLM or promise of understanding arbitrary language.
 * Replace/add other DevModule implementations without changing the renderer.
 */
public final class BasicUiModule implements DevModule {
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static Matcher match(String input, String regex) {
        Matcher m = Pattern.compile(regex, FLAGS).matcher(input);
        return m.matches() ? m : null;
    }

    @Override
    public Result apply(String request, UiDocument document) {
        String raw = request.trim();
        if (raw.isEmpty()) return new Result(false, "Scrivi una richiesta per modificare lo schermo.");
        String lower = raw.toLowerCase(Locale.ROOT);

        if (lower.equals("aiuto") || lower.equals("help") || lower.equals("?")) {
            return new Result(false, "Esempi:\n• aggiungi titolo Benvenuto\n• crea pulsante Salva\n• aggiungi campo Email\n• aggiungi testo Ciao\n• aggiungi scheda Riepilogo\n• sfondo blu\n• al clic su Salva mostra messaggio Fatto\n• rinomina Salva in Conferma\n• rimuovi Conferma\n• annulla\n• svuota\n• crea schermata login");
        }
        if (lower.matches("^(svuota|cancella tutto|pulisci|reset|azzera)(?:\\s+(?:canvas|schermo|tutto))?$")) {
            document.clear();
            if (lower.startsWith("reset") || lower.startsWith("azzera")) {
                document.setBackground(Color.rgb(16, 19, 26));
            }
            return new Result(true, "Canvas ripulito.");
        }
        if (lower.matches("^(rimuovi|elimina|cancella)\\s+(ultimo|ultima|l'ultimo|l'ultima)(?:\\s+elemento)?$")) {
            return document.removeLast() ? new Result(true, "Ultimo elemento rimosso.")
                    : new Result(false, "Il canvas è già vuoto.");
        }
        Matcher m = match(raw, "^(?:sfondo|background|colore\\s+(?:di\\s+)?sfondo|cambia\\s+(?:lo\\s+)?sfondo(?:\\s+in)?|imposta\\s+(?:lo\\s+)?sfondo(?:\\s+su)?)\\s+(.+)$");
        if (m != null) {
            Integer color = parseColor(m.group(1));
            if (color == null) return new Result(false, "Colore non riconosciuto. Prova nero, bianco, blu, rosso, viola o #RRGGBB.");
            document.setBackground(color);
            return new Result(true, "Sfondo aggiornato.");
        }

        m = match(raw, "^(?:al\\s+clic\\s+su|al\\s+click\\s+su|quando\\s+(?:clicco|premo)\\s+(?:su\\s+)?)\\s*(?:il\\s+)?(?:pulsante\\s+)?(.+?)\\s+(?:mostra|visualizza|fai\\s+apparire)\\s+(?:il\\s+)?(?:messaggio\\s+)?(.+)$");
        if (m != null) {
            return document.setButtonMessage(m.group(1), m.group(2))
                    ? new Result(true, "Azione di clic collegata al pulsante \"" + m.group(1) + "\".")
                    : new Result(false, "Non trovo un pulsante chiamato \"" + m.group(1) + "\".");
        }

        m = match(raw, "^(?:rinomina|modifica\\s+(?:testo|etichetta)\\s+di)\\s+(.+?)\\s+(?:in|con)\\s+(.+)$");
        if (m != null) {
            return document.rename(m.group(1), m.group(2))
                    ? new Result(true, "Etichetta aggiornata in tempo reale.")
                    : new Result(false, "Elemento da rinominare non trovato.");
        }
        m = match(raw, "^(?:rimuovi|elimina|cancella)\\s+(.+)$");
        if (m != null) {
            String title = m.group(1).replaceFirst("(?i)^(?:il|la|lo|un|una)\\s+", "")
                    .replaceFirst("(?i)^(?:pulsante|bottone|testo|titolo|campo|scheda)\\s+", "");
            return document.removeByTitle(title)
                    ? new Result(true, "Elemento \"" + title + "\" rimosso.")
                    : new Result(false, "Non trovo \"" + title + "\" nel canvas.");
        }

        if (lower.matches("^(?:crea|fammi|genera|mostra|aggiungi)(?:\\s+(?:una|un|la|il))?\\s+(?:schermata|pagina|interfaccia|ui)(?:\\s+di)?\\s+login.*$")
                || lower.equals("login")) {
            document.clear();
            document.add("title", "Accesso");
            document.add("text", "Accedi al tuo account");
            document.add("input", "Email");
            document.add("input", "Password");
            document.add("button", "Accedi");
            document.setButtonMessage("Accedi", "Pulsante Accedi premuto");
            return new Result(true, "Template login generato. Puoi modificarlo con altri comandi.");
        }
        if (lower.matches("^(?:crea|fammi|genera|mostra|aggiungi)(?:\\s+(?:una|un|la|il))?\\s+(?:schermata|pagina|interfaccia|ui)?\\s*dashboard.*$")) {
            document.clear();
            document.add("title", "Dashboard");
            document.add("card", "Riepilogo");
            document.add("button", "Nuovo elemento");
            return new Result(true, "Template dashboard creato.");
        }

        m = match(raw, "^(?:aggiungi|crea|inserisci|metti|fammi|mostra|genera)\\s+(?:(?:un|una|il|la)\\s+)?(titolo|testo|pulsante|bottone|button|campo|input|scheda|card|separatore|linea)\\s*(.*)$");
        if (m != null) {
            String type = m.group(1).toLowerCase(Locale.ROOT);
            String title = m.group(2).trim().replaceFirst("(?i)^(?::|chiamat[oa]\\s+|con\\s+scritto\\s+|di\\s+nome\\s+)", "").trim();
            if (type.equals("bottone") || type.equals("button") || type.equals("pulsante")) type = "button";
            if (type.equals("campo") || type.equals("input")) type = "input";
            if (type.equals("scheda") || type.equals("card")) type = "card";
            if (type.equals("linea") || type.equals("separatore")) type = "divider";
            if (type.equals("divider")) title = "";
            if (title.isEmpty() && !type.equals("divider")) {
                return new Result(false, "Quale testo devo visualizzare? Esempio: crea pulsante Conferma.");
            }
            document.add(type, title);
            return new Result(true, "Aggiunto " + type + (title.isEmpty() ? "" : ": " + title) + ". Lo vedi già sul canvas.");
        }

        return null;
    }

    private static Integer parseColor(String text) {
        String name = text.trim().toLowerCase(Locale.ROOT).replace("colore ", "");
        switch (name) {
            case "nero": case "black": return Color.rgb(16, 19, 26);
            case "bianco": case "white": return Color.WHITE;
            case "blu": case "blue": return Color.rgb(19, 44, 94);
            case "rosso": case "red": return Color.rgb(116, 35, 47);
            case "verde": case "green": return Color.rgb(15, 89, 67);
            case "viola": case "purple": return Color.rgb(72, 39, 109);
            case "grigio": case "gray": return Color.rgb(62, 67, 77);
            case "rosa": case "pink": return Color.rgb(135, 49, 96);
            case "arancione": case "orange": return Color.rgb(145, 69, 24);
            default:
                if (name.matches("#[0-9a-f]{6}")) {
                    try { return Color.parseColor(name); } catch (IllegalArgumentException ignored) {}
                }
                return null;
        }
    }
}
