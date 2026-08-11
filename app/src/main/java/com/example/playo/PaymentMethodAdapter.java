package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adapter for the payment method list on the booking screen.
 * Single selection (radio behavior); selected row shows filled circle.
 */
public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.MethodViewHolder> {

    public interface OnPaymentMethodSelectedListener {
        void onPaymentMethodSelected(String method);
    }

    public static class PaymentMethod {
        public final String name;
        public final String subtitle;
        public final int iconRes;

        public PaymentMethod(String name, String subtitle, int iconRes) {
            this.name = name;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
        }
    }

    private final List<PaymentMethod> methods;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private OnPaymentMethodSelectedListener listener;

    public PaymentMethodAdapter(List<PaymentMethod> methods) {
        this.methods = methods;
    }

    public void setOnPaymentMethodSelectedListener(OnPaymentMethodSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MethodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_method, parent, false);
        return new MethodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MethodViewHolder holder, int position) {
        PaymentMethod method = methods.get(position);
        holder.paymentName.setText(method.name);
        holder.paymentSubtitle.setText(method.subtitle);
        holder.paymentIcon.setImageResource(method.iconRes);
        boolean selected = position == selectedPosition;
        holder.itemView.setSelected(selected);
        holder.radioCircle.setSelected(selected);

        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (previous != RecyclerView.NO_POSITION) {
                notifyItemChanged(previous);
            }
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onPaymentMethodSelected(getSelectedMethod());
            }
        });
    }

    @Override
    public int getItemCount() {
        return methods.size();
    }

    /** Returns the currently selected payment method name, or null if none selected. */
    public String getSelectedMethod() {
        if (selectedPosition == RecyclerView.NO_POSITION) {
            return null;
        }
        return methods.get(selectedPosition).name;
    }

    static class MethodViewHolder extends RecyclerView.ViewHolder {
        ImageView paymentIcon;
        TextView paymentName;
        TextView paymentSubtitle;
        View radioCircle;
        MethodViewHolder(View view) {
            super(view);
            paymentIcon = view.findViewById(R.id.paymentIcon);
            paymentName = view.findViewById(R.id.paymentName);
            paymentSubtitle = view.findViewById(R.id.paymentSubtitle);
            radioCircle = view.findViewById(R.id.radioCircle);
        }
    }
}