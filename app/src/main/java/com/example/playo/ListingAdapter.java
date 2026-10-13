package com.example.playo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

/**
 * RecyclerView adapter for the list of turf listings owned by the user.
 * Supports View / Edit / Delete actions per row.
 */
public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.Binding> {

    /** Row actions exposed to the hosting screen. */
    public interface ListingActionListener {
        void onView(TurfListing listing);
        void onEdit(TurfListing listing);
        void onDelete(TurfListing listing);
    }

    private final ArrayList<TurfListing> listings;
    private final ListingActionListener listener;

    public ListingAdapter(ArrayList<TurfListing> listings, ListingActionListener listener) {
        this.listings = listings;
        this.listener = listener;
    }

    public static class Binding extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView availability;
        final TextView price;
        final TextView bookings;
        final RatingBar rating;

        Binding(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.listingImage);
            name = itemView.findViewById(R.id.listingName);
            availability = itemView.findViewById(R.id.listingAvailability);
            price = itemView.findViewById(R.id.listingPrice);
            bookings = itemView.findViewById(R.id.listingBookings);
            rating = itemView.findViewById(R.id.listingRating);
        }
    }

    @NonNull
    @Override
    public Binding onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing, parent, false);
        PressEffect.attach(view);
        PressEffect.attach(view.findViewById(R.id.btnEdit));
        PressEffect.attach(view.findViewById(R.id.btnDelete));
        return new Binding(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Binding holder, int position) {
        TurfListing listing = listings.get(position);

        if (listing.getThumbnailBytes() != null) {
            Glide.with(holder.image.getContext())
                    .load(listing.getThumbnailBytes())
                    .placeholder(listing.getImageRes())
                    .into(holder.image);
        } else if (listing.getImageUrl() != null) {
            Glide.with(holder.image.getContext())
                    .load(listing.getImageUrl())
                    .placeholder(listing.getImageRes())
                    .into(holder.image);
        } else {
            holder.image.setImageResource(listing.getImageRes());
        }
        holder.name.setText(listing.getTurfName());
        holder.price.setText("₹" + (int) listing.getPrice() + " / Hour");
        holder.bookings.setText("Bookings: " + listing.getTotalBookings());
        holder.rating.setRating(listing.getRating());

        // Availability chip styling.
        Context context = holder.itemView.getContext();
        if (listing.isAvailable()) {
            holder.availability.setText("Available");
            holder.availability.setTextColor(ContextCompat.getColor(context, R.color.status_confirmed));
        } else {
            holder.availability.setText("Unavailable");
            holder.availability.setTextColor(ContextCompat.getColor(context, R.color.status_cancelled));
        }

        // Row click opens the turf detail page.
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onView(listing);
        });

        holder.itemView.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            if (listener != null) listener.onEdit(listing);
        });
        holder.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (listener != null) listener.onDelete(listing);
        });
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }
}
