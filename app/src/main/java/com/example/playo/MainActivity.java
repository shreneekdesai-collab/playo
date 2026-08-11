package com.example.playo;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Host activity for the bottom navigation tabs (Home / Category / Profile).
 * Handles deep links into a specific tab and back-navigation between tabs.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SHOW_FRAGMENT = "SHOW_FRAGMENT";
    public static final String FRAGMENT_CATEGORY = "CATEGORY";
    public static final String FRAGMENT_PROFILE = "PROFILE";

    private BottomNavigationView bottomView;
    private int currentTabId = R.id.Home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Reserve the system bar insets for the edge-to-edge layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomView = findViewById(R.id.bottomView);

        // --- Initial tab selection (supports deep links from other screens) ---
        if (savedInstanceState == null) {
            String showFragment = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
            if (FRAGMENT_CATEGORY.equals(showFragment)) {
                currentTabId = R.id.Category;
                replaceFragment(new CategoryFragment());
                bottomView.setSelectedItemId(R.id.Category);
            } else if (FRAGMENT_PROFILE.equals(showFragment)) {
                currentTabId = R.id.Profile;
                replaceFragment(new ProfileFragment());
                bottomView.setSelectedItemId(R.id.Profile);
            } else {
                currentTabId = R.id.Home;
                replaceFragment(new HomeFragment());
            }
        }

        // --- Bottom navigation selection ---
        bottomView.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.Home) {
                currentTabId = R.id.Home;
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.Category) {
                currentTabId = R.id.Category;
                replaceFragment(new CategoryFragment());
                return true;
            } else if (itemId == R.id.Profile) {
                currentTabId = R.id.Profile;
                replaceFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // --- Back button: return to Home first instead of exiting abruptly ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentTabId != R.id.Home) {
                    currentTabId = R.id.Home;
                    bottomView.setSelectedItemId(R.id.Home);
                    replaceFragment(new HomeFragment());
                } else {
                    // Already on Home: let the app finish normally.
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /** Swaps the fragment shown inside the fragment container. */
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}