package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * RecyclerView adapter showing a list of payment records.
 */
public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.Binding> {

    private final ArrayList<Payment> payments;
    private OnPaymentClickListener clickListener;

    public PaymentAdapter(ArrayList<Payment> payments) {
        this.payments = payments;
    }

    public PaymentAdapter(ArrayList<Payment> payments, OnPaymentClickListener clickListener) {
        this.payments = payments;
        this.clickListener = clickListener;
    }

    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
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
        PressEffect.attach(view);
        return new Binding(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Binding holder, int position) {
        Payment payment = payments.get(position);

        holder.method.setText(payment.getMethod());
        holder.amount.setText("₹" + (int) payment.getAmount());
        holder.bookingId.setText("ID: " + (payment.getPaymentId() == null
                ? payment.getBookingId() : payment.getPaymentId()));
        holder.turfName.setText(payment.getTurfName());
        String date = payment.getDate() == null ? "" : payment.getDate();
        if (payment.getSlotLabel() != null && !payment.getSlotLabel().isEmpty()) {
            date += " • " + payment.getSlotLabel();
        }
        holder.date.setText(date);
        holder.status.setText(payment.getStatus() == null ? "unknown" : payment.getStatus());

        int color = statusColor(payment.getStatus());
        holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.button_text));
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(ContextCompat.getColor(holder.itemView.getContext(), color));
        chip.setCornerRadius(24f);
        holder.status.setBackground(chip);
        holder.status.setPadding(12, 5, 12, 5);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onPaymentClick(payment);
        });
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    private int statusColor(String status) {
        if ("success".equals(status) || "paid".equalsIgnoreCase(status)) return R.color.status_success;
        if ("failed".equals(status)) return R.color.status_failed;
        if ("processing".equals(status) || "created".equals(status)) return R.color.status_processing;
        if ("needs_review".equals(status)) return R.color.status_needs_review;
        return R.color.status_neutral;
    }
}
