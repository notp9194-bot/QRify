package com.qrify.app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.Locale;

public class ViewerActivity extends AppCompatActivity {

    public static final String EXTRA_SRC = "src";
    public static final String EXTRA_TYPE = "type";

    private final Handler main = new Handler(Looper.getMainLooper());

    private String src;
    private boolean isVideo;
    private boolean isLocalFile;

    private ZoomImageView photo;
    private VideoView video;
    private View loading;
    private View error;
    private View controls;
    private View topBar;
    private ImageView btnPlay;
    private SeekBar seek;
    private TextView txtTime;
    private boolean fullscreen;
    private boolean userSeeking;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (isVideo && video != null && !userSeeking) {
                int pos = video.getCurrentPosition();
                seek.setProgress(pos);
                txtTime.setText(fmt(pos) + " / " + fmt(video.getDuration()));
                btnPlay.setImageResource(video.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            }
            main.postDelayed(this, 400);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        src = getIntent().getStringExtra(EXTRA_SRC);
        isVideo = "video".equals(getIntent().getStringExtra(EXTRA_TYPE));
        if (src == null) {
            finish();
            return;
        }
        isLocalFile = !Utils.isHttp(src);

        photo = findViewById(R.id.img_photo);
        video = findViewById(R.id.video);
        loading = findViewById(R.id.loading);
        error = findViewById(R.id.txt_error);
        controls = findViewById(R.id.controls);
        topBar = findViewById(R.id.top_bar);
        btnPlay = findViewById(R.id.btn_play);
        seek = findViewById(R.id.seek);
        txtTime = findViewById(R.id.txt_time);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share).setOnClickListener(v -> share());
        findViewById(R.id.btn_more).setOnClickListener(this::more);

        if (isVideo) {
            setupVideo();
        } else {
            setupPhoto();
        }
    }

    private void setupPhoto() {
        photo.setVisibility(View.VISIBLE);
        Object model = isLocalFile ? new File(src) : src;
        Glide.with(this)
                .load(model)
                .override(2048, 2048)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object m, Target<Drawable> t, boolean first) {
                        loading.setVisibility(View.GONE);
                        error.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable r, Object m, Target<Drawable> t, DataSource d, boolean first) {
                        loading.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(photo);
    }

    private void setupVideo() {
        video.setVisibility(View.VISIBLE);
        video.setVideoURI(isLocalFile ? Uri.fromFile(new File(src)) : Uri.parse(src));

        video.setOnPreparedListener(mp -> {
            loading.setVisibility(View.GONE);
            controls.setVisibility(View.VISIBLE);
            seek.setMax(video.getDuration());
            video.start();
        });
        video.setOnErrorListener((mp, what, extra) -> {
            loading.setVisibility(View.GONE);
            controls.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            return true;
        });
        video.setOnCompletionListener(mp -> btnPlay.setImageResource(R.drawable.ic_play));
        video.setOnClickListener(v -> {
            int vis = controls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            controls.setVisibility(vis);
        });

        btnPlay.setOnClickListener(v -> {
            if (video.isPlaying()) {
                video.pause();
            } else {
                if (video.getCurrentPosition() >= video.getDuration() - 200) video.seekTo(0);
                video.start();
            }
            btnPlay.setImageResource(video.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        });
        findViewById(R.id.btn_back10).setOnClickListener(v ->
                video.seekTo(Math.max(0, video.getCurrentPosition() - 10000)));
        findViewById(R.id.btn_fwd10).setOnClickListener(v ->
                video.seekTo(Math.min(video.getDuration(), video.getCurrentPosition() + 10000)));
        findViewById(R.id.btn_fullscreen).setOnClickListener(v -> toggleFullscreen());

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) txtTime.setText(fmt(p) + " / " + fmt(video.getDuration()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                userSeeking = false;
                video.seekTo(s.getProgress());
            }
        });
        main.post(ticker);
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        setRequestedOrientation(fullscreen
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (fullscreen) {
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            c.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            c.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    private void share() {
        if (isLocalFile) {
            android.widget.Toast.makeText(this,
                    "This media is stored only on this phone", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, src);
        startActivity(Intent.createChooser(i, "Share link"));
    }

    private void more(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        if (!isLocalFile) {
            pm.getMenu().add(0, 1, 0, "Copy link");
            pm.getMenu().add(0, 2, 1, "Open in browser");
        } else {
            pm.getMenu().add(0, 3, 0, "Stored on this phone");
        }
        pm.setOnMenuItemClickListener(m -> {
            if (m.getItemId() == 1) {
                Utils.copyText(this, "Media link", src);
            } else if (m.getItemId() == 2) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(src)));
                } catch (Exception ignored) {
                }
            }
            return true;
        });
        pm.show();
    }

    private static String fmt(int ms) {
        int s = Math.max(0, ms) / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isVideo && video != null && video.isPlaying()) video.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        main.removeCallbacks(ticker);
        if (isVideo && video != null) video.stopPlayback();
    }
}
