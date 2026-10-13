package com.example.playo;

/** Immutable data needed to create and finalize one payment attempt. */
public class PaymentRequest {
    public final String paymentId;
    public final String userId;
    public final String ownerId;
    public final String turfId;
    public final String turfName;
    public final String date;
    public final String slotKey;
    public final String slotLabel;
    public final double amount;
    public final String method;
    public final String holdSlotId;
    public final String userName;

    public PaymentRequest(String paymentId, String userId, String ownerId, String turfId,
                          String turfName, String date, String slotKey, String slotLabel,
                          double amount, String method, String holdSlotId, String userName) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.ownerId = ownerId;
        this.turfId = turfId;
        this.turfName = turfName;
        this.date = date;
        this.slotKey = slotKey;
        this.slotLabel = slotLabel;
        this.amount = amount;
        this.method = method;
        this.holdSlotId = holdSlotId;
        this.userName = userName;
    }
}
