package com.bayarkuy.notifier;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Database lokal: antrean pengiriman (agar tidak ada notifikasi hilang saat offline) dan log.
 */
public class Store extends SQLiteOpenHelper {

    public static class Item {
        public long id;
        public String payload;
        public int tries;
        public long logId;
    }

    public static class Log {
        public long id;
        public long ts;
        public String app;
        public String title;
        public String text;
        public long amount;
        public String direction;
        public String status;
        public String detail;
    }

    private static Store instance;

    public static synchronized Store get(Context ctx) {
        if (instance == null) {
            instance = new Store(ctx.getApplicationContext());
        }
        return instance;
    }

    private Store(Context ctx) {
        super(ctx, "notifier.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE queue (id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL, tries INTEGER NOT NULL DEFAULT 0, next_at INTEGER NOT NULL DEFAULT 0, log_id INTEGER NOT NULL DEFAULT 0, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE logs (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, app TEXT, title TEXT, text TEXT, amount INTEGER NOT NULL DEFAULT 0, direction TEXT, status TEXT, detail TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS queue");
        db.execSQL("DROP TABLE IF EXISTS logs");
        onCreate(db);
    }

    /* ---------------- antrean ---------------- */

    public synchronized long enqueue(String payload, long logId) {
        ContentValues v = new ContentValues();
        v.put("payload", payload);
        v.put("tries", 0);
        v.put("next_at", 0);
        v.put("log_id", logId);
        v.put("created", System.currentTimeMillis());
        return getWritableDatabase().insert("queue", null, v);
    }

    public synchronized Item nextDue() {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id, payload, tries, log_id FROM queue WHERE next_at <= ? ORDER BY id ASC LIMIT 1",
            new String[]{String.valueOf(System.currentTimeMillis())});
        try {
            if (c.moveToFirst()) {
                Item it = new Item();
                it.id = c.getLong(0);
                it.payload = c.getString(1);
                it.tries = c.getInt(2);
                it.logId = c.getLong(3);
                return it;
            }
        } finally {
            c.close();
        }
        return null;
    }

    public synchronized void deleteItem(long id) {
        getWritableDatabase().delete("queue", "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized void retryLater(long id, int tries, long delayMs) {
        ContentValues v = new ContentValues();
        v.put("tries", tries);
        v.put("next_at", System.currentTimeMillis() + delayMs);
        getWritableDatabase().update("queue", v, "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized int queueSize() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM queue", null);
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 0;
    }

    public synchronized void clearQueue() {
        getWritableDatabase().delete("queue", null, null);
    }

    /* ---------------- log ---------------- */

    public synchronized long addLog(String app, String title, String text, long amount, String direction, String status, String detail) {
        ContentValues v = new ContentValues();
        v.put("ts", System.currentTimeMillis());
        v.put("app", app);
        v.put("title", title);
        v.put("text", text);
        v.put("amount", amount);
        v.put("direction", direction);
        v.put("status", status);
        v.put("detail", detail);
        long id = getWritableDatabase().insert("logs", null, v);
        // Batasi 300 log terbaru
        getWritableDatabase().execSQL("DELETE FROM logs WHERE id NOT IN (SELECT id FROM logs ORDER BY id DESC LIMIT 300)");
        return id;
    }

    public synchronized void updateLog(long id, String status, String detail) {
        if (id <= 0) return;
        ContentValues v = new ContentValues();
        v.put("status", status);
        v.put("detail", detail);
        getWritableDatabase().update("logs", v, "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized List<Log> recentLogs(int limit) {
        List<Log> out = new ArrayList<Log>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id, ts, app, title, text, amount, direction, status, detail FROM logs ORDER BY id DESC LIMIT " + limit, null);
        try {
            while (c.moveToNext()) {
                Log l = new Log();
                l.id = c.getLong(0);
                l.ts = c.getLong(1);
                l.app = c.getString(2);
                l.title = c.getString(3);
                l.text = c.getString(4);
                l.amount = c.getLong(5);
                l.direction = c.getString(6);
                l.status = c.getString(7);
                l.detail = c.getString(8);
                out.add(l);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized void clearLogs() {
        getWritableDatabase().delete("logs", null, null);
    }
}
