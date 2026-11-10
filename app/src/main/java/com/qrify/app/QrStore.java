package com.qrify.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** SharedPreferences-backed storage for created QR codes and scan history. */
public final class QrStore {

    private static final String KEY_ITEMS = "items";
    private static final String KEY_HISTORY = "history";
    private static final int HISTORY_MAX = 100;

    private QrStore() {
    }

    public static class HistoryEntry {
        public String content;
        /** image, video, link or text */
        public String type;
        public long time;
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("qrify_store", Context.MODE_PRIVATE);
    }

    // ---- QR items -------------------------------------------------------------------------

    public static synchronized List<QrItem> list(Context c) {
        List<QrItem> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(QrItem.fromJson(a.getJSONObject(i)));
        } catch (JSONException ignored) {
        }
        return out; // newest first (add() inserts at 0)
    }

    private static void save(Context c, List<QrItem> items) {
        JSONArray a = new JSONArray();
        try {
            for (QrItem q : items) a.put(q.toJson());
        } catch (JSONException ignored) {
        }
        sp(c).edit().putString(KEY_ITEMS, a.toString()).apply();
    }

    public static synchronized QrItem get(Context c, String id) {
        if (id == null) return null;
        for (QrItem q : list(c)) if (id.equals(q.id)) return q;
        return null;
    }

    public static synchronized QrItem findByContent(Context c, String content) {
        for (QrItem q : list(c)) if (content.equals(q.content)) return q;
        return null;
    }

    public static synchronized void add(Context c, QrItem item) {
        List<QrItem> items = list(c);
        items.add(0, item);
        save(c, items);
    }

    public static synchronized void rename(Context c, String id, String name) {
        List<QrItem> items = list(c);
        for (QrItem q : items) if (q.id.equals(id)) q.name = name;
        save(c, items);
    }

    public static synchronized void bumpScans(Context c, String id) {
        List<QrItem> items = list(c);
        for (QrItem q : items) if (q.id.equals(id)) q.scans++;
        save(c, items);
    }

    public static synchronized void delete(Context c, String id) {
        List<QrItem> items = list(c);
        List<QrItem> keep = new ArrayList<>();
        for (QrItem q : items) {
            if (q.id.equals(id)) {
                q.pngFile(c).delete();
                if (q.mediaPath != null) new File(q.mediaPath).delete();
            } else {
                keep.add(q);
            }
        }
        save(c, keep);
    }

    // ---- History --------------------------------------------------------------------------

    public static synchronized List<HistoryEntry> history(Context c) {
        List<HistoryEntry> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString(KEY_HISTORY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                HistoryEntry h = new HistoryEntry();
                h.content = o.optString("content");
                h.type = o.optString("type", "text");
                h.time = o.optLong("time");
                out.add(h);
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static synchronized void addHistory(Context c, String content, String type) {
        List<HistoryEntry> list = history(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).content.equals(content)) list.remove(i);
        }
        HistoryEntry h = new HistoryEntry();
        h.content = content;
        h.type = type;
        h.time = System.currentTimeMillis();
        list.add(0, h);
        while (list.size() > HISTORY_MAX) list.remove(list.size() - 1);

        JSONArray a = new JSONArray();
        try {
            for (HistoryEntry e : list) {
                JSONObject o = new JSONObject();
                o.put("content", e.content);
                o.put("type", e.type);
                o.put("time", e.time);
                a.put(o);
            }
        } catch (JSONException ignored) {
        }
        sp(c).edit().putString(KEY_HISTORY, a.toString()).apply();
    }

    public static synchronized void clearHistory(Context c) {
        sp(c).edit().remove(KEY_HISTORY).apply();
    }
}
