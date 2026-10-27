package com.ampyai.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Persistent, local examples and binary neural weights. No network or cloud. */
public final class FragTrainingStore extends SQLiteOpenHelper {
    private final File weights;
    private static final int MAGIC = 0x46524147; // FRAG
    private static final int FORMAT = 1;
    public FragTrainingStore(Context context) {
        super(context, "frag_training_v2.db", null, 1);
        weights = new File(context.getFilesDir(), "frag_ui_model_v2.bin");
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE examples (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT NOT NULL, label TEXT NOT NULL, origin TEXT NOT NULL, " +
                "UNIQUE(text,label))");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // Future migration must preserve examples.
    }

    public synchronized boolean add(String text, String rawLabel, String origin) {
        String clean = text.trim();
        int label = FragNeuralTrainer.label(rawLabel);
        if (label < 0 || clean.length() < 3 || clean.length() > 250) return false;
        ContentValues values = new ContentValues();
        values.put("text", clean);
        values.put("label", FragNeuralTrainer.LABELS[label]);
        values.put("origin", origin);
        return getWritableDatabase().insertWithOnConflict("examples", null, values,
                SQLiteDatabase.CONFLICT_IGNORE) != -1L;
    }

    public synchronized int size() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM examples", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public synchronized List<FragNeuralTrainer.Example> examples() {
        List<FragNeuralTrainer.Example> output = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("examples",
                new String[]{"text", "label"}, null, null, null, null, "id ASC", "5000")) {
            while (c.moveToNext()) {
                int label = FragNeuralTrainer.label(c.getString(1));
                if (label >= 0) output.add(new FragNeuralTrainer.Example(c.getString(0), label));
            }
        }
        return output;
    }

    public int seed() {
        String[][] examples = {
            {"button", "crea un pulsante Salva", "metti bottone Conferma",
             "voglio un tasto per continuare", "inserisci un pulsante Esci",
             "aggiungi un bottone con scritto Apri", "visualizza tasto avanti",
             "mostra un pulsante per riprovare", "mi serve un bottone con testo Invia",
             "metti un comando cliccabile per salvare", "aggiungi tasto Accetta"},
            {"title", "aggiungi un titolo Benvenuto", "crea un'intestazione principale",
             "voglio un titolo in alto", "metti una testata con scritto Home",
             "inserisci il titolo Profilo", "crea un heading grande",
             "mostra intestazione Accesso", "scrivi un titolo per la pagina",
             "imposta un header Contatti", "aggiungi un titolo Dashboard"},
            {"text", "scrivi un testo informativo", "aggiungi testo Ciao",
             "inserisci una frase descrittiva", "mostra un paragrafo di testo",
             "metti una scritta di benvenuto", "inserisci una etichetta leggibile",
             "aggiungi una descrizione sotto", "visualizza testo di aiuto",
             "scrivi una nota sulla schermata", "metti testo Informazioni"},
            {"input", "crea un campo Email", "inserisci una casella di testo",
             "voglio un input per nome", "metti un campo da compilare",
             "aggiungi una casella per la password", "visualizza campo di ricerca",
             "crea input Cognome", "inserisci campo Telefono",
             "aggiungi un campo editabile", "metti form con inserimento testo"},
            {"card", "crea una scheda riepilogo", "aggiungi una card",
             "metti un riquadro di informazioni", "voglio un pannello rettangolare",
             "inserisci una scheda statistiche", "crea card prodotti",
             "aggiungi un contenitore per notizie", "mostra una scheda del profilo",
             "metti riquadro con titolo Spese", "crea pannello risultati"},
            {"login", "crea schermata login", "progetta pagina di accesso",
             "fammi una pagina per entrare con email e password",
             "schermata per accedere all'account", "voglio un modulo di login",
             "prepara la pagina di autenticazione", "crea form di accesso completo",
             "costruisci pagina per fare login", "interfaccia per inserire le credenziali",
             "genera un accesso utente"},
            {"dashboard", "crea una dashboard", "voglio un cruscotto riepilogativo",
             "realizza la pagina principale con statistiche",
             "mostra pannello di controllo", "fammi una dashboard con schede",
             "prepara una home con riepilogo", "interfaccia di monitoraggio generale",
             "genera un cruscotto di controllo", "schermata dashboard dell'app",
             "costruisci pagina riepilogativa"}
        };
        int inserted = 0;
        for (String[] group : examples) {
            for (int i = 1; i < group.length; i++)
                if (add(group[i], group[0], "starter")) inserted++;
        }
        return inserted;
    }

    /** JSON array or JSON Lines. Max 5,000 examples / import (enforced by caller). */
    public int importExamples(String content) throws Exception {
        List<JSONObject> rows = new ArrayList<>();
        if (content.trim().startsWith("[")) {
            JSONArray json = new JSONArray(content);
            if (json.length() > 5000) throw new IOException("Troppi esempi (max 5000).");
            for (int i = 0; i < json.length(); i++) {
                JSONObject row = json.optJSONObject(i);
                if (row != null) rows.add(row);
            }
        } else {
            String[] lines = content.split("\\R");
            if (lines.length > 5000) throw new IOException("Troppi esempi (max 5000).");
            for (String line : lines) {
                if (!line.trim().isEmpty()) rows.add(new JSONObject(line));
            }
        }
        int inserted = 0;
        for (JSONObject row : rows) {
            if (add(row.optString("text"), row.optString("label"), "import")) inserted++;
        }
        return inserted;
    }

    public synchronized void saveModel(FragNeuralTrainer.Model m) throws IOException {
        File temp = new File(weights.getParentFile(), weights.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(temp)))) {
            out.writeInt(MAGIC);
            out.writeInt(FORMAT);
            out.writeInt(FragNeuralTrainer.FEATURES);
            out.writeInt(FragNeuralTrainer.HIDDEN);
            out.writeInt(FragNeuralTrainer.LABELS.length);
            out.writeInt(m.examples);
            out.writeInt(m.epochs);
            out.writeDouble(m.validationAccuracy);
            for (float[] row : m.hidden) for (float v : row) out.writeFloat(v);
            for (float v : m.hiddenBias) out.writeFloat(v);
            for (float[] row : m.output) for (float v : row) out.writeFloat(v);
            for (float v : m.outputBias) out.writeFloat(v);
        }
        if (!temp.renameTo(weights)) {
            // Some file systems require deleting the old file before renaming.
            if (weights.exists() && !weights.delete())
                throw new IOException("Non posso sostituire i pesi precedenti.");
            if (!temp.renameTo(weights)) throw new IOException("Non posso salvare i nuovi pesi.");
        }
    }

    public synchronized FragNeuralTrainer.Model loadModel() throws IOException {
        if (!weights.exists()) return null;
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(weights)))) {
            if (in.readInt() != MAGIC || in.readInt() != FORMAT ||
                in.readInt() != FragNeuralTrainer.FEATURES ||
                in.readInt() != FragNeuralTrainer.HIDDEN ||
                in.readInt() != FragNeuralTrainer.LABELS.length)
                throw new IOException("Formato pesi non valido.");
            FragNeuralTrainer.Model model = new FragNeuralTrainer.Model();
            model.examples = in.readInt();
            model.epochs = in.readInt();
            model.validationAccuracy = in.readDouble();
            for (float[] row : model.hidden)
                for (int j = 0; j < row.length; j++) row[j] = in.readFloat();
            for (int j = 0; j < model.hiddenBias.length; j++)
                model.hiddenBias[j] = in.readFloat();
            for (float[] row : model.output)
                for (int j = 0; j < row.length; j++) row[j] = in.readFloat();
            for (int j = 0; j < model.outputBias.length; j++)
                model.outputBias[j] = in.readFloat();
            return model;
        }
    }
}
