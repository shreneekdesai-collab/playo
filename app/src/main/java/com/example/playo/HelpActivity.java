package com.example.playo;

/**
 * Placeholder Help & Support screen. Shows a "coming soon" message.
 */
public class HelpActivity extends SimpleInfoActivity {

    @Override
    protected String getScreenTitle() {
        return getString(R.string.help_support);
    }

    @Override
    protected String getScreenMessage() {
        return getString(R.string.help_msg);
    }

    @Override
    protected int getScreenIcon() {
        return R.drawable.ic_help;
    }
}