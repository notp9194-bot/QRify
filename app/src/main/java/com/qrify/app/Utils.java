package com.qrify.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Utils {

    private Utils() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static File qrDir(Context c) {
        File d = new File(c.getFilesDir(), "qr");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File mediaDir(Context c) {
        File d = new File(c.getFilesDir(), "media");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static String formatDate(long t) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(t);
        if (sameDay(now, then)) {
            return "Today • " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(t));
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(now, then)) return "Yesterday";
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(t));
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isHttp(String s) {
        if (s == null) return false;
        String l = s.trim().toLowerCase(Locale.ROOT);
        return l.startsWith("http://") || l.startsWith("https://");
    }

    /** Returns "image", "video" or "link". */
    public static String guessType(String url) {
        String l = url.toLowerCase(Locale.ROOT);
        if (l.contains("/video/upload/")) return "video";
        if (l.contains("/image/upload/")) return "image";
        int q = l.indexOf('?');
        if (q >= 0) l = l.substring(0, q);
        String[] vid = {".mp4", ".mov", ".m4v", ".webm", ".3gp", ".mkv", ".avi"};
        String[] img = {".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".heic"};
        for (String e : vid) if (l.endsWith(e)) return "video";
        for (String e : img) if (l.endsWith(e)) return "image";
        return "link";
    }

    public static void copyText(Context c, String label, String text) {
        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
            Toast.makeText(c, "Copied", Toast.LENGTH_SHORT).show();
        }
    }

    /** Decodes a small preview of the media to use as the QR centre logo. May return null. */
    public static Bitmap loadLogo(Context c, Uri uri, String type) {
        try {
            if ("video".equals(type)) {
                MediaMetadataRetriever r = new MediaMetadataRetriever();
                try {
                    r.setDataSource(c, uri);
                    return r.getFrameAtTime(0);
                } finally {
                    r.release();
                }
            }
            ContentResolver cr = c.getContentResolver();
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = cr.openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int max = Math.max(o.outWidth, o.outHeight);
            int sample = 1;
            while (max / sample > 512) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            try (InputStream in = cr.openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, o2);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String extensionFor(Context c, Uri uri, String type) {
        String mime = c.getContentResolver().getType(uri);
        String ext = mime == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if (ext == null) ext = "video".equals(type) ? "mp4" : "jpg";
        return ext;
    }

    public static File copyToLocal(Context c, Uri uri, String id, String type) throws IOException {
        File out = new File(mediaDir(c), id + "." + extensionFor(c, uri, type));
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("Cannot read selected file");
            byte[] buf = new byte[32 * 1024];
            int r;
            while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
        }
        return out;
    }

    public static void savePng(Bitmap bmp, File f) throws IOException {
        try (FileOutputStream os = new FileOutputStream(f)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
    }

    public static void sharePng(Context c, File png) {
        Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", png);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/png");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.putExtra(Intent.EXTRA_TEXT, "Scan this QR code with QRify to open my media!");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        c.startActivity(Intent.createChooser(i, "Share QR"));
    }

    public static boolean saveToGallery(Context c, File png, String displayName) {
        try {
            String fname = displayName.replaceAll("[^A-Za-z0-9_-]", "_") + "_" + System.currentTimeMillis() + ".png";
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, fname);
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRify");
                v.put(MediaStore.Images.Media.IS_PENDING, 1);
                ContentResolver cr = c.getContentResolver();
                Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return false;
                try (OutputStream os = cr.openOutputStream(uri);
                     InputStream in = new FileInputStream(png)) {
                    if (os == null) return false;
                    byte[] buf = new byte[16 * 1024];
                    int r;
                    while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
                }
                ContentValues done = new ContentValues();
                done.put(MediaStore.Images.Media.IS_PENDING, 0);
                cr.update(uri, done, null, null);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "QRify");
                if (!dir.exists() && !dir.mkdirs()) return false;
                File out = new File(dir, fname);
                try (OutputStream os = new FileOutputStream(out);
                     InputStream in = new FileInputStream(png)) {
                    byte[] buf = new byte[16 * 1024];
                    int r;
                    while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
                }
                MediaScannerConnection.scanFile(c, new String[]{out.getAbsolutePath()}, null, null);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
