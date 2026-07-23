package com.example.playo;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomView = findViewById(R.id.bottomView);

        if (savedInstanceState == null) {
            String showFragment = getIntent().getStringExtra("SHOW_FRAGMENT");
            if ("CATEGORY".equals(showFragment)) {
                replaceFragment(new CategoryFragment());
                bottomView.setSelectedItemId(R.id.Category);
            } else {
                replaceFragment(new HomeFragment());
            }
        }

        bottomView.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.Home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.Category) {
                replaceFragment(new CategoryFragment());
                return true;
            } else if (itemId == R.id.Profile) {
                replaceFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
