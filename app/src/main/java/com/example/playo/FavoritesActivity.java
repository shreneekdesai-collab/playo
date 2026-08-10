package com.example.playo;

/**
 * Placeholder Favorites screen. Shows a "coming soon" message.
 */
public class FavoritesActivity extends SimpleInfoActivity {

    @Override
    protected String getScreenTitle() {
        return getString(R.string.favorites);
    }

    @Override
    protected String getScreenMessage() {
        return getString(R.string.favorites_msg);
    }

    @Override
    protected int getScreenIcon() {
        return R.drawable.ic_favorite;
    }
}