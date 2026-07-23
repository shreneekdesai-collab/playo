package com.example.playo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProductPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_page);

        Button btnBookNow = findViewById(R.id.btnBookNow);
        Button btnSubmitReview = findViewById(R.id.btnSubmitReview);

        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> Toast.makeText(this, "Proceeding to Booking...", Toast.LENGTH_SHORT).show());
        }

        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> Toast.makeText(this, "Review Posted! Thank you.", Toast.LENGTH_SHORT).show());
        }
    }
}
