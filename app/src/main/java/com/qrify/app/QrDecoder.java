package com.qrify.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Thin wrapper around ZXing for decoding from bitmaps and camera frames. */
public final class QrDecoder {

    private QrDecoder() {
    }

    private static MultiFormatReader newReader() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        MultiFormatReader r = new MultiFormatReader();
        r.setHints(hints);
        return r;
    }

    /** Returns decoded text or null. */
    public static String decode(Bitmap src) {
        if (src == null) return null;
        Bitmap b = src;
        int max = Math.max(b.getWidth(), b.getHeight());
        if (max > 1600) {
            float s = 1600f / max;
            b = Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * s), Math.round(b.getHeight() * s), true);
        }
        // flatten onto white so transparent pixels are not read as black
        Bitmap flat = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(flat);
        c.drawColor(Color.WHITE);
        c.drawBitmap(b, 0, 0, null);

        int w = flat.getWidth();
        int h = flat.getHeight();
        int[] px = new int[w * h];
        flat.getPixels(px, 0, w, 0, 0, w, h);
        return decodeSource(new RGBLuminanceSource(w, h, px));
    }

    public static String decodeYuv(byte[] data, int dataWidth, int width, int height) {
        return decodeSource(new PlanarYUVLuminanceSource(data, dataWidth, height, 0, 0, width, height, false));
    }

    private static String decodeSource(LuminanceSource source) {
        String r = tryDecode(new BinaryBitmap(new HybridBinarizer(source)));
        if (r != null) return r;
        r = tryDecode(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
        if (r != null) return r;
        return tryDecode(new BinaryBitmap(new HybridBinarizer(source.invert())));
    }

    private static String tryDecode(BinaryBitmap bb) {
        MultiFormatReader reader = newReader();
        try {
            Result res = reader.decodeWithState(bb);
            return res.getText();
        } catch (Exception e) {
            return null;
        } finally {
            reader.reset();
        }
    }
}
