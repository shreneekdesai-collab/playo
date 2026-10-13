package com.example.playo;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Renders live Firestore slot states while keeping only the selection local. */
public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.SlotViewHolder> {

    public enum State { AVAILABLE, SELECTED, HELD_BY_OTHER, HELD_BY_ME, BOOKED, PAST }

    public static final class SlotData {
        public final String status;
        public final String heldBy;
        public final Timestamp heldAt;

        public SlotData(String status, String heldBy, Timestamp heldAt) {
            this.status = status;
            this.heldBy = heldBy;
            this.heldAt = heldAt;
        }
    }

    public interface OnSlotSelectedListener {
        void onSlotSelected(String slotKey);
    }

    private final List<SlotConfig.Slot> slots;
    private final String currentUserId;
    private String selectedDate;
    private String selectedSlotKey;
    private Map<String, SlotData> slotData = new HashMap<>();
    private final Map<String, State> lastStates = new HashMap<>();
    private OnSlotSelectedListener listener;

    public TimeSlotAdapter(List<SlotConfig.Slot> slots, String currentUserId, String selectedDate) {
        this.slots = slots;
        this.currentUserId = currentUserId;
        this.selectedDate = selectedDate;
    }

    public void setOnSlotSelectedListener(OnSlotSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
        clearSelection();
        lastStates.clear();
        notifyDataSetChanged();
    }

    public void setSlotData(Map<String, SlotData> slotData) {
        this.slotData = slotData == null ? new HashMap<>() : new HashMap<>(slotData);
        notifyDataSetChanged();
    }

    public void refreshStates() {
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedSlotKey = null;
    }

    public String getSelectedSlotKey() {
        return selectedSlotKey;
    }

    public String getSelectedSlotLabel() {
        for (SlotConfig.Slot slot : slots) {
            if (slot.getKey().equals(selectedSlotKey)) {
                return slot.getLabel();
            }
        }
        return null;
    }

    public boolean isSelectedSlotUnavailable() {
        if (selectedSlotKey == null) {
            return false;
        }
        return getState(selectedSlotKey) != State.SELECTED;
    }

    public State getState(String slotKey) {
        SlotData data = slotData.get(slotKey);
        long now = System.currentTimeMillis();
        if (data != null && "booked".equals(data.status)) {
            return State.BOOKED;
        }
        if (data != null && "held".equals(data.status)
                && !SlotConfig.isExpired(data.heldAt, now)) {
            return currentUserId != null && currentUserId.equals(data.heldBy)
                    ? State.HELD_BY_ME : State.HELD_BY_OTHER;
        }
        if (SlotConfig.isPast(selectedDate, slotKey, now)) {
            return State.PAST;
        }
        if (selectedSlotKey != null && selectedSlotKey.equals(slotKey)) {
            return State.SELECTED;
        }
        return State.AVAILABLE;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        PressEffect.attach(view);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        SlotConfig.Slot slot = slots.get(position);
        State state = getState(slot.getKey());
        State previousState = lastStates.put(slot.getKey(), state);
        holder.slotTimeText.setText(slot.getLabel());
        holder.slotCaption.setText(captionFor(state));
        holder.slotCaption.setVisibility(captionFor(state).isEmpty() ? View.GONE : View.VISIBLE);
        holder.itemView.setAlpha(state == State.PAST ? 0.4f : 1f);
        holder.itemView.setEnabled(isTappable(state));
        holder.itemView.setSelected(state == State.SELECTED || state == State.HELD_BY_ME);
        holder.slotTimeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                state == State.SELECTED || state == State.HELD_BY_ME
                        ? R.color.button_text : state == State.BOOKED
                        ? R.color.slot_booked_text : state == State.HELD_BY_OTHER || state == State.PAST
                        ? R.color.slot_booked_text : R.color.slot_time_color));
        holder.slotCaption.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                R.color.slot_caption));
        holder.slotTimeText.setPaintFlags(state == State.BOOKED
                ? holder.slotTimeText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : holder.slotTimeText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        int backgroundColor = backgroundColor(holder, state);
        if (previousState != null && previousState != state && Motion.enabled(holder.itemView.getContext())) {
            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                    backgroundColor(holder, previousState), backgroundColor);
            colorAnimator.setDuration(Motion.DURATION_SHORT);
            colorAnimator.addUpdateListener(value -> setBackground(holder.itemView,
                    (Integer) value.getAnimatedValue()));
            colorAnimator.start();
        } else {
            setBackground(holder.itemView, backgroundColor);
        }
        if (state == State.SELECTED && previousState != State.SELECTED) {
            AnimUtils.pulse(holder.itemView);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!isTappable(getState(slot.getKey()))) {
                return;
            }
            String previous = selectedSlotKey;
            selectedSlotKey = slot.getKey().equals(previous) ? null : slot.getKey();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onSlotSelected(selectedSlotKey);
            }
        });
    }

    private boolean isTappable(State state) {
        return state == State.AVAILABLE || state == State.SELECTED;
    }

    private int backgroundColor(SlotViewHolder holder, State state) {
        return ContextCompat.getColor(holder.itemView.getContext(),
                state == State.SELECTED || state == State.HELD_BY_ME ? R.color.button_bg
                        : state == State.BOOKED ? R.color.slot_booked_bg
                        : state == State.HELD_BY_OTHER ? R.color.slot_held_bg
                        : R.color.slot_available_bg);
    }

    private void setBackground(View view, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(8 * view.getResources().getDisplayMetrics().density);
        view.setBackground(background);
    }

    private String captionFor(State state) {
        if (state == State.HELD_BY_OTHER) return "Held";
        if (state == State.HELD_BY_ME) return "Your hold";
        if (state == State.BOOKED) return "Booked";
        return "";
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        final TextView slotTimeText;
        final TextView slotCaption;

        SlotViewHolder(View view) {
            super(view);
            slotTimeText = view.findViewById(R.id.slotTimeText);
            slotCaption = view.findViewById(R.id.slotCaption);
        }
    }
}
