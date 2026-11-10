package com.qrify.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import java.io.File;

/** Resolves scanned QR text into something we can show, and launches the viewer. */
public final class MediaOpener {

    public static final String LOCAL_PREFIX = "qrify://local/";

    private MediaOpener() {
    }

    public static final class Result {
        public String content;
        /** true when ViewerActivity can show it */
        public boolean media;
        public String src;
        /** image, video, link or text */
        public String type = "text";
        public QrItem item;
        public String error;
    }

    public static Result resolve(Context c, String content) {
        Result r = new Result();
        r.content = content;
        if (content.startsWith(LOCAL_PREFIX)) {
            String id = content.substring(LOCAL_PREFIX.length());
            QrItem it = QrStore.get(c, id);
            if (it != null && it.mediaPath != null && new File(it.mediaPath).exists()) {
                r.media = true;
                r.src = it.mediaPath;
                r.type = it.type;
                r.item = it;
            } else {
                r.type = "text";
                r.error = "This QR points to media stored on another phone.\n"
                        + "Ask the sender to create it with cloud storage enabled.";
            }
        } else if (Utils.isHttp(content)) {
            String t = Utils.guessType(content);
            r.type = t;
            if (!"link".equals(t)) {
                r.media = true;
                r.src = content;
                r.item = QrStore.findByContent(c, content);
            }
        }
        return r;
    }

    /** Records history + analytics and opens the viewer (media) or a dialog (everything else). */
    public static void launch(Context c, Result r) {
        QrStore.addHistory(c, r.content, r.type);
        if (r.media) {
            if (r.item != null) QrStore.bumpScans(c, r.item.id);
            Intent i = new Intent(c, ViewerActivity.class);
            i.putExtra(ViewerActivity.EXTRA_SRC, r.src);
            i.putExtra(ViewerActivity.EXTRA_TYPE, r.type);
            c.startActivity(i);
        } else {
            showText(c, r);
        }
    }

    public static void showText(final Context c, Result r) {
        final String text = r.error != null ? r.error : r.content;
        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setTitle(r.error != null ? "Cannot open media" : "QR Content")
                .setMessage(text)
                .setNegativeButton("Close", null);
        if (r.error == null) {
            b.setNeutralButton("Copy", (d, w) -> Utils.copyText(c, "QR", text));
            if (Utils.isHttp(text)) {
                b.setPositiveButton("Open", (d, w) -> {
                    try {
                        c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(text)));
                    } catch (Exception ignored) {
                    }
                });
            }
        }
        b.show();
    }
}
