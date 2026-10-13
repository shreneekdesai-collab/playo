package com.example.playo;

import com.google.firebase.Timestamp;

/** A payment attempt. It intentionally contains no payment credentials. */
public class Payment {
    private final String paymentId;
    private final String userId;
    private final String ownerId;
    private final String turfId;
    private final String turfName;
    private final String date;
    private final String slotKey;
    private final String slotLabel;
    private final double amount;
    private final String currency;
    private final String method;
    private final int attempt;
    private final String holdSlotId;
    private final String status;
    private final String bookingId;
    private final String gatewayName;
    private final String gatewayOrderId;
    private final String gatewayPaymentId;
    private final String failureCode;
    private final String failureMessage;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;
    private final Timestamp completedAt;

    public Payment(String paymentId, String userId, String ownerId, String turfId,
                   String turfName, String date, String slotKey, String slotLabel,
                   double amount, String currency, String method, int attempt,
                   String holdSlotId, String status, String bookingId,
                   String gatewayName, String gatewayOrderId, String gatewayPaymentId,
                   String failureCode, String failureMessage, Timestamp createdAt,
                   Timestamp updatedAt, Timestamp completedAt) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.ownerId = ownerId;
        this.turfId = turfId;
        this.turfName = turfName;
        this.date = date;
        this.slotKey = slotKey;
        this.slotLabel = slotLabel;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.attempt = attempt;
        this.holdSlotId = holdSlotId;
        this.status = status;
        this.bookingId = bookingId;
        this.gatewayName = gatewayName;
        this.gatewayOrderId = gatewayOrderId;
        this.gatewayPaymentId = gatewayPaymentId;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    /** Legacy constructor retained for adapter/source compatibility. */
    public Payment(String bookingId, String turfName, String date, String method,
                   double amount, String status) {
        this(null, null, null, null, turfName, date, null, null, amount, "INR", method,
                1, null, status, bookingId, null, null, null, null, null,
                null, null, null);
    }

    public String getPaymentId() { return paymentId; }
    public String getUserId() { return userId; }
    public String getOwnerId() { return ownerId; }
    public String getTurfId() { return turfId; }
    public String getTurfName() { return turfName; }
    public String getDate() { return date; }
    public String getSlotKey() { return slotKey; }
    public String getSlotLabel() { return slotLabel; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public int getAttempt() { return attempt; }
    public String getHoldSlotId() { return holdSlotId; }
    public String getStatus() { return status; }
    public String getBookingId() { return bookingId; }
    public String getGatewayName() { return gatewayName; }
    public String getGatewayOrderId() { return gatewayOrderId; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public Timestamp getCompletedAt() { return completedAt; }
}
