package com.qrify.app;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomizeActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type";

    /** Realistic-length sample so the preview density matches the real QR. */
    private static final String SAMPLE = "https://res.cloudinary.com/qrify/image/upload/v1700000000/sample1234567.jpg";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private Uri mediaUri;
    private String mediaType;
    private Bitmap logo;
    private boolean working;

    private final QrRenderer.Options opts = new QrRenderer.Options();
    private int fgIdx;
    private int bgIdx;

    private ImageView preview;
    private ImageView[] styleViews = new ImageView[6];
    private ImageView[] eyeViews = new ImageView[4];
    private TextView chipStyle;
    private TextView chipColor;
    private TextView chipBg;
    private View panelStyle;
    private View panelColor;
    private View panelBg;
    private LinearLayout rowFg;
    private LinearLayout rowBg;
    private TextView txtBorder;
    private View overlay;
    private TextView overlayText;
    private ProgressBar overlayProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);

        mediaUri = getIntent().getData();
        mediaType = getIntent().getStringExtra(EXTRA_TYPE);
        if (mediaUri == null) {
            finish();
            return;
        }
        if (mediaType == null) mediaType = "image";

        preview = findViewById(R.id.img_preview);
        chipStyle = findViewById(R.id.chip_style);
        chipColor = findViewById(R.id.chip_color);
        chipBg = findViewById(R.id.chip_bg);
        panelStyle = findViewById(R.id.panel_style);
        panelColor = findViewById(R.id.panel_color);
        panelBg = findViewById(R.id.panel_bg);
        rowFg = findViewById(R.id.row_fg);
        rowBg = findViewById(R.id.row_bg);
        txtBorder = findViewById(R.id.txt_border);
        overlay = findViewById(R.id.overlay);
        overlayText = findViewById(R.id.overlay_text);
        overlayProgress = findViewById(R.id.overlay_progress);

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            styleViews[i] = findViewById(getResources().getIdentifier("style_" + i, "id", getPackageName()));
            styleViews[i].setOnClickListener(v -> {
                opts.style = idx;
                refresh();
            });
        }
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            eyeViews[i] = findViewById(getResources().getIdentifier("eye_" + i, "id", getPackageName()));
            eyeViews[i].setOnClickListener(v -> {
                opts.eye = idx;
                refresh();
            });
        }

        // defaults from Settings
        opts.style = Prefs.defStyle(this);
        opts.eye = QrRenderer.EYE_ROUNDED;
        fgIdx = Prefs.defColor(this);
        bgIdx = 0;
        applyColors();

        buildSwatches();

        chipStyle.setOnClickListener(v -> showPanel(0));
        chipColor.setOnClickListener(v -> showPanel(1));
        chipBg.setOnClickListener(v -> showPanel(2));
        showPanel(0);

        SeekBar seek = findViewById(R.id.seek_border);
        opts.borderDp = seek.getProgress();
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                opts.borderDp = p;
                txtBorder.setText(p + " dp");
                refresh();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_done).setOnClickListener(v -> onDone());

        refresh();

        if (Prefs.addLogo(this)) {
            io.execute(() -> {
                final Bitmap b = Utils.loadLogo(this, mediaUri, mediaType);
                main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    logo = b;
                    opts.logo = b;
                    refresh();
                });
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    // ---- UI -------------------------------------------------------------------------------

    private void showPanel(int which) {
        panelStyle.setVisibility(which == 0 ? View.VISIBLE : View.GONE);
        panelColor.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        panelBg.setVisibility(which == 2 ? View.VISIBLE : View.GONE);
        styleChip(chipStyle, which == 0);
        styleChip(chipColor, which == 1);
        styleChip(chipBg, which == 2);
    }

    private void styleChip(TextView t, boolean on) {
        t.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
        t.setTextColor(ContextCompat.getColor(this, on ? R.color.white : R.color.text_primary));
    }

    private void buildSwatches() {
        rowFg.removeAllViews();
        for (int i = 0; i < Palette.FG.length; i++) {
            final int idx = i;
            int[] c = Palette.FG[i];
            rowFg.addView(swatch(c[0], c[1], fgIdx == i, v -> {
                fgIdx = idx;
                applyColors();
                buildSwatches();
                refresh();
            }));
        }
        rowBg.removeAllViews();
        for (int i = 0; i < Palette.BG.length; i++) {
            final int idx = i;
            rowBg.addView(swatch(Palette.BG[i], Palette.BG[i], bgIdx == i, v -> {
                bgIdx = idx;
                applyColors();
                buildSwatches();
                refresh();
            }));
        }
    }

    private View swatch(int c1, int c2, boolean selected, View.OnClickListener l) {
        View v = new View(this);
        int size = Utils.dp(this, 46);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(Utils.dp(this, 4), Utils.dp(this, 4), Utils.dp(this, 6), Utils.dp(this, 4));
        v.setLayoutParams(lp);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setShape(GradientDrawable.OVAL);
        if (selected) {
            g.setStroke(Utils.dp(this, 3), ContextCompat.getColor(this, R.color.primary));
        } else {
            g.setStroke(Utils.dp(this, 1), ContextCompat.getColor(this, R.color.stroke));
        }
        v.setBackground(g);
        v.setOnClickListener(l);
        return v;
    }

    private void applyColors() {
        opts.fg1 = Palette.FG[fgIdx][0];
        opts.fg2 = Palette.FG[fgIdx][1];
        opts.bg = Palette.BG[bgIdx];
    }

    private void refresh() {
        preview.setImageBitmap(QrRenderer.render(SAMPLE, opts, 700));
        for (int i = 0; i < styleViews.length; i++) {
            QrRenderer.Options o = opts.copy();
            o.style = i;
            o.logo = null;
            o.borderDp = 2;
            styleViews[i].setImageBitmap(QrRenderer.render(SAMPLE, o, 240));
            styleViews[i].setBackgroundResource(i == opts.style ? R.drawable.bg_outline_selected : R.drawable.bg_outline_default);
        }
        for (int i = 0; i < eyeViews.length; i++) {
            eyeViews[i].setBackgroundResource(i == opts.eye ? R.drawable.bg_outline_selected : R.drawable.bg_outline_default);
        }
    }

    // ---- Create ---------------------------------------------------------------------------

    private void onDone() {
        if (working) return;
        if (Prefs.cloudReady(this)) {
            create(true);
        } else {
            create(false);
        }
    }

    private void showOverlay(String text, boolean determinate) {
        overlay.setVisibility(View.VISIBLE);
        overlayText.setText(text);
        overlayProgress.setIndeterminate(!determinate);
        overlayProgress.setProgress(0);
    }

    private void create(final boolean cloud) {
        working = true;
        showOverlay(cloud ? "Uploading media…" : "Creating QR…", cloud);
        final String cloudName = Prefs.cloudName(this);
        final String preset = Prefs.uploadPreset(this);
        final QrRenderer.Options snapshot = opts.copy();

        io.execute(() -> {
            final String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String content;
            String mediaPath = null;
            String type = mediaType;
            try {
                if (cloud) {
                    CloudinaryUploader.Result r = CloudinaryUploader.upload(this, mediaUri, cloudName, preset,
                            p -> main.post(() -> overlayProgress.setProgress(p)));
                    content = r.url;
                    if ("video".equals(r.resourceType)) type = "video";
                    else if ("image".equals(r.resourceType)) type = "image";
                } else {
                    File f = Utils.copyToLocal(this, mediaUri, id, mediaType);
                    mediaPath = f.getAbsolutePath();
                    content = MediaOpener.LOCAL_PREFIX + id;
                }

                Bitmap bmp = QrRenderer.render(content, snapshot, 1024);
                QrItem item = new QrItem();
                item.id = id;
                item.type = type;
                item.name = "video".equals(type) ? "My Video" : "My Photo";
                item.content = content;
                item.mediaPath = mediaPath;
                item.createdAt = System.currentTimeMillis();
                Utils.savePng(bmp, item.pngFile(this));
                QrStore.add(this, item);

                main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    android.content.Intent i = new android.content.Intent(this, ReadyActivity.class);
                    i.putExtra(ReadyActivity.EXTRA_ID, id);
                    i.putExtra(ReadyActivity.EXTRA_NEW, true);
                    startActivity(i);
                    finish();
                });
            } catch (final Exception e) {
                main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    working = false;
                    overlay.setVisibility(View.GONE);
                    if (cloud) {
                        new AlertDialog.Builder(this)
                                .setTitle("Upload failed")
                                .setMessage(e.getMessage() == null ? "Check your connection and Cloudinary settings."
                                        : e.getMessage())
                                .setPositiveButton("Retry", (d, w) -> create(true))
                                .setNeutralButton("Local QR instead", (d, w) -> create(false))
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Could not create QR")
                                .setMessage(e.getMessage() == null ? "Unknown error" : e.getMessage())
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            }
        });
    }
}
