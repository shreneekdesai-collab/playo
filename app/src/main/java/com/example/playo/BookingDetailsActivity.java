package com.example.playo;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Shows the full details of a selected booking.
 * Receives the booking data through the intent extras.
 */
public class BookingDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- Read intent extras ---
        String turfName = getIntent().getStringExtra("turf_name");
        String location = getIntent().getStringExtra("location");
        String date = getIntent().getStringExtra("date");
        String time = getIntent().getStringExtra("time");
        String status = getIntent().getStringExtra("status");
        double amount = getIntent().getDoubleExtra("amount", 0);
        int image = getIntent().getIntExtra("image", R.drawable.user);

        // --- Bind views ---
        ImageView detailsImage = findViewById(R.id.detailsImage);
        TextView detailsTurfName = findViewById(R.id.detailsTurfName);
        TextView detailsLocation = findViewById(R.id.detailsLocation);
        TextView detailsDate = findViewById(R.id.detailsDate);
        TextView detailsTime = findViewById(R.id.detailsTime);
        TextView detailsStatus = findViewById(R.id.detailsStatus);
        TextView detailsAmount = findViewById(R.id.detailsAmount);

        detailsImage.setImageResource(image);
        detailsTurfName.setText(turfName);
        detailsLocation.setText(location);
        detailsDate.setText(date);
        detailsTime.setText(time);
        detailsStatus.setText(status);
        detailsAmount.setText("₹" + (int) amount);

        // Colour-code the status.
        int color;
        if ("confirmed".equalsIgnoreCase(status)) {
            color = R.color.status_confirmed;
        } else if ("cancelled".equalsIgnoreCase(status)) {
            color = R.color.status_cancelled;
        } else {
            color = R.color.status_pending;
        }
        detailsStatus.setTextColor(ContextCompat.getColor(this, color));
    }
}