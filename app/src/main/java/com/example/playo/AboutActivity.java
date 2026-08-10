package com.example.playo;

/**
 * About screen. Shows a short app description.
 */
public class AboutActivity extends SimpleInfoActivity {

    @Override
    protected String getScreenTitle() {
        return getString(R.string.about);
    }

    @Override
    protected String getScreenMessage() {
        return getString(R.string.about_msg);
    }

    @Override
    protected int getScreenIcon() {
        return R.drawable.ic_info;
    }
}