package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * RecyclerView adapter showing a list of payment records.
 */
public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.Binding> {

    private final ArrayList<Payment> payments;

    public PaymentAdapter(ArrayList<Payment> payments) {
        this.payments = payments;
    }

    public static class Binding extends RecyclerView.ViewHolder {
        final TextView method;
        final TextView amount;
        final TextView bookingId;
        final TextView turfName;
        final TextView date;
        final TextView status;

        Binding(@NonNull View itemView) {
            super(itemView);
            method = itemView.findViewById(R.id.paymentMethod);
            amount = itemView.findViewById(R.id.paymentAmount);
            bookingId = itemView.findViewById(R.id.paymentBookingId);
            turfName = itemView.findViewById(R.id.paymentTurfName);
            date = itemView.findViewById(R.id.paymentDate);
            status = itemView.findViewById(R.id.paymentStatus);
        }
    }

    @NonNull
    @Override
    public Binding onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment, parent, false);
        return new Binding(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Binding holder, int position) {
        Payment payment = payments.get(position);

        holder.method.setText(payment.getMethod());
        holder.amount.setText("₹" + (int) payment.getAmount());
        holder.bookingId.setText("ID: " + payment.getBookingId());
        holder.turfName.setText(payment.getTurfName());
        holder.date.setText(payment.getDate());
        holder.status.setText(payment.getStatus());

        // Colour-code payment status.
        int color;
        if ("paid".equalsIgnoreCase(payment.getStatus())) {
            color = R.color.status_confirmed;
        } else {
            color = R.color.status_pending;
        }
        holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }
}