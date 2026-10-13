package com.example.playo;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/** Creates user support tickets without accepting payment credentials. */
public class SupportRepository {
    public void createTicket(String message, String paymentId, String bookingId) {
        createTicket(message, paymentId, bookingId, new PaymentRepository.Callback<Void>() {
            @Override public void onSuccess(Void value) { }
            @Override public void onFailure(Exception error) { }
        });
    }

    public void createTicket(String message, String paymentId, String bookingId,
                             PaymentRepository.Callback<Void> callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            callback.onFailure(new IllegalStateException("Not signed in"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        if (paymentId != null) data.put("paymentId", paymentId);
        if (bookingId != null) data.put("bookingId", bookingId);
        String cleanMessage = message == null ? "" : message.trim();
        data.put("message", cleanMessage.length() > 500 ? cleanMessage.substring(0, 500) : cleanMessage);
        data.put("status", "open");
        data.put("createdAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("supportTickets").add(data)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
