package com.qrify.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyQrFragment extends Fragment {

    private static final int F_ALL = 0;
    private static final int F_PHOTO = 1;
    private static final int F_VIDEO = 2;

    private QrAdapter adapter;
    private TextView chipAll;
    private TextView chipPhotos;
    private TextView chipVideos;
    private TextView empty;
    private EditText search;
    private int filter = F_ALL;
    private String query = "";
    private List<QrItem> all = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_my_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        chipAll = v.findViewById(R.id.chip_all);
        chipPhotos = v.findViewById(R.id.chip_photos);
        chipVideos = v.findViewById(R.id.chip_videos);
        empty = v.findViewById(R.id.txt_empty);
        search = v.findViewById(R.id.edit_search);

        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new QrAdapter(new QrAdapter.Listener() {
            @Override
            public void onClick(QrItem item) {
                openDetails(item);
            }

            @Override
            public void onMore(QrItem item, View anchor) {
                showMenu(item, anchor);
            }
        });
        rv.setAdapter(adapter);

        chipAll.setOnClickListener(x -> setFilter(F_ALL));
        chipPhotos.setOnClickListener(x -> setFilter(F_PHOTO));
        chipVideos.setOnClickListener(x -> setFilter(F_VIDEO));

        v.findViewById(R.id.btn_search).setOnClickListener(x -> {
            boolean show = search.getVisibility() != View.VISIBLE;
            search.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) {
                search.setText("");
            } else {
                search.requestFocus();
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                apply();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        v.findViewById(R.id.btn_create).setOnClickListener(x ->
                ((MainActivity) requireActivity()).select(MainActivity.TAB_HOME));
    }

    @Override
    public void onResume() {
        super.onResume();
        all = QrStore.list(requireContext());
        apply();
    }

    private void setFilter(int f) {
        filter = f;
        apply();
    }

    private void apply() {
        List<QrItem> out = new ArrayList<>();
        int photos = 0;
        int videos = 0;
        for (QrItem q : all) {
            if (q.isVideo()) videos++;
            else photos++;
            if (filter == F_PHOTO && q.isVideo()) continue;
            if (filter == F_VIDEO && !q.isVideo()) continue;
            if (!query.isEmpty() && !q.name.toLowerCase(Locale.ROOT).contains(query)) continue;
            out.add(q);
        }
        chipAll.setText("All (" + all.size() + ")");
        chipPhotos.setText("Photos (" + photos + ")");
        chipVideos.setText("Videos (" + videos + ")");
        style(chipAll, filter == F_ALL);
        style(chipPhotos, filter == F_PHOTO);
        style(chipVideos, filter == F_VIDEO);
        adapter.submit(out);
        empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void style(TextView t, boolean on) {
        t.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
        t.setTextColor(ContextCompat.getColor(requireContext(), on ? R.color.white : R.color.text_primary));
    }

    private void openDetails(QrItem q) {
        Intent i = new Intent(requireContext(), ReadyActivity.class);
        i.putExtra(ReadyActivity.EXTRA_ID, q.id);
        startActivity(i);
    }

    private void showMenu(final QrItem q, View anchor) {
        PopupMenu pm = new PopupMenu(requireContext(), anchor);
        pm.getMenu().add(0, 1, 0, "Open");
        pm.getMenu().add(0, 2, 1, "Share");
        pm.getMenu().add(0, 3, 2, "Rename");
        pm.getMenu().add(0, 4, 3, "Delete");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    openDetails(q);
                    return true;
                case 2:
                    Utils.sharePng(requireContext(), q.pngFile(requireContext()));
                    return true;
                case 3:
                    rename(q);
                    return true;
                case 4:
                    confirmDelete(q);
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    private void rename(final QrItem q) {
        final EditText et = new EditText(requireContext());
        et.setText(q.name);
        et.setSelection(q.name.length());
        et.setSingleLine(true);
        int pad = Utils.dp(requireContext(), 20);
        et.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(requireContext())
                .setTitle("Rename QR")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String n = et.getText().toString().trim();
                    if (!n.isEmpty()) {
                        QrStore.rename(requireContext(), q.id, n);
                        onResume();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(final QrItem q) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete QR?")
                .setMessage("\"" + q.name + "\" will be removed from this phone.")
                .setPositiveButton("Delete", (d, w) -> {
                    QrStore.delete(requireContext(), q.id);
                    onResume();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
