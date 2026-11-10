package com.qrify.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;

public class HomeFragment extends Fragment {

    private TextView greeting;
    private Uri cameraUri;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) openCustomize(uri, "image");
            });

    private final ActivityResultLauncher<String> pickVideo =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) openCustomize(uri, "video");
            });

    private final ActivityResultLauncher<String> pickAny =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String mime = requireContext().getContentResolver().getType(uri);
                if (mime != null && mime.startsWith("image/")) {
                    openCustomize(uri, "image");
                } else if (mime != null && mime.startsWith("video/")) {
                    openCustomize(uri, "video");
                } else {
                    Toast.makeText(requireContext(), "Please choose a photo or a video", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && cameraUri != null) openCustomize(cameraUri, "image");
            });

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        greeting = v.findViewById(R.id.txt_greeting);

        v.findViewById(R.id.card_photo).setOnClickListener(x -> pickImage.launch("image/*"));
        v.findViewById(R.id.card_video).setOnClickListener(x -> pickVideo.launch("video/*"));
        v.findViewById(R.id.card_files).setOnClickListener(x -> pickAny.launch("*/*"));
        v.findViewById(R.id.card_camera).setOnClickListener(x -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA);
            }
        });

        v.findViewById(R.id.qt_scan).setOnClickListener(x -> main().select(MainActivity.TAB_SCAN));
        v.findViewById(R.id.qt_myqr).setOnClickListener(x -> main().select(MainActivity.TAB_MYQR));
        v.findViewById(R.id.qt_settings).setOnClickListener(x -> main().select(MainActivity.TAB_SETTINGS));
        v.findViewById(R.id.btn_view_all).setOnClickListener(x -> main().select(MainActivity.TAB_MYQR));
        v.findViewById(R.id.btn_menu).setOnClickListener(x -> main().select(MainActivity.TAB_SETTINGS));
        v.findViewById(R.id.btn_avatar).setOnClickListener(x -> main().select(MainActivity.TAB_SETTINGS));
        v.findViewById(R.id.chip_premium).setOnClickListener(x ->
                Toast.makeText(requireContext(), "Premium is coming soon", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        String name = Prefs.name(requireContext());
        greeting.setText(name.isEmpty() ? "Hello 👋" : "Hello, " + name + " 👋");
    }

    private MainActivity main() {
        return (MainActivity) requireActivity();
    }

    private void launchCamera() {
        File f = new File(requireContext().getCacheDir(), "cam_" + System.currentTimeMillis() + ".jpg");
        cameraUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", f);
        takePicture.launch(cameraUri);
    }

    private void openCustomize(Uri uri, String type) {
        Intent i = new Intent(requireContext(), CustomizeActivity.class);
        i.setData(uri);
        i.putExtra(CustomizeActivity.EXTRA_TYPE, type);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
    }
}
