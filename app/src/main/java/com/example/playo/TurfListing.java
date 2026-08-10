package com.example.playo;

/**
 * Model representing a single turf listing uploaded by the user.
 */
public class TurfListing {

    private final String turfName;
    private final String location;
    private final double price;
    private final boolean available;
    private final float rating;
    private final int totalBookings;
    private final int imageRes;

    public TurfListing(String turfName, String location, double price, boolean available,
                       float rating, int totalBookings, int imageRes) {
        this.turfName = turfName;
        this.location = location;
        this.price = price;
        this.available = available;
        this.rating = rating;
        this.totalBookings = totalBookings;
        this.imageRes = imageRes;
    }

    public String getTurfName() { return turfName; }
    public String getLocation() { return location; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return available; }
    public float getRating() { return rating; }
    public int getTotalBookings() { return totalBookings; }
    public int getImageRes() { return imageRes; }
}