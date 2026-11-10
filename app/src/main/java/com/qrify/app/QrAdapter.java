package com.qrify.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class QrAdapter extends RecyclerView.Adapter<QrAdapter.VH> {

    public interface Listener {
        void onClick(QrItem item);

        void onMore(QrItem item, View anchor);
    }

    private final List<QrItem> items = new ArrayList<>();
    private final Listener listener;

    public QrAdapter(Listener l) {
        this.listener = l;
    }

    public void submit(List<QrItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_qr, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final QrItem q = items.get(position);
        h.name.setText(q.name);
        h.date.setText(Utils.formatDate(q.createdAt));
        Glide.with(h.img).load(q.pngFile(h.img.getContext())).into(h.img);
        h.itemView.setOnClickListener(v -> listener.onClick(q));
        h.more.setOnClickListener(v -> listener.onMore(q, v));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img;
        final ImageView more;
        final TextView name;
        final TextView date;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img_qr);
            more = v.findViewById(R.id.btn_more);
            name = v.findViewById(R.id.txt_name);
            date = v.findViewById(R.id.txt_date);
        }
    }
}
