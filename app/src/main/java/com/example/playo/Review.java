package com.example.playo;

import com.google.firebase.Timestamp;

/** A review for one turf, uniquely identified by the reviewer's user ID. */
public class Review {
    private final String userId;
    private final String userName;
    private final float rating;
    private final String comment;
    private final Timestamp createdAt;

    public Review(String userId, String userName, float rating, String comment, Timestamp createdAt) {
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public float getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
