package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private final ArrayList<Turfname> turfnameArrayList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Turfname turfname);
    }

    public CustomAdapter(ArrayList<Turfname> turfnameArrayList, OnItemClickListener listener) {
        this.turfnameArrayList = turfnameArrayList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView image;
        public final TextView name;
        public final TextView price;
        public final RatingBar ratingBar;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.image);
            name = view.findViewById(R.id.turfname);
            price = view.findViewById(R.id.price);
            ratingBar = view.findViewById(R.id.ratingBar);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.itemelayout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Turfname turfname = turfnameArrayList.get(position);
        if (turfname != null) {
            holder.name.setText(turfname.getName());
            holder.price.setText("₹ " + turfname.getPrice() + " / Hour");
            holder.image.setImageResource(turfname.getTurfimg());
            holder.ratingBar.setRating((float) turfname.getRating());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(turfname);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return turfnameArrayList.size();
    }
}
