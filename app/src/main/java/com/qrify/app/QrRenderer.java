package com.qrify.app;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/** Draws styled QR codes (module shape, eye shape, gradient, border, centre logo). */
public final class QrRenderer {

    public static final int STYLE_SQUARE = 0;
    public static final int STYLE_DOT = 1;
    public static final int STYLE_ROUNDED = 2;
    public static final int STYLE_DIAMOND = 3;
    public static final int STYLE_HEART = 4;
    public static final int STYLE_BARS = 5;

    public static final int EYE_SQUARE = 0;
    public static final int EYE_CIRCLE = 1;
    public static final int EYE_ROUNDED = 2;
    public static final int EYE_LEAF = 3;

    public static class Options {
        public int style = STYLE_ROUNDED;
        public int eye = EYE_ROUNDED;
        public int fg1 = 0xFF6A35EE;
        public int fg2 = 0xFF6A35EE;
        public int bg = Color.WHITE;
        public int borderDp = 12;
        public Bitmap logo;

        public Options copy() {
            Options o = new Options();
            o.style = style;
            o.eye = eye;
            o.fg1 = fg1;
            o.fg2 = fg2;
            o.bg = bg;
            o.borderDp = borderDp;
            o.logo = logo;
            return o;
        }
    }

    private static final Path HEART = new Path();

    static {
        HEART.moveTo(0.5f, 0.92f);
        HEART.cubicTo(0.02f, 0.58f, 0.0f, 0.18f, 0.25f, 0.12f);
        HEART.cubicTo(0.38f, 0.09f, 0.47f, 0.18f, 0.5f, 0.27f);
        HEART.cubicTo(0.53f, 0.18f, 0.62f, 0.09f, 0.75f, 0.12f);
        HEART.cubicTo(1.0f, 0.18f, 0.98f, 0.58f, 0.5f, 0.92f);
        HEART.close();
    }

    private QrRenderer() {
    }

    public static Bitmap render(String content, Options o, int size) {
        BitMatrix mx;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            mx = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (WriterException e) {
            throw new IllegalArgumentException(e);
        }

        final int n = mx.getWidth();
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        // white base so scanners never see transparent (black) corners
        c.drawColor(Color.WHITE);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(o.bg);
        c.drawRoundRect(new RectF(0, 0, size, size), size * 0.06f, size * 0.06f, bgPaint);

        float pad = size * (0.05f + o.borderDp * 0.004f);
        float m = (size - 2 * pad) / n;

        Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);
        fg.setStyle(Paint.Style.FILL);
        if (o.fg1 != o.fg2) {
            fg.setShader(new LinearGradient(0, 0, size, size, o.fg1, o.fg2, Shader.TileMode.CLAMP));
        } else {
            fg.setColor(o.fg1);
        }

        Path scratch = new Path();
        Matrix mt = new Matrix();
        RectF rr = new RectF();

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (!mx.get(x, y) || isEye(x, y, n)) continue;
                drawModule(c, fg, o.style, pad + x * m, pad + y * m, m, scratch, mt, rr);
            }
        }

        drawEye(c, fg, bgPaint, o.eye, pad, pad, m);
        drawEye(c, fg, bgPaint, o.eye, pad + (n - 7) * m, pad, m);
        drawEye(c, fg, bgPaint, o.eye, pad, pad + (n - 7) * m, m);

        if (o.borderDp > 0) {
            drawBrackets(c, fg, size);
        }

        if (o.logo != null) {
            drawLogo(c, o.logo, size);
        }
        return bmp;
    }

    private static boolean isEye(int x, int y, int n) {
        return (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7);
    }

    private static void drawModule(Canvas c, Paint p, int style, float l, float t, float m,
                                   Path scratch, Matrix mt, RectF rr) {
        switch (style) {
            case STYLE_SQUARE:
                c.drawRect(l, t, l + m + 0.6f, t + m + 0.6f, p);
                break;
            case STYLE_DOT:
                c.drawCircle(l + m / 2f, t + m / 2f, m * 0.47f, p);
                break;
            case STYLE_DIAMOND:
                scratch.reset();
                scratch.moveTo(l + m / 2f, t - m * 0.04f);
                scratch.lineTo(l + m * 1.04f, t + m / 2f);
                scratch.lineTo(l + m / 2f, t + m * 1.04f);
                scratch.lineTo(l - m * 0.04f, t + m / 2f);
                scratch.close();
                c.drawPath(scratch, p);
                break;
            case STYLE_HEART:
                mt.reset();
                mt.setScale(m * 1.12f, m * 1.12f);
                mt.postTranslate(l - m * 0.06f, t - m * 0.06f);
                HEART.transform(mt, scratch);
                c.drawPath(scratch, p);
                break;
            case STYLE_BARS:
                rr.set(l + m * 0.14f, t, l + m * 0.86f, t + m + 0.4f);
                c.drawRoundRect(rr, m * 0.3f, m * 0.3f, p);
                break;
            case STYLE_ROUNDED:
            default:
                rr.set(l + m * 0.04f, t + m * 0.04f, l + m * 0.96f, t + m * 0.96f);
                c.drawRoundRect(rr, m * 0.36f, m * 0.36f, p);
                break;
        }
    }

    private static void drawEye(Canvas c, Paint fg, Paint bg, int eye, float x, float y, float m) {
        shape(c, fg, new RectF(x, y, x + 7 * m, y + 7 * m), eye);
        shape(c, bg, new RectF(x + m, y + m, x + 6 * m, y + 6 * m), eye);
        shape(c, fg, new RectF(x + 2 * m, y + 2 * m, x + 5 * m, y + 5 * m), eye);
    }

    private static void shape(Canvas c, Paint p, RectF r, int eye) {
        float w = r.width();
        switch (eye) {
            case EYE_CIRCLE:
                c.drawOval(r, p);
                break;
            case EYE_ROUNDED:
                c.drawRoundRect(r, w * 0.28f, w * 0.28f, p);
                break;
            case EYE_LEAF: {
                float b = w * 0.45f;
                float s = w * 0.08f;
                Path pa = new Path();
                pa.addRoundRect(r, new float[]{b, b, s, s, b, b, s, s}, Path.Direction.CW);
                c.drawPath(pa, p);
                break;
            }
            case EYE_SQUARE:
            default:
                c.drawRect(r, p);
                break;
        }
    }

    private static void drawBrackets(Canvas c, Paint fg, int size) {
        Paint p = new Paint(fg);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(size * 0.012f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float in = size * 0.02f;
        float len = size * 0.11f;
        float r = size * 0.03f;
        float e = size - in;
        Path pa = new Path();
        // top-left
        pa.moveTo(in, in + len);
        pa.lineTo(in, in + r);
        pa.quadTo(in, in, in + r, in);
        pa.lineTo(in + len, in);
        // top-right
        pa.moveTo(e - len, in);
        pa.lineTo(e - r, in);
        pa.quadTo(e, in, e, in + r);
        pa.lineTo(e, in + len);
        // bottom-right
        pa.moveTo(e, e - len);
        pa.lineTo(e, e - r);
        pa.quadTo(e, e, e - r, e);
        pa.lineTo(e - len, e);
        // bottom-left
        pa.moveTo(in + len, e);
        pa.lineTo(in + r, e);
        pa.quadTo(in, e, in, e - r);
        pa.lineTo(in, e - len);
        c.drawPath(pa, p);
    }

    private static void drawLogo(Canvas c, Bitmap logo, int size) {
        int d = Math.round(size * 0.2f);
        float cx = size / 2f;
        float cy = size / 2f;

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setColor(Color.WHITE);
        c.drawCircle(cx, cy, d / 2f + size * 0.014f, ring);

        int side = Math.min(logo.getWidth(), logo.getHeight());
        int sx = (logo.getWidth() - side) / 2;
        int sy = (logo.getHeight() - side) / 2;
        Bitmap sq = Bitmap.createBitmap(logo, sx, sy, side, side);
        Bitmap sc = Bitmap.createScaledBitmap(sq, d, d, true);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader sh = new BitmapShader(sc, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix mt = new Matrix();
        mt.setTranslate(cx - d / 2f, cy - d / 2f);
        sh.setLocalMatrix(mt);
        p.setShader(sh);
        c.drawCircle(cx, cy, d / 2f, p);
    }
}
