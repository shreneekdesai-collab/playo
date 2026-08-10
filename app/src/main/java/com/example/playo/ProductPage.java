package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Displays the details of a single turf, with book-now / review actions
 * and bottom navigation so the user can reach any tab (including Profile).
 */
public class ProductPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_page);

        // --- Read details passed from the list ---
        Intent intent = getIntent();
        String name = intent.getStringExtra("TURF_NAME");
        String price = intent.getStringExtra("TURF_PRICE");
        int imageRes = intent.getIntExtra("TURF_IMAGE", R.mipmap.ic_launcher);
        float rating = intent.getFloatExtra("TURF_RATING", 0.0f);

        TextView nameView = findViewById(R.id.turfname);
        TextView priceView = findViewById(R.id.price);
        ImageView imageView = findViewById(R.id.product_image);
        RatingBar ratingBar = findViewById(R.id.ratingBar);

        nameView.setText(name);
        priceView.setText(price);
        imageView.setImageResource(imageRes);
        ratingBar.setRating(rating);

        // --- Book now ---
        Button btnBookNow = findViewById(R.id.btnBookNow);
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v ->
                    Toast.makeText(this, "Proceeding to Booking...", Toast.LENGTH_SHORT).show());
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

    /** Sets up the bottom navigation to switch to other main tabs. */
    private void setupBottomNavigation() {
        BottomNavigationView bottomView = findViewById(R.id.productBottomView);
        if (bottomView == null) {
            return;
        }
        // Highlight the Category tab since this page is part of category flow.
        bottomView.setSelectedItemId(R.id.Category);

        bottomView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = new Intent(ProductPage.this, MainActivity.class);
            if (itemId == R.id.Home) {
                // no fragment extra -> Home tab
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