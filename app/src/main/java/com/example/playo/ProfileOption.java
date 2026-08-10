package com.example.playo;

/**
 * Model class representing a single option shown in the Profile account menu.
 * Holds the displayed title, its icon resource, and the activity to open on click.
 */
public class ProfileOption {

    private final String title;
    private final int iconRes;
    private final Class<?> targetActivity;

    public ProfileOption(String title, int iconRes, Class<?> targetActivity) {
        this.title = title;
        this.iconRes = iconRes;
        this.targetActivity = targetActivity;
    }

    public String getTitle() {
        return title;
    }

    public int getIconRes() {
        return iconRes;
    }

    public Class<?> getTargetActivity() {
        return targetActivity;
    }
}