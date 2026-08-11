package com.example.playo;

public class Turfname {
    private String name;
    private double price;
    private int turfimg;
    private double rating;
    private String location;

    public Turfname(String name, double price, int turfimg, double rating, String location) {
        this.name = name;
        this.price = price;
        this.turfimg = turfimg;
        this.rating = rating;
        this.location = location;
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
}
