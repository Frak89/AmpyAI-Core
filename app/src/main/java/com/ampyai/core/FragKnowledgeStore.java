package com.ampyai.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Locally indexed, source-attributed snippets; retrieval knowledge, not neural weights. */
public final class FragKnowledgeStore extends SQLiteOpenHelper {
    public FragKnowledgeStore(Context context) {
        super(context, "frag_knowledge.db", null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sources (url TEXT PRIMARY KEY, provider TEXT NOT NULL, "
                + "title TEXT NOT NULL, snippet TEXT NOT NULL, query TEXT NOT NULL, "
                + "fetched INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_sources_query ON sources(query)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade with migrations when the schema changes; never delete knowledge here.
    }

    public void save(String query, FragResearch.Hit hit) {
        ContentValues values = new ContentValues();
        values.put("url", hit.url);
        values.put("provider", hit.provider);
        values.put("title", hit.title);
        values.put("snippet", hit.snippet);
        values.put("query", query.toLowerCase(java.util.Locale.ROOT));
        values.put("fetched", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("sources", null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Returns ONLY actually cached source snippets matching the topic. */
    public List<FragResearch.Hit> findTopic(String topic) {
        List<FragResearch.Hit> found = new ArrayList<>();
        String q=topic.toLowerCase(java.util.Locale.ROOT).trim();
        // Avoid interpreting LIKE wildcards as user controlled patterns.
        q=q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT provider,title,snippet,url FROM sources " +
                "WHERE LOWER(title) LIKE ? ESCAPE '\\' OR LOWER(query) LIKE ? ESCAPE '\\' " +
                "OR LOWER(snippet) LIKE ? ESCAPE '\\' ORDER BY fetched DESC LIMIT 3",
                new String[]{"%"+q+"%","%"+q+"%","%"+q+"%"})) {
            while(cursor.moveToNext()) found.add(new FragResearch.Hit(
                    cursor.getString(0),cursor.getString(1),
                    cursor.getString(2),cursor.getString(3)));
        }
        return found;
    }

    public int sourceCount() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sources", null)) {
            return c.moveToFirst()?c.getInt(0):0;
        }
    }

    public List<FragResearch.Hit> cached(String query) {
        List<FragResearch.Hit> hits = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("sources",
                new String[]{"provider", "title", "snippet", "url"},
                "query = ?", new String[]{query.toLowerCase(java.util.Locale.ROOT)},
                null, null, "fetched DESC", "5")) {
            while (cursor.moveToNext()) {
                hits.add(new FragResearch.Hit(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3)));
            }
        }
        return hits;
    }
}
