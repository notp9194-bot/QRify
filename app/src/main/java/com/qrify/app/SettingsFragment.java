package com.qrify.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.List;

public class SettingsFragment extends Fragment {

    private TextView txtName;
    private LinearLayout list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        txtName = v.findViewById(R.id.txt_name);
        list = v.findViewById(R.id.list_container);
        v.findViewById(R.id.row_profile).setOnClickListener(x -> editName());
        v.findViewById(R.id.btn_upgrade).setOnClickListener(x ->
                Toast.makeText(requireContext(), "Premium is coming soon", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        String n = Prefs.name(requireContext());
        txtName.setText(n.isEmpty() ? "Add your name" : n);

        list.removeAllViews();
        addRow(R.drawable.ic_cloud, "Cloud Storage",
                Prefs.cloudReady(requireContext()) ? "Connected" : "Not set", this::cloudDialog);
        addRow(R.drawable.ic_grid, "Default Shape",
                Palette.STYLE_NAMES[Prefs.defStyle(requireContext())], this::shapeDialog);
        addRow(R.drawable.ic_palette, "Default Color",
                Palette.FG_NAMES[Prefs.defColor(requireContext())], this::colorDialog);
        addRow(R.drawable.ic_image, "Add Logo",
                Prefs.addLogo(requireContext()) ? "On" : "Off", () -> {
                    Prefs.setAddLogo(requireContext(), !Prefs.addLogo(requireContext()));
                    build();
                });
        addRow(R.drawable.ic_chart, "Scan Analytics", "", this::analyticsDialog);
        addRow(R.drawable.ic_lock, "Privacy & Security", "", () -> info("Privacy & Security",
                "• Your media stays on your phone unless you set up Cloud Storage.\n"
                        + "• With Cloud Storage, the file is uploaded to your own Cloudinary account and the QR stores only its link.\n"
                        + "• Anyone who has the QR can open that link, so share it only with people you trust.\n"
                        + "• No account or API secret is stored in this app."));
        addRow(R.drawable.ic_help, "Help & Support", "", () -> info("Help & Support",
                "1. Pick a photo or video on Home.\n"
                        + "2. Style your QR and tap Done.\n"
                        + "3. Share the QR. Anyone can scan it with QRify to view your media.\n\n"
                        + "To make QR codes that work on other phones, open Cloud Storage and enter your Cloudinary cloud name and an unsigned upload preset."));
        addRow(R.drawable.ic_info, "About App", "v1.0", () -> info("QRify",
                "Convert photos & videos into QR codes, and scan them to play.\nVersion 1.0"));
    }

    private void addRow(@DrawableRes int icon, String label, String value, final Runnable click) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_setting, list, false);
        ((ImageView) row.findViewById(R.id.row_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        ((TextView) row.findViewById(R.id.row_value)).setText(value);
        row.findViewById(R.id.row_root).setOnClickListener(x -> click.run());
        list.addView(row);
    }

    private void editName() {
        final EditText et = new EditText(requireContext());
        et.setHint("Your name");
        et.setText(Prefs.name(requireContext()));
        et.setSingleLine(true);
        int pad = Utils.dp(requireContext(), 20);
        et.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(requireContext())
                .setTitle("Your name")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    Prefs.setName(requireContext(), et.getText().toString());
                    build();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cloudDialog() {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Utils.dp(requireContext(), 20);
        box.setPadding(pad, pad / 2, pad, 0);
        final EditText cloud = new EditText(requireContext());
        cloud.setHint("Cloud name");
        cloud.setSingleLine(true);
        cloud.setText(Prefs.cloudName(requireContext()));
        final EditText preset = new EditText(requireContext());
        preset.setHint("Unsigned upload preset");
        preset.setSingleLine(true);
        preset.setText(Prefs.uploadPreset(requireContext()));
        box.addView(cloud);
        box.addView(preset);
        new AlertDialog.Builder(requireContext())
                .setTitle("Cloudinary")
                .setMessage("Needed so QR codes work on other phones. Create an unsigned upload preset in your Cloudinary dashboard (Settings > Upload).")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    Prefs.setCloud(requireContext(), cloud.getText().toString(), preset.getText().toString());
                    build();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shapeDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Default Shape")
                .setSingleChoiceItems(Palette.STYLE_NAMES, Prefs.defStyle(requireContext()), (d, which) -> {
                    Prefs.setDefStyle(requireContext(), which);
                    d.dismiss();
                    build();
                })
                .show();
    }

    private void colorDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Default Color")
                .setSingleChoiceItems(Palette.FG_NAMES, Prefs.defColor(requireContext()), (d, which) -> {
                    Prefs.setDefColor(requireContext(), which);
                    d.dismiss();
                    build();
                })
                .show();
    }

    private void analyticsDialog() {
        List<QrItem> items = QrStore.list(requireContext());
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (QrItem q : items) {
            total += q.scans;
            sb.append("• ").append(q.name).append(": ").append(q.scans).append(q.scans == 1 ? " scan\n" : " scans\n");
        }
        if (items.isEmpty()) sb.append("No QR codes yet.");
        else sb.insert(0, "Total scans on this phone: " + total + "\n\n");
        info("Scan Analytics", sb.toString().trim());
    }

    private void info(String title, String msg) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}
