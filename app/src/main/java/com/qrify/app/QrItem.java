package com.qrify.app;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class QrItem {
    public String id;
    public String name;
    /** "image" or "video" */
    public String type;
    /** Text encoded in the QR: cloud URL or qrify://local/&lt;id&gt; */
    public String content;
    /** Absolute path of the local media copy (local QR only), else null. */
    public String mediaPath;
    public long createdAt;
    public int scans;

    public boolean isLocal() {
        return content != null && content.startsWith(MediaOpener.LOCAL_PREFIX);
    }

    public boolean isVideo() {
        return "video".equals(type);
    }

    public File pngFile(Context c) {
        return new File(Utils.qrDir(c), id + ".png");
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("type", type);
        o.put("content", content);
        o.put("mediaPath", mediaPath == null ? JSONObject.NULL : mediaPath);
        o.put("createdAt", createdAt);
        o.put("scans", scans);
        return o;
    }

    public static QrItem fromJson(JSONObject o) {
        QrItem q = new QrItem();
        q.id = o.optString("id");
        q.name = o.optString("name", "My QR");
        q.type = o.optString("type", "image");
        q.content = o.optString("content");
        q.mediaPath = o.isNull("mediaPath") ? null : o.optString("mediaPath", null);
        q.createdAt = o.optLong("createdAt");
        q.scans = o.optInt("scans");
        return q;
    }
}
