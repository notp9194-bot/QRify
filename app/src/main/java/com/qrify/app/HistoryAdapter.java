package com.qrify.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface Listener {
        void onClick(QrStore.HistoryEntry e);
    }

    private final List<QrStore.HistoryEntry> items = new ArrayList<>();
    private final Listener listener;

    public HistoryAdapter(Listener l) {
        this.listener = l;
    }

    public void submit(List<QrStore.HistoryEntry> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final QrStore.HistoryEntry e = items.get(position);
        int icon;
        String label;
        switch (e.type) {
            case "video":
                icon = R.drawable.ic_video;
                label = "Video";
                break;
            case "image":
                icon = R.drawable.ic_image;
                label = "Photo";
                break;
            case "link":
                icon = R.drawable.ic_share;
                label = "Link";
                break;
            default:
                icon = R.drawable.ic_scan;
                label = "Text";
                break;
        }
        h.type.setImageResource(icon);
        h.url.setText(e.content);
        h.meta.setText(label + " • " + Utils.formatDate(e.time));
        h.itemView.setOnClickListener(v -> listener.onClick(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView type;
        final TextView url;
        final TextView meta;

        VH(View v) {
            super(v);
            type = v.findViewById(R.id.img_type);
            url = v.findViewById(R.id.txt_url);
            meta = v.findViewById(R.id.txt_meta);
        }
    }
}
