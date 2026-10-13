package com.example.playo;

public class Turfname {
    private String name;
    private double price;
    private int turfimg;
    private double rating;
    private String location;
    private String sport;
    private String turfId;
    private String imageUrl;
    private byte[] thumbnailBytes;

    public Turfname(String name, double price, int turfimg, double rating, String location,
                    String sport, String turfId, String imageUrl) {
        this.name = name;
        this.price = price;
        this.turfimg = turfimg;
        this.rating = rating;
        this.location = location;
        this.sport = sport;
        this.turfId = turfId;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getTurfimg() {
        return turfimg;
    }

    public void setTurfimg(int turfimg) {
        this.turfimg = turfimg;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getTurfId() {
        return turfId;
    }

    public void setTurfId(String turfId) {
        this.turfId = turfId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public byte[] getThumbnailBytes() {
        return thumbnailBytes;
    }

    public void setThumbnailBytes(byte[] thumbnailBytes) {
        this.thumbnailBytes = thumbnailBytes;
    }
}
