package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * RecyclerView adapter rendering the list of user bookings.
 */
public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.Binding> {

    /** Callback when a booking row is clicked. */
    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    private final ArrayList<Booking> bookings;
    private final OnBookingClickListener clickListener;

    public BookingAdapter(ArrayList<Booking> bookings, OnBookingClickListener clickListener) {
        this.bookings = bookings;
        this.clickListener = clickListener;
    }

    public static class Binding extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView turfName;
        final TextView location;
        final TextView date;
        final TextView time;
        final TextView status;
        final TextView amount;

        public Binding(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.bookingImage);
            turfName = itemView.findViewById(R.id.bookingTurfName);
            location = itemView.findViewById(R.id.bookingLocation);
            date = itemView.findViewById(R.id.bookingDate);
            time = itemView.findViewById(R.id.bookingTime);
            status = itemView.findViewById(R.id.bookingStatus);
            amount = itemView.findViewById(R.id.bookingAmount);
        }
    }

    @NonNull
    @Override
    public Binding onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new Binding(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Binding holder, int position) {
        Booking booking = bookings.get(position);

        holder.image.setImageResource(booking.getImageRes());
        holder.turfName.setText(booking.getTurfName());
        holder.location.setText(booking.getLocation());
        holder.date.setText(booking.getDate());
        holder.time.setText(booking.getTimeSlot());
        holder.amount.setText("₹" + (int) booking.getAmount());

        // Colour-code the status chip.
        int color;
        switch (booking.getStatus().toLowerCase()) {
            case "confirmed":
                color = R.color.status_confirmed;
                break;
            case "cancelled":
                color = R.color.status_cancelled;
                break;
            default:
                color = R.color.status_pending;
                break;
        }
        holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));
        holder.status.setText(booking.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onBookingClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }
}