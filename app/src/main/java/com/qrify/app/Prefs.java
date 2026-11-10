package com.qrify.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("qrify_prefs", Context.MODE_PRIVATE);
    }

    public static boolean welcomed(Context c) {
        return sp(c).getBoolean("welcomed", false);
    }

    public static void setWelcomed(Context c) {
        sp(c).edit().putBoolean("welcomed", true).apply();
    }

    public static String name(Context c) {
        return sp(c).getString("name", "");
    }

    public static void setName(Context c, String v) {
        sp(c).edit().putString("name", v == null ? "" : v.trim()).apply();
    }

    public static String cloudName(Context c) {
        return sp(c).getString("cloud_name", "rvipvegh");
    }

    public static String uploadPreset(Context c) {
        return sp(c).getString("upload_preset", "fg2o9q17");
    }

    public static void setCloud(Context c, String cloud, String preset) {
        sp(c).edit().putString("cloud_name", cloud.trim()).putString("upload_preset", preset.trim()).apply();
    }

    public static boolean cloudReady(Context c) {
        return !cloudName(c).isEmpty() && !uploadPreset(c).isEmpty();
    }

    public static int defStyle(Context c) {
        return sp(c).getInt("def_style", QrRenderer.STYLE_ROUNDED);
    }

    public static void setDefStyle(Context c, int v) {
        sp(c).edit().putInt("def_style", v).apply();
    }

    public static int defColor(Context c) {
        return sp(c).getInt("def_color", 0);
    }

    public static void setDefColor(Context c, int v) {
        sp(c).edit().putInt("def_color", v).apply();
    }

    public static boolean addLogo(Context c) {
        return sp(c).getBoolean("add_logo", true);
    }

    public static void setAddLogo(Context c, boolean v) {
        sp(c).edit().putBoolean("add_logo", v).apply();
    }
}
