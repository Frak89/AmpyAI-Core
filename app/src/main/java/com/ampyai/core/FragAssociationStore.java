package com.ampyai.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Durable, human-auditable association memory, separate from neural weights. */
public final class FragAssociationStore extends SQLiteOpenHelper {
    public FragAssociationStore(Context ctx) {
        super(ctx,"frag_association_v3.db",null,1);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE concepts (name TEXT PRIMARY KEY, sides INTEGER NOT NULL,"
                +" equal_edges INTEGER NOT NULL, right_angles INTEGER NOT NULL,"
                +" regular INTEGER NOT NULL, closed INTEGER NOT NULL,"
                +" observations INTEGER NOT NULL, program TEXT, last_evidence TEXT,"
                +" updated_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV) {}

    public synchronized FragDrawingEngine.Concept get(String requested) {
        String name=FragDrawingEngine.cleanName(requested);
        try(Cursor c=getReadableDatabase().query("concepts",
             new String[]{"sides","equal_edges","right_angles","regular",
                          "closed","observations"}, "name=?",new String[]{name},
                          null,null,null,"1")) {
            if(!c.moveToFirst())return null;
            FragDrawingEngine.Concept item=new FragDrawingEngine.Concept(name);
            item.sides=c.getInt(0);
            item.equal=c.getInt(1)!=0;item.right=c.getInt(2)!=0;
            item.regular=c.getInt(3)!=0;item.closed=c.getInt(4)!=0;
            item.observations=c.getInt(5);
            return item;
        }
    }

    /** Adds only facts with an identifiable subject. Generates only verified code. */
    public synchronized String learn(String lesson) {
        FragDrawingEngine.Fact fact=FragDrawingEngine.extract(lesson);
        if(!fact.useful())return "";
        FragDrawingEngine.Concept c=get(fact.name);
        if(c==null)c=new FragDrawingEngine.Concept(fact.name);
        boolean expanded=c.merge(fact);
        String program=null;
        String note=FragDrawingEngine.limitation(c);
        if(note.isEmpty()) {
            FragDrawingEngine.Program compiled=FragDrawingEngine.compile(c);
            program=compiled.path();
        }
        ContentValues v=new ContentValues();
        v.put("name",c.name);v.put("sides",c.sides);
        v.put("equal_edges",c.equal?1:0);v.put("right_angles",c.right?1:0);
        v.put("regular",c.regular?1:0);v.put("closed",c.closed?1:0);
        v.put("observations",c.observations);
        if(program!=null)v.put("program",program);else v.putNull("program");
        v.put("last_evidence",fact.evidence);
        v.put("updated_at",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("concepts",null,v,
                SQLiteDatabase.CONFLICT_REPLACE);
        return "🧠 Associazioni aggiornate: "+c.learned()+
                (program==null ? "\nCodice non generato: "+note
                        : "\n✨ Nuova competenza vettoriale generata e verificata: "
                          +"«disegna "+c.name+"». Codice persistente in memoria.");
    }

    public synchronized String program(String name) {
        String clean=FragDrawingEngine.cleanName(name);
        try(Cursor c=getReadableDatabase().query("concepts",
                new String[]{"program"},"name=?",new String[]{clean},
                null,null,null,"1")) {
            return c.moveToFirst()?c.getString(0):null;
        }
    }

    public synchronized int conceptCount() {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM concepts",null)) {
            return c.moveToFirst()?c.getInt(0):0;
        }
    }

    public synchronized int skillCount() {
        try(Cursor c=getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM concepts WHERE program IS NOT NULL",null)) {
            return c.moveToFirst()?c.getInt(0):0;
        }
    }

    public synchronized String overview() {
        StringBuilder b=new StringBuilder("🧠 MEMORIA ASSOCIATIVA"+
                "\nConcetti: "+conceptCount()+" · programmi disegno: "+skillCount());
        try(Cursor c=getReadableDatabase().query("concepts",
                new String[]{"name","sides","equal_edges","right_angles","regular",
                             "observations","program"},null,null,null,null,
                "updated_at DESC","30")) {
            while(c.moveToNext()) {
                b.append("\n• ").append(c.getString(0)).append(": ")
                 .append(c.getInt(1)).append(" lati, ")
                 .append(c.getInt(5)).append(" lezioni, codice ")
                 .append(c.isNull(6)?"incompleto":"verificato");
            }
        }
        return b.toString();
    }
}
