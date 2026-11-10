package com.qrify.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryFragment extends Fragment {

    private HistoryAdapter adapter;
    private TextView empty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        empty = v.findViewById(R.id.txt_empty);
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(e -> {
            MediaOpener.Result r = MediaOpener.resolve(requireContext(), e.content);
            MediaOpener.launch(requireContext(), r);
        });
        rv.setAdapter(adapter);

        v.findViewById(R.id.btn_clear).setOnClickListener(x -> {
            if (adapter.getItemCount() == 0) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear history?")
                    .setMessage("All scan history will be removed.")
                    .setPositiveButton("Clear", (d, w) -> {
                        QrStore.clearHistory(requireContext());
                        reload();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<QrStore.HistoryEntry> list = QrStore.history(requireContext());
        adapter.submit(list);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
