package com.duymanh.voicerecorder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {
    private List<String> recordings;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String filePath);
    }

    public RecordingAdapter(List<String> recordings, OnItemClickListener listener) {
        this.recordings = recordings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String filePath = recordings.get(position);
        holder.textView.setText(new File(filePath).getName());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(filePath));
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
