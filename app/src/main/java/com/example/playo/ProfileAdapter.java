package com.example.playo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * RecyclerView adapter for the Profile account menu.
 * Binds each {@link ProfileOption} to an item layout and handles clicks.
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

    /** Callback invoked when a menu option is clicked. */
    public interface OnOptionClickListener {
        void onOptionClick(ProfileOption option);
    }

    private final ArrayList<ProfileOption> options;
    private final OnOptionClickListener clickListener;

    public ProfileAdapter(ArrayList<ProfileOption> options, OnOptionClickListener clickListener) {
        this.options = options;
        this.clickListener = clickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.menuIcon);
            title = itemView.findViewById(R.id.menuTitle);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileOption option = options.get(position);

        // Apply icon tint so all menu icons share the brand color.
        holder.icon.setImageResource(option.getIconRes());
        holder.icon.setColorFilter(holder.itemView.getContext().getColor(R.color.colorPrimary));
        holder.title.setText(option.getTitle());

        // Attach click listener for the whole row.
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onOptionClick(option);
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }
}