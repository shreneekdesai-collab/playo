package com.example.playo;

/**
 * Model representing a single payment record.
 */
public class Payment {

    private final String bookingId;
    private final String turfName;
    private final String date;
    private final String method;
    private final double amount;
    private final String status;

    public Payment(String bookingId, String turfName, String date, String method,
                   double amount, String status) {
        this.bookingId = bookingId;
        this.turfName = turfName;
        this.date = date;
        this.method = method;
        this.amount = amount;
        this.status = status;
    }

    public String getBookingId() { return bookingId; }
    public String getTurfName() { return turfName; }
    public String getDate() { return date; }
    public String getMethod() { return method; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
}