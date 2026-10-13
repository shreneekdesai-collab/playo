package com.example.playo;

import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.HashMap;
import java.util.Map;

/** Performs the booking, slot, and successful-payment writes atomically. */
public class BookingService {
    public interface Callback {
        void onSuccess(String bookingId);
        void onFailure(String code, String message, Exception cause);
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    public void finalizeBooking(PaymentRequest request, String gatewayPaymentId, Callback callback) {
        DocumentReference slotRef = firestore.collection("turfs").document(request.turfId)
                .collection("slots").document(request.holdSlotId);
        DocumentReference bookingRef = firestore.collection("bookings").document();
        DocumentReference paymentRef = firestore.collection("payments").document(request.paymentId);
        String bookingId = bookingRef.getId();
        firestore.runTransaction(transaction -> {
            DocumentSnapshot slot = transaction.get(slotRef);
            if (!slot.exists()) {
                throw new FinalizeException("HOLD_EXPIRED", "The booking hold has expired.");
            }
            Timestamp heldAt = slot.getTimestamp("heldAt");
            String heldBy = slot.getString("heldBy");
            String status = slot.getString("status");
            if (!request.userId.equals(heldBy) || !"held".equals(status)) {
                throw new FinalizeException("SLOT_TAKEN", "The selected slot is no longer available.");
            }
            if (SlotConfig.isExpired(heldAt, System.currentTimeMillis())) {
                throw new FinalizeException("HOLD_EXPIRED", "The booking hold has expired.");
            }
            Map<String, Object> booking = new HashMap<>();
            booking.put("bookingId", bookingId);
            booking.put("paymentId", request.paymentId);
            booking.put("userId", request.userId);
            booking.put("userName", request.userName == null ? "Player" : request.userName);
            booking.put("turfId", request.turfId);
            booking.put("turfName", request.turfName == null ? "Turf" : request.turfName);
            booking.put("ownerId", request.ownerId == null ? "" : request.ownerId);
            booking.put("date", request.date);
            booking.put("slotKey", request.slotKey);
            booking.put("slotLabel", request.slotLabel);
            booking.put("amount", request.amount);
            booking.put("paymentMethod", request.method);
            booking.put("paymentStatus", "paid_test");
            booking.put("status", "confirmed");
            booking.put("createdAt", FieldValue.serverTimestamp());
            transaction.set(bookingRef, booking);

            Map<String, Object> bookedSlot = new HashMap<>();
            bookedSlot.put("status", "booked");
            bookedSlot.put("bookingId", bookingId);
            bookedSlot.put("heldBy", request.userId);
            transaction.update(slotRef, bookedSlot);

            Map<String, Object> payment = new HashMap<>();
            payment.put("status", "success");
            payment.put("bookingId", bookingId);
            payment.put("gatewayPaymentId", gatewayPaymentId);
            payment.put("failureCode", null);
            payment.put("failureMessage", null);
            payment.put("updatedAt", FieldValue.serverTimestamp());
            payment.put("completedAt", FieldValue.serverTimestamp());
            transaction.update(paymentRef, payment);
            return bookingId;
        }).addOnSuccessListener(id -> {
            paymentRepository.addEvent(request.paymentId, "booking_confirmed",
                    "Booking " + id + " confirmed");
            callback.onSuccess(id);
        }).addOnFailureListener(error -> {
            String code = mapFailureCode(error);
            callback.onFailure(code, friendlyMessage(code), error);
        });
    }

    public static String mapFailureCode(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof FinalizeException) {
                return ((FinalizeException) current).code;
            }
            if (current instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) current).getCode();
                if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return "PERMISSION_DENIED";
                if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                        || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) return "NETWORK";
            }
            current = current.getCause();
        }
        return "BOOKING_WRITE_FAILED";
    }

    public static String friendlyMessage(String code) {
        if ("HOLD_EXPIRED".equals(code)) return "Your payment was received, but the booking hold expired.";
        if ("SLOT_TAKEN".equals(code)) return "That slot is no longer available.";
        if ("NETWORK".equals(code)) return "A network problem prevented confirmation.";
        if ("PERMISSION_DENIED".equals(code)) return "The booking could not be confirmed right now.";
        if ("GATEWAY_DECLINED".equals(code)) return "The payment was declined.";
        if ("USER_CANCELLED".equals(code)) return "Payment was cancelled.";
        if ("TIMEOUT".equals(code)) return "The payment took too long to complete.";
        return "The booking could not be confirmed right now.";
    }

    private static class FinalizeException extends RuntimeException {
        final String code;
        FinalizeException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
