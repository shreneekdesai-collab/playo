package com.example.playo;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the details of a single turf, with book-now / review actions,
 * a swipeable image gallery (ViewPager2), and bottom navigation.
 */
public class ProductPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.product_page);

        // --- Status bar: light icons for dark hero image ---
        setupStatusBar();

        // --- Edge-to-edge: pad only the toolbar below the status bar,
        //     hero image stays behind the status bar ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorRoot), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            Log.d("ProductPage", "WindowInsets topInset=" + topInset);
            Toolbar toolbar = findViewById(R.id.productToolbar);
            if (toolbar != null) {
                toolbar.setPadding(toolbar.getPaddingLeft(), topInset, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            }
            return insets;
        });

        // --- Static toolbar with back arrow ---
        Toolbar toolbar = findViewById(R.id.productToolbar);
        if (toolbar != null) {
            Drawable navIcon = ContextCompat.getDrawable(this, R.drawable.ic_back);
            if (navIcon != null) {
                navIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                toolbar.setNavigationIcon(navIcon);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // --- Read details passed from the list ---
        Intent intent = getIntent();
        String name = intent.getStringExtra("TURF_NAME");
        String price = intent.getStringExtra("TURF_PRICE");
        int imageRes = intent.getIntExtra("TURF_IMAGE", R.mipmap.ic_launcher);
        float rating = intent.getFloatExtra("TURF_RATING", 0.0f);

        TextView nameView = findViewById(R.id.turfname);
        TextView priceView = findViewById(R.id.price);
        RatingBar ratingBar = findViewById(R.id.ratingBar);

        nameView.setText(name);
        priceView.setText(price);
        ratingBar.setRating(rating);

        // --- Flipkart-style image gallery (ViewPager2) ---
        setupImageGallery(imageRes);

        // --- Book now ---
        Button btnBookNow = findViewById(R.id.btnBookNow);
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                Intent bookIntent = new Intent(ProductPage.this, BookSlotActivity.class);
                bookIntent.putExtra("TURF_NAME", name);
                bookIntent.putExtra("TURF_PRICE", price);
                startActivity(bookIntent);
            });
        }

        // --- Post review ---
        Button btnSubmitReview = findViewById(R.id.btnSubmitReview);
        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v ->
                    Toast.makeText(this, "Review Posted! Thank you.", Toast.LENGTH_SHORT).show());
        }

        // --- Bottom navigation (Profile always reachable) ---
        setupBottomNavigation();
    }

    /** Sets up the ViewPager2 image gallery with dots indicator. */
    private void setupImageGallery(int primaryImageRes) {
        ViewPager2 pager = findViewById(R.id.productImagePager);
        TabLayout indicator = findViewById(R.id.imageIndicator);

        // Build image list — for now single image wrapped in list.
        // TODO: Replace with multiple images when backend provides gallery URLs.
        List<Integer> images = new ArrayList<>();
        images.add(primaryImageRes);

        ProductImagePagerAdapter adapter = new ProductImagePagerAdapter(images);
        pager.setAdapter(adapter);

        // Attach dots indicator via TabLayoutMediator
        new TabLayoutMediator(indicator, pager, (tab, position) -> {
            // TabLayoutMediator handles selection automatically; no title needed for dots
        }).attach();
    }

    private void setupStatusBar() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    /** Sets up the bottom navigation to switch to other main tabs. */
    private void setupBottomNavigation() {
        BottomNavigationView bottomView = findViewById(R.id.productBottomView);
        if (bottomView == null) {
            return;
        }
        bottomView.setSelectedItemId(R.id.Category);

        bottomView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = new Intent(ProductPage.this, MainActivity.class);
            if (itemId == R.id.Home) {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.Category) {
                intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, MainActivity.FRAGMENT_CATEGORY);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.Profile) {
                intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, MainActivity.FRAGMENT_PROFILE);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}