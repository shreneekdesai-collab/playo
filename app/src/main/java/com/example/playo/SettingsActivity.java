package com.example.playo;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Settings screen with appearance options (dark mode) and legal links.
 * Preferences are stored in SharedPreferences.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "playo_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- Edge-to-edge: keep the toolbar below the status bar ---
        final int baseToolbarHeight = toolbar.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topInset = systemBars.top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = baseToolbarHeight + topInset;
            v.setLayoutParams(lp);
            return insets;
        });

        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        SwitchMaterial switchNotifications = findViewById(R.id.switchNotifications);

        // Restore saved states.
        boolean darkMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_DARK_MODE, false);
        boolean notifications = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_NOTIFICATIONS, true);
        switchDarkMode.setChecked(darkMode);
        switchNotifications.setChecked(notifications);

        // Dark mode toggle applies the theme at runtime.
        switchDarkMode.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_DARK_MODE, checked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Notifications toggle (placeholder).
        switchNotifications.setOnCheckedChangeListener((button, checked) ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putBoolean(KEY_NOTIFICATIONS, checked).apply());

        // Placeholder rows.
        findViewById(R.id.rowLanguage).setOnClickListener(v -> toast(getString(R.string.coming_soon)));
        findViewById(R.id.rowPrivacy).setOnClickListener(v -> toast(getString(R.string.coming_soon)));
        findViewById(R.id.rowTerms).setOnClickListener(v -> toast(getString(R.string.coming_soon)));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}