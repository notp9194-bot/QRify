package com.qrify.app;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/** ImageView with pinch-zoom, drag and double-tap zoom. */
public class ZoomImageView extends AppCompatImageView {

    private static final float MAX_FACTOR = 5f;

    private final Matrix matrix = new Matrix();
    private final float[] vals = new float[9];
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float baseScale = 1f;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                zoomBy(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (scaleDetector.isInProgress()) return false;
                matrix.postTranslate(-dx, -dy);
                fixTranslation();
                setImageMatrix(matrix);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float cur = currentScale();
                float target = cur > baseScale * 1.05f ? baseScale : baseScale * 2.5f;
                zoomBy(target / cur, e.getX(), e.getY());
                return true;
            }
        });
    }

    @Override
    public void setImageDrawable(Drawable d) {
        super.setImageDrawable(d);
        fit();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fit();
    }

    private void fit() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) return;
        float dw = d.getIntrinsicWidth();
        float dh = d.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) return;
        float vw = getWidth();
        float vh = getHeight();
        baseScale = Math.min(vw / dw, vh / dh);
        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        matrix.postTranslate((vw - dw * baseScale) / 2f, (vh - dh * baseScale) / 2f);
        setImageMatrix(matrix);
    }

    private float currentScale() {
        matrix.getValues(vals);
        return vals[Matrix.MSCALE_X];
    }

    private void zoomBy(float factor, float px, float py) {
        float cur = currentScale();
        float target = Math.max(baseScale, Math.min(baseScale * MAX_FACTOR, cur * factor));
        float f = target / cur;
        matrix.postScale(f, f, px, py);
        fixTranslation();
        setImageMatrix(matrix);
    }

    private void fixTranslation() {
        Drawable d = getDrawable();
        if (d == null) return;
        RectF r = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(r);
        float vw = getWidth();
        float vh = getHeight();
        float dx = 0;
        float dy = 0;
        if (r.width() <= vw) {
            dx = (vw - r.width()) / 2f - r.left;
        } else if (r.left > 0) {
            dx = -r.left;
        } else if (r.right < vw) {
            dx = vw - r.right;
        }
        if (r.height() <= vh) {
            dy = (vh - r.height()) / 2f - r.top;
        } else if (r.top > 0) {
            dy = -r.top;
        } else if (r.bottom < vh) {
            dy = vh - r.bottom;
        }
        matrix.postTranslate(dx, dy);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }
}
