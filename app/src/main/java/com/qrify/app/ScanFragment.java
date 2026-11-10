package com.qrify.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanFragment extends Fragment {

    private PreviewView preview;
    private View permLayout;
    private View sheet;
    private ImageView flashBtn;
    private Camera camera;
    private boolean torch;
    private ExecutorService analysisExecutor;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable pendingOpen;

    private final ActivityResultLauncher<String> permission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    permLayout.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> pickQr =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) scanFromGallery(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        preview = v.findViewById(R.id.preview);
        permLayout = v.findViewById(R.id.perm_layout);
        sheet = v.findViewById(R.id.sheet);
        flashBtn = v.findViewById(R.id.btn_flash);
        analysisExecutor = Executors.newSingleThreadExecutor();

        v.findViewById(R.id.btn_gallery).setOnClickListener(x -> pickQr.launch("image/*"));
        v.findViewById(R.id.btn_gallery_alt).setOnClickListener(x -> pickQr.launch("image/*"));
        v.findViewById(R.id.btn_grant).setOnClickListener(x -> permission.launch(Manifest.permission.CAMERA));
        v.findViewById(R.id.btn_cancel).setOnClickListener(x -> cancelPending());
        flashBtn.setOnClickListener(x -> {
            if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
                Toast.makeText(requireContext(), "Flash not available", Toast.LENGTH_SHORT).show();
                return;
            }
            torch = !torch;
            camera.getCameraControl().enableTorch(torch);
            flashBtn.setAlpha(torch ? 1f : 0.7f);
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permission.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // returning from the viewer: allow scanning again
        busy.set(false);
        if (sheet != null) sheet.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingOpen != null) main.removeCallbacks(pendingOpen);
        pendingOpen = null;
        if (analysisExecutor != null) analysisExecutor.shutdown();
        camera = null;
    }

    private void startCamera() {
        permLayout.setVisibility(View.GONE);
        final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                if (!isAdded() || getView() == null) return;
                ProcessCameraProvider provider = future.get();
                Preview pv = new Preview.Builder().build();
                pv.setSurfaceProvider(preview.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(analysisExecutor, this::analyze);

                provider.unbindAll();
                camera = provider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA, pv, analysis);
            } catch (ExecutionException | InterruptedException | RuntimeException e) {
                Toast.makeText(requireContext(), "Camera error", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyze(@NonNull ImageProxy image) {
        try {
            if (busy.get()) return;
            ImageProxy.PlaneProxy plane = image.getPlanes()[0];
            ByteBuffer buf = plane.getBuffer();
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            String text = QrDecoder.decodeYuv(data, plane.getRowStride(), image.getWidth(), image.getHeight());
            if (text != null && busy.compareAndSet(false, true)) {
                main.post(() -> onDetected(text));
            }
        } catch (Exception ignored) {
        } finally {
            image.close();
        }
    }

    private void onDetected(String text) {
        if (!isAdded()) return;
        final MediaOpener.Result r = MediaOpener.resolve(requireContext(), text);
        if (r.media) {
            sheet.setVisibility(View.VISIBLE);
            pendingOpen = () -> {
                pendingOpen = null;
                if (isAdded()) {
                    sheet.setVisibility(View.GONE);
                    MediaOpener.launch(requireContext(), r);
                }
            };
            main.postDelayed(pendingOpen, 700);
        } else {
            MediaOpener.launch(requireContext(), r);
            // unlock again once the dialog is dismissed or after a short delay
            main.postDelayed(() -> busy.set(false), 2500);
        }
    }

    private void cancelPending() {
        if (pendingOpen != null) {
            main.removeCallbacks(pendingOpen);
            pendingOpen = null;
        }
        sheet.setVisibility(View.GONE);
        main.postDelayed(() -> busy.set(false), 800);
    }

    private void scanFromGallery(final Uri uri) {
        final android.content.Context ctx = requireContext().getApplicationContext();
        new Thread(() -> {
            String text = null;
            try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                Bitmap bmp = BitmapFactory.decodeStream(in);
                text = QrDecoder.decode(bmp);
            } catch (Exception ignored) {
            }
            final String result = text;
            main.post(() -> {
                if (!isAdded()) return;
                if (result == null) {
                    Toast.makeText(requireContext(), "No QR code found in this image", Toast.LENGTH_SHORT).show();
                } else {
                    busy.set(true);
                    onDetected(result);
                }
            });
        }).start();
    }
}
