package com.qrify.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

public class ReadyActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_NEW = "new";

    private QrItem item;
    private boolean fresh;
    private TextView txtTitle;
    private TextView txtSub;

    private final ActivityResultLauncher<String> storagePermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    saveNow();
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ready);

        item = QrStore.get(this, getIntent().getStringExtra(EXTRA_ID));
        if (item == null) {
            finish();
            return;
        }
        fresh = getIntent().getBooleanExtra(EXTRA_NEW, false);

        txtTitle = findViewById(R.id.txt_title);
        txtSub = findViewById(R.id.txt_sub);
        ((TextView) findViewById(R.id.txt_header)).setText(fresh ? "QR Ready" : "QR Details");
        Glide.with(this).load(item.pngFile(this)).into((ImageView) findViewById(R.id.img_qr));
        bindText();

        findViewById(R.id.txt_local).setVisibility(item.isLocal() ? View.VISIBLE : View.GONE);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share).setOnClickListener(v -> Utils.sharePng(this, item.pngFile(this)));
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_open).setOnClickListener(v -> {
            MediaOpener.Result r = MediaOpener.resolve(this, item.content);
            if (r.media) {
                Intent i = new Intent(this, ViewerActivity.class);
                i.putExtra(ViewerActivity.EXTRA_SRC, r.src);
                i.putExtra(ViewerActivity.EXTRA_TYPE, r.type);
                startActivity(i);
            } else {
                MediaOpener.showText(this, r);
            }
        });
        findViewById(R.id.btn_more).setOnClickListener(this::showMore);
    }

    private void bindText() {
        if (fresh) {
            txtTitle.setText("QR Code Ready!");
            txtSub.setText("Scan to open your photo or video");
        } else {
            txtTitle.setText(item.name);
            txtSub.setText((item.isVideo() ? "Video" : "Photo") + " • " + Utils.formatDate(item.createdAt)
                    + " • " + item.scans + (item.scans == 1 ? " scan" : " scans"));
        }
    }

    private void save() {
        if (Build.VERSION.SDK_INT < 29
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        saveNow();
    }

    private void saveNow() {
        boolean ok = Utils.saveToGallery(this, item.pngFile(this), item.name);
        Toast.makeText(this, ok ? "Saved to Gallery" : "Could not save", Toast.LENGTH_SHORT).show();
    }

    private void showMore(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "Rename");
        pm.getMenu().add(0, 2, 1, "Copy link");
        pm.getMenu().add(0, 3, 2, "Delete");
        pm.setOnMenuItemClickListener(m -> {
            switch (m.getItemId()) {
                case 1:
                    rename();
                    return true;
                case 2:
                    Utils.copyText(this, "QR link", item.content);
                    return true;
                case 3:
                    confirmDelete();
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    private void rename() {
        final EditText et = new EditText(this);
        et.setText(item.name);
        et.setSelection(item.name.length());
        et.setSingleLine(true);
        int pad = Utils.dp(this, 20);
        et.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle("Rename QR")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String n = et.getText().toString().trim();
                    if (!n.isEmpty()) {
                        QrStore.rename(this, item.id, n);
                        item.name = n;
                        bindText();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete QR?")
                .setMessage("\"" + item.name + "\" will be removed from this phone.")
                .setPositiveButton("Delete", (d, w) -> {
                    QrStore.delete(this, item.id);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
