package com.example.playo;

import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;

/** Firestore persistence for payment attempts and their audit events. */
public class PaymentRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onFailure(@NonNull Exception error);
    }

    private final FirebaseFirestore firestore;

    public PaymentRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void createAttempt(PaymentRequest request, Callback<Payment> callback) {
        firestore.collection("payments").whereEqualTo("userId", request.userId).get()
                .addOnSuccessListener(snapshot -> {
                    int attempt = 1;
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        if (request.holdSlotId.equals(document.getString("holdSlotId"))) {
                            Number oldAttempt = document.getLong("attempt");
                            attempt = Math.max(attempt, oldAttempt == null ? 1 : oldAttempt.intValue() + 1);
                        }
                    }
                    String paymentId = firestore.collection("payments").document().getId();
                    Map<String, Object> data = new HashMap<>();
                    data.put("paymentId", paymentId);
                    data.put("userId", request.userId);
                    data.put("ownerId", request.ownerId == null ? "" : request.ownerId);
                    data.put("turfId", request.turfId);
                    data.put("turfName", request.turfName == null ? "Turf" : request.turfName);
                    data.put("date", request.date);
                    data.put("slotKey", request.slotKey);
                    data.put("slotLabel", request.slotLabel);
                    data.put("amount", request.amount);
                    data.put("currency", "INR");
                    data.put("method", request.method);
                    data.put("attempt", attempt);
                    data.put("holdSlotId", request.holdSlotId);
                    data.put("status", "created");
                    data.put("bookingId", null);
                    data.put("gatewayName", "simulated");
                    data.put("gatewayOrderId", null);
                    data.put("gatewayPaymentId", null);
                    data.put("failureCode", null);
                    data.put("failureMessage", null);
                    data.put("createdAt", FieldValue.serverTimestamp());
                    data.put("updatedAt", FieldValue.serverTimestamp());
                    data.put("completedAt", null);
                    DocumentReference ref = firestore.collection("payments").document(paymentId);
                    ref.set(data).addOnSuccessListener(unused -> addEvent(paymentId, "pay_tapped",
                            "Payment attempt created", new Callback<Void>() {
                                @Override public void onSuccess(Void value) {
                                    callback.onSuccess(loadPayment(data, paymentId));
                                }
                                @Override public void onFailure(@NonNull Exception error) {
                                    callback.onFailure(error);
                                }
                            })).addOnFailureListener(callback::onFailure);
                }).addOnFailureListener(callback::onFailure);
    }

    public void updateStatus(String paymentId, String status, String failureCode,
                             String failureMessage, Callback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        updates.put("failureCode", failureCode);
        updates.put("failureMessage", failureMessage);
        FirebaseFirestore.getInstance().collection("payments").document(paymentId).update(updates)
                .addOnSuccessListener(unused -> addEvent(paymentId, "status_changed",
                        "Payment status changed to " + status, new Callback<Void>() {
                            @Override public void onSuccess(Void value) { callback.onSuccess(null); }
                            @Override public void onFailure(@NonNull Exception error) { callback.onSuccess(null); }
                        }))
                .addOnFailureListener(callback::onFailure);
    }

    public void addEvent(String paymentId, String type, String message, Callback<Void> callback) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("message", message == null ? "" : message);
        event.put("createdAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("payments").document(paymentId)
                .collection("events").add(event)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void addEvent(String paymentId, String type, String message) {
        addEvent(paymentId, type, message, new Callback<Void>() {
            @Override public void onSuccess(Void value) { }
            @Override public void onFailure(@NonNull Exception error) { }
        });
    }

    public static Payment fromDocument(DocumentSnapshot doc) {
        Number amount = doc.getDouble("amount");
        Number attempt = doc.getLong("attempt");
        return new Payment(doc.getString("paymentId"), doc.getString("userId"),
                doc.getString("ownerId"), doc.getString("turfId"), doc.getString("turfName"),
                doc.getString("date"), doc.getString("slotKey"), doc.getString("slotLabel"),
                amount == null ? 0 : amount.doubleValue(), doc.getString("currency"),
                doc.getString("method"), attempt == null ? 1 : attempt.intValue(),
                doc.getString("holdSlotId"), doc.getString("status"), doc.getString("bookingId"),
                doc.getString("gatewayName"), doc.getString("gatewayOrderId"),
                doc.getString("gatewayPaymentId"), doc.getString("failureCode"),
                doc.getString("failureMessage"), doc.getTimestamp("createdAt"),
                doc.getTimestamp("updatedAt"), doc.getTimestamp("completedAt"));
    }

    private Payment loadPayment(Map<String, Object> data, String paymentId) {
        Number amount = (Number) data.get("amount");
        Number attempt = (Number) data.get("attempt");
        return new Payment(paymentId, (String) data.get("userId"), (String) data.get("ownerId"),
                (String) data.get("turfId"), (String) data.get("turfName"), (String) data.get("date"),
                (String) data.get("slotKey"), (String) data.get("slotLabel"), amount.doubleValue(),
                "INR", (String) data.get("method"), attempt.intValue(), (String) data.get("holdSlotId"),
                "created", null, "simulated", null, null, null, null, null, null, null);
    }
}
