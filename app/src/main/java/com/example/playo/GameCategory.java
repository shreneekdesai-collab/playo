package com.example.playo;

public class GameCategory {
    private String name;
    private int imageResId;

    public GameCategory(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}
