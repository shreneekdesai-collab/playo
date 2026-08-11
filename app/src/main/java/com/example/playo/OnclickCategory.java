package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;

public class OnclickCategory extends AppCompatActivity {

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
        turfList.add(new Turfname("Gully Cricket Turf", 500.0, R.mipmap.ic_launcher, 4.5, "Andheri West, Mumbai"));
        turfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8, "Bandra East, Mumbai"));
        turfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8, "Powai, Mumbai"));

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        CustomAdapter adapter = new CustomAdapter(turfList, turfname -> {
            Intent intent = new Intent(OnclickCategory.this, ProductPage.class);
            intent.putExtra("TURF_NAME", turfname.getName());
            intent.putExtra("TURF_PRICE", "₹ " + turfname.getPrice() + " / Hour");
            intent.putExtra("TURF_IMAGE", turfname.getTurfimg());
            intent.putExtra("TURF_RATING", (float) turfname.getRating());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        BottomNavigationView bottom = findViewById(R.id.bottomView);
        bottom.setSelectedItemId(R.id.Category);

        bottom.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.Home) {
                Intent intent = new Intent(OnclickCategory.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.Category) {
                Intent intent = new Intent(OnclickCategory.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, MainActivity.FRAGMENT_CATEGORY);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.Profile) {
                Intent intent = new Intent(OnclickCategory.this, MainActivity.class);
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
