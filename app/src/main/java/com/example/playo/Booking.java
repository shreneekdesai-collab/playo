package com.example.playo;

import com.google.firebase.Timestamp;

/** Model for a confirmed Firestore booking. */
public class Booking {
    private final String bookingId;
    private final String userId;
    private final String userName;
    private final String turfId;
    private final String turfName;
    private final String ownerId;
    private final String location;
    private final String date;
    private final String slotKey;
    private final String timeSlot;
    private final double amount;
    private final String paymentMethod;
    private final String paymentStatus;
    private final String status;
    private final Timestamp createdAt;
    private final int imageRes;

    public Booking(String bookingId, String userId, String userName, String turfId,
                   String turfName, String ownerId, String location, String date,
                   String slotKey, String timeSlot, double amount, String paymentMethod,
                   String paymentStatus, String status, Timestamp createdAt, int imageRes) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.userName = userName;
        this.turfId = turfId;
        this.turfName = turfName;
        this.ownerId = ownerId;
        this.location = location;
        this.date = date;
        this.slotKey = slotKey;
        this.timeSlot = timeSlot;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.status = status;
        this.createdAt = createdAt;
        this.imageRes = imageRes;
    }

    public String getBookingId() { return bookingId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getTurfId() { return turfId; }
    public String getTurfName() { return turfName; }
    public String getOwnerId() { return ownerId; }
    public String getLocation() { return location; }
    public String getDate() { return date; }
    public String getSlotKey() { return slotKey; }
    public String getTimeSlot() { return timeSlot; }
    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getImageRes() { return imageRes; }
}
