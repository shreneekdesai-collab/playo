package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class Onclick_Category extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.onclick_category);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        TextView textView = findViewById(R.id.textView);
        if (categoryName != null) {
            textView.setText(categoryName);
        }

        ArrayList<Turfname> turfList = new ArrayList<>();
        turfList.add(new Turfname("Gully Cricket Turf", 500.0, R.mipmap.ic_launcher, 4.5));
        turfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8));
        turfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8));

        ListView listView = findViewById(R.id.listView);
        customeAdapter adapter = new customeAdapter(turfList, this);
        listView.setAdapter(adapter);

        // 🔥 CLICK WORKING HERE
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Turfname selectedTurf = (Turfname) parent.getItemAtPosition(position);

            Intent intent = new Intent(Onclick_Category.this, product_page.class);
            intent.putExtra("TURF_NAME", selectedTurf.getName());
            intent.putExtra("TURF_PRICE", "₹ " + selectedTurf.getPrice() + " / Hour");
            intent.putExtra("TURF_IMAGE", selectedTurf.getTurfimg());
            intent.putExtra("TURF_RATING", (float) selectedTurf.getRating());

            startActivity(intent);
        });

        // BOTTOM NAVIGATION LOGIC
        BottomNavigationView bottom = findViewById(R.id.bottomView);
        bottom.setSelectedItemId(R.id.Category); // Keep Category highlighted
        
        bottom.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.Home) {
                Intent intent = new Intent(Onclick_Category.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.Category) {
                Intent intent = new Intent(Onclick_Category.this, CategoryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}
