package com.ampyai.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Knowledge != weights: word definitions remain a sourced SQLite record;
 * the language model stores statistical weights separately.
 */
public final class FragLanguageMemory extends SQLiteOpenHelper {
    private final File path;
    private static final int MAGIC=0x4C465233; // LFR3
    private static final int VERSION=1;
    public FragLanguageMemory(Context ctx) {
        super(ctx,"frag_language_v3.db",null,1);
        path=new File(ctx.getFilesDir(),"frag_language_core_v3.bin");
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE definitions (term TEXT PRIMARY KEY COLLATE NOCASE,"
                +" meaning TEXT NOT NULL, source TEXT NOT NULL, retrieved INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE language_text (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                +" content TEXT NOT NULL, origin TEXT NOT NULL, timestamp INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int a,int b) {}

    public synchronized void saveDefinition(String term,String meaning,String source) {
        ContentValues v=new ContentValues();
        v.put("term",term.toLowerCase(java.util.Locale.ITALIAN));
        v.put("meaning",meaning);
        v.put("source",source);
        v.put("retrieved",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("definitions",null,v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }
    public synchronized String[] definition(String word) {
        try(Cursor c=getReadableDatabase().query("definitions",
                new String[]{"meaning","source"},"term=?",
                new String[]{word.toLowerCase(java.util.Locale.ITALIAN)},
                null,null,null,"1")) {
            return c.moveToFirst() ? new String[]{c.getString(0),c.getString(1)} : null;
        }
    }
    public synchronized int definitions() {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM definitions",null)) {
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
    public synchronized void saveText(String content,String origin) {
        ContentValues v=new ContentValues();
        v.put("content",content);
        v.put("origin",origin);
        v.put("timestamp",System.currentTimeMillis());
        getWritableDatabase().insert("language_text",null,v);
    }
    public synchronized int texts() {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM language_text",null)) {
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
    public synchronized String corpus(int charLimit) {
        StringBuilder sb=new StringBuilder();
        try(Cursor c=getReadableDatabase().query("language_text",
                new String[]{"content"},null,null,null,null,"id DESC","600")) {
            while(c.moveToNext()) {
                String t=c.getString(0);
                if(sb.length()+t.length()>charLimit) break;
                sb.append(t).append('\n');
            }
        }
        return sb.toString();
    }
    public synchronized void saveModel(FragLanguageModel.Model m) throws IOException {
        File tmp=new File(path.getParentFile(),path.getName()+".tmp");
        try(DataOutputStream out=new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmp)))) {
            out.writeInt(MAGIC);out.writeInt(VERSION);
            out.writeInt(FragLanguageModel.VOCAB);
            out.writeInt(FragLanguageModel.CONTEXT);
            out.writeInt(FragLanguageModel.HIDDEN);
            out.writeLong(m.charactersSeen);
            out.writeInt(m.sessions);
            out.writeFloat(m.lastLoss);
            for(float[] row:m.input) for(float w:row) out.writeFloat(w);
            for(float w:m.bias)out.writeFloat(w);
            for(float[] row:m.output)for(float w:row)out.writeFloat(w);
            for(float w:m.outputBias)out.writeFloat(w);
        }
        if(!tmp.renameTo(path)) {
            if(path.exists()&&!path.delete())throw new IOException("Pesi precedenti bloccati.");
            if(!tmp.renameTo(path))throw new IOException("Salvataggio pesi fallito.");
        }
    }
    public synchronized FragLanguageModel.Model loadModel() throws IOException {
        if(!path.exists())return null;
        try(DataInputStream in=new DataInputStream(
                new BufferedInputStream(new FileInputStream(path)))) {
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION||
               in.readInt()!=FragLanguageModel.VOCAB||
               in.readInt()!=FragLanguageModel.CONTEXT||
               in.readInt()!=FragLanguageModel.HIDDEN)
                throw new IOException("Formato Language Core incompatibile.");
            FragLanguageModel.Model m=new FragLanguageModel.Model();
            m.charactersSeen=in.readLong();
            m.sessions=in.readInt();
            m.lastLoss=in.readFloat();
            for(float[] row:m.input)for(int i=0;i<row.length;i++)row[i]=in.readFloat();
            for(int i=0;i<m.bias.length;i++)m.bias[i]=in.readFloat();
            for(float[] row:m.output)for(int i=0;i<row.length;i++)row[i]=in.readFloat();
            for(int i=0;i<m.outputBias.length;i++)m.outputBias[i]=in.readFloat();
            return m;
        }
    }
}
