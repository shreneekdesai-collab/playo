package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProductPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_page);

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
    }
}
