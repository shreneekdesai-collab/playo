package com.example.playo;

import com.google.firebase.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shared slot timing and deterministic slot-id rules. */
public final class SlotConfig {
    public static final long HOLD_MINUTES = 5;
    public static final long HOLD_MILLIS = HOLD_MINUTES * 60L * 1000L;
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private SlotConfig() {
    }

    public static final class Slot {
        private final String key;
        private final String label;

        public Slot(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }
    }

    public static List<Slot> hourlySlots() {
        List<Slot> slots = new ArrayList<>();
        Calendar time = Calendar.getInstance();
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        for (int hour = 6; hour <= 22; hour++) {
            time.set(Calendar.HOUR_OF_DAY, hour);
            slots.add(new Slot(String.format(Locale.US, "%02d00", hour),
                    new SimpleDateFormat("h:mm a", Locale.US).format(time.getTime())));
        }
        return slots;
    }

    public static String slotId(String date, String slotKey) {
        return date + "_" + slotKey;
    }

    public static boolean isExpired(Timestamp heldAt, long nowMillis) {
        return heldAt != null && heldAt.toDate().getTime() + HOLD_MILLIS < nowMillis;
    }

    public static boolean isPast(String date, String slotKey, long nowMillis) {
        try {
            Date slotDate = new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
                    .parse(date + "_" + slotKey);
            return slotDate != null && slotDate.getTime() <= nowMillis;
        } catch (ParseException exception) {
            return false;
        }
    }

    public static String formatDate(Calendar calendar) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.US).format(calendar.getTime());
    }
}
