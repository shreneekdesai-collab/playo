package com.example.playo;

/**
 * Model representing a single turf booking.
 */
public class Booking {

    private final String turfName;
    private final String location;
    private final String date;
    private final String timeSlot;
    private final String status;
    private final double amount;
    private final int imageRes;

    public Booking(String turfName, String location, String date, String timeSlot,
                   String status, double amount, int imageRes) {
        this.turfName = turfName;
        this.location = location;
        this.date = date;
        this.timeSlot = timeSlot;
        this.status = status;
        this.amount = amount;
        this.imageRes = imageRes;
    }

    public String getTurfName() { return turfName; }
    public String getLocation() { return location; }
    public String getDate() { return date; }
    public String getTimeSlot() { return timeSlot; }
    public String getStatus() { return status; }
    public double getAmount() { return amount; }
    public int getImageRes() { return imageRes; }
}