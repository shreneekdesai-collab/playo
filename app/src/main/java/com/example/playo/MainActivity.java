package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

            Intent intent = new Intent(MainActivity.this, product_page.class);
            intent.putExtra("TURF_NAME", selectedTurf.getName());
            intent.putExtra("TURF_PRICE", "₹ " + selectedTurf.getPrice() + " / Hour");
            intent.putExtra("TURF_IMAGE", selectedTurf.getTurfimg());
            intent.putExtra("TURF_RATING", (float) selectedTurf.getRating());

            startActivity(intent);
        });
        BottomNavigationView bottom=findViewById(R.id.bottomView);
        bottom.setOnItemSelectedListener(menuItem -> {
            int itemid= menuItem.getItemId();
            if (itemid==R.id.Home){
                Toast.makeText(MainActivity.this,"home clicked",Toast.LENGTH_SHORT).show();
                return true;}
//            if (itemid == R.id.Category) {
//                startActivity(new Intent(MainActivity.this, CategoryActivity.class));
//                return true;
//            }
//
//            if (itemid == R.id.Profile) {
//                startActivity(new Intent(MainActivity.this, AccountActivity.class));
//                return true;
//            }

            return false;
        });
    }


}