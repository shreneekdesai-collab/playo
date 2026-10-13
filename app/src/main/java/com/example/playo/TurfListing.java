package com.example.playo;

/**
 * Model representing a single turf listing uploaded by the user.
 */
public class TurfListing {

    private final String turfId;
    private final String turfName;
    private final String location;
    private final double price;
    private final boolean available;
    private final float rating;
    private final int totalBookings;
    private final int imageRes;
    private final String imageUrl;
    private final String sport;
    private byte[] thumbnailBytes;

    public TurfListing(String turfName, String location, double price, boolean available,
                       float rating, int totalBookings, int imageRes, String sport) {
        this(null, turfName, location, price, available, rating, totalBookings, imageRes, null, sport);
    }

    public TurfListing(String turfId, String turfName, String location, double price, boolean available,
                       float rating, int totalBookings, int imageRes, String imageUrl, String sport) {
        this.turfId = turfId;
        this.turfName = turfName;
        this.location = location;
        this.price = price;
        this.available = available;
        this.rating = rating;
        this.totalBookings = totalBookings;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl;
        this.sport = sport;
    }

    public String getTurfId() { return turfId; }
    public String getTurfName() { return turfName; }
    public String getLocation() { return location; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return available; }
    public float getRating() { return rating; }
    public int getTotalBookings() { return totalBookings; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
    public String getSport() { return sport; }
    public byte[] getThumbnailBytes() { return thumbnailBytes; }
    public void setThumbnailBytes(byte[] thumbnailBytes) { this.thumbnailBytes = thumbnailBytes; }
}
