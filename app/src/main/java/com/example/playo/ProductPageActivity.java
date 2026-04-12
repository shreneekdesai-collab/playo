package com.example.playo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ProductPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_page);

        // 1. Setup Toolbar with Back Button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); 
        }

        // Handle Back Navigation
        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Setup Buttons using IDs from your XML
        Button btnBookNow = findViewById(R.id.btnBookNow);
        Button btnSubmitReview = findViewById(R.id.btnSubmitReview);

        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                Toast.makeText(this, "Proceeding to Booking...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> {
                Toast.makeText(this, "Review Posted! Thank you.", Toast.LENGTH_SHORT).show();
            });
        }
    }
}