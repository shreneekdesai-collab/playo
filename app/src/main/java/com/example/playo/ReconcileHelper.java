package com.example.playo;

import android.content.Context;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;

/** Best-effort client reconciliation for payment attempts without a server worker. */
public final class ReconcileHelper {
    private static boolean ranThisSession;

    private ReconcileHelper() { }

    public static void runOnce(Context context) {
        if (ranThisSession) return;
        ranThisSession = true;
        run(context);
    }

    public static void run(Context context) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("payments").whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    long now = System.currentTimeMillis();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Payment payment = PaymentRepository.fromDocument(doc);
                        Timestamp updated = payment.getUpdatedAt() == null
                                ? payment.getCreatedAt() : payment.getUpdatedAt();
                        if (updated == null) continue;
                        long age = now - updated.toDate().getTime();
                        if ("processing".equals(payment.getStatus()) && age > 10 * 60_000L) {
                            markTimeout(payment);
                        } else if ("success".equals(payment.getStatus())
                                && payment.getBookingId() == null && age > 2 * 60_000L) {
                            retrySuccess(context, payment);
                        }
                    }
                }).addOnFailureListener(error -> ErrorLogger.log(context, "Reconcile", "load_payments", error));
    }

    private static void markTimeout(Payment payment) {
        PaymentRepository repository = new PaymentRepository();
        repository.updateStatus(payment.getPaymentId(), "timeout", "TIMEOUT",
                "Payment confirmation timed out", new PaymentRepository.Callback<Void>() {
                    @Override public void onSuccess(Void value) {
                        repository.addEvent(payment.getPaymentId(), "marked_timeout", "Payment marked timed out");
                        repository.addEvent(payment.getPaymentId(), "reconcile_checked", "Reconciliation checked this payment");
                    }
                    @Override public void onFailure(Exception error) { }
                });
    }

    private static void retrySuccess(Context context, Payment payment) {
        PaymentRepository repository = new PaymentRepository();
        repository.addEvent(payment.getPaymentId(), "reconcile_checked", "Reconciliation checked this payment");
        PaymentRequest request = new PaymentRequest(payment.getPaymentId(), payment.getUserId(),
                payment.getOwnerId(), payment.getTurfId(), payment.getTurfName(), payment.getDate(),
                payment.getSlotKey(), payment.getSlotLabel(), payment.getAmount(), payment.getMethod(),
                payment.getHoldSlotId(), "Player");
        new BookingService().finalizeBooking(request, payment.getGatewayPaymentId(), new BookingService.Callback() {
            @Override public void onSuccess(String bookingId) { }
            @Override public void onFailure(String code, String message, Exception cause) {
                repository.updateStatus(payment.getPaymentId(), "needs_review", code, message,
                        new PaymentRepository.Callback<Void>() {
                            @Override public void onSuccess(Void value) { }
                            @Override public void onFailure(Exception error) { }
                        });
                ErrorLogger.log(context, "Reconcile", "finalize_payment", cause);
            }
        });
    }
}
