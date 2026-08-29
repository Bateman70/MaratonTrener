package com.jostein.maratontrener;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder> {

    private List<String> planNames;
    private OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onPlanClick(String planName);
    }

    public ArchiveAdapter(List<String> planNames, OnPlanClickListener listener) {
        this.planNames = planNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArchiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ArchiveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchiveViewHolder holder, int position) {
        String name = planNames.get(position);
        holder.textView.setText(name);
        holder.textView.setTextColor(0xFFFFFFFF); // White text for dark theme
        holder.itemView.setOnClickListener(v -> listener.onPlanClick(name));
    }

    @Override
    public int getItemCount() {
        return planNames.size();
    }

    static class ArchiveViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ArchiveViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}