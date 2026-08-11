package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adapter for the time-slot grid on the booking screen.
 * Supports single selection; selected slot turns black.
 */
public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.SlotViewHolder> {

    public interface OnSlotSelectedListener {
        void onSlotSelected(String slot);
    }

    private final List<String> slotList;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private OnSlotSelectedListener listener;

    public TimeSlotAdapter(List<String> slotList) {
        this.slotList = slotList;
    }

    public void setOnSlotSelectedListener(OnSlotSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        String slot = slotList.get(position);
        holder.slotTimeText.setText(slot);
        holder.itemView.setSelected(position == selectedPosition);
        holder.itemView.setEnabled(true);

        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (previous != RecyclerView.NO_POSITION) {
                notifyItemChanged(previous);
            }
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onSlotSelected(getSelectedSlot());
            }
        });
    }

    @Override
    public int getItemCount() {
        return slotList.size();
    }

    /** Returns the currently selected slot label, or null if none selected. */
    public String getSelectedSlot() {
        if (selectedPosition == RecyclerView.NO_POSITION) {
            return null;
        }
        return slotList.get(selectedPosition);
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView slotTimeText;
        SlotViewHolder(View view) {
            super(view);
            slotTimeText = view.findViewById(R.id.slotTimeText);
        }
    }
}