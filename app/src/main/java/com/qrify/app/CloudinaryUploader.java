package com.qrify.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Unsigned upload to Cloudinary. No API secret is stored in the app: the user creates an
 * "unsigned" upload preset in the Cloudinary dashboard and enters cloud name + preset in Settings.
 */
public final class CloudinaryUploader {

    public interface Progress {
        void onProgress(int percent);
    }

    public static final class Result {
        public String url;
        /** "image" or "video" as reported by Cloudinary */
        public String resourceType;
    }

    private CloudinaryUploader() {
    }

    public static Result upload(Context ctx, Uri uri, String cloud, String preset, Progress cb) throws IOException {
        ContentResolver cr = ctx.getContentResolver();
        String mime = cr.getType(uri);
        if (mime == null) mime = "application/octet-stream";
        long total = sizeOf(cr, uri);

        String boundary = "----QRify" + UUID.randomUUID().toString().replace("-", "");
        String crlf = "\r\n";
        URL url = new URL("https://api.cloudinary.com/v1_1/" + cloud.trim() + "/auto/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(120000);
            conn.setChunkedStreamingMode(64 * 1024);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                out.writeBytes("--" + boundary + crlf);
                out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + crlf + crlf);
                out.writeBytes(preset.trim() + crlf);

                String filename = "media_" + System.currentTimeMillis();
                out.writeBytes("--" + boundary + crlf);
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + crlf);
                out.writeBytes("Content-Type: " + mime + crlf + crlf);

                try (InputStream in = cr.openInputStream(uri)) {
                    if (in == null) throw new IOException("Cannot read selected file");
                    byte[] buf = new byte[32 * 1024];
                    long sent = 0;
                    int last = -1;
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                        sent += r;
                        if (cb != null && total > 0) {
                            int p = (int) Math.min(99, sent * 100 / total);
                            if (p != last) {
                                last = p;
                                cb.onProgress(p);
                            }
                        }
                    }
                }
                out.writeBytes(crlf + "--" + boundary + "--" + crlf);
                out.flush();
            }

            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(is);
            if (code < 200 || code >= 300) {
                String msg = "HTTP " + code;
                try {
                    msg = new JSONObject(body).getJSONObject("error").optString("message", msg);
                } catch (Exception ignored) {
                }
                throw new IOException(msg);
            }
            try {
                JSONObject j = new JSONObject(body);
                Result res = new Result();
                res.url = j.getString("secure_url");
                res.resourceType = j.optString("resource_type", "image");
                if (cb != null) cb.onProgress(100);
                return res;
            } catch (Exception e) {
                throw new IOException("Unexpected response from Cloudinary");
            }
        } finally {
            conn.disconnect();
        }
    }

    private static long sizeOf(ContentResolver cr, Uri uri) {
        try (Cursor c = cr.query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getLong(0);
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int r;
        while ((r = is.read(b)) != -1) bo.write(b, 0, r);
        is.close();
        return bo.toString("UTF-8");
    }
}
