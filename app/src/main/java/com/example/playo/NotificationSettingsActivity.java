package com.example.playo;

/**
 * Placeholder Notification Settings screen. Shows a "coming soon" message.
 */
public class NotificationSettingsActivity extends SimpleInfoActivity {

    @Override
    protected String getScreenTitle() {
        return getString(R.string.notification_settings);
    }

    @Override
    protected String getScreenMessage() {
        return getString(R.string.notifications_msg);
    }

    @Override
    protected int getScreenIcon() {
        return R.drawable.ic_notifications;
    }
}