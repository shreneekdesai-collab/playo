package com.example.cut_cut;

import android.os.Bundle;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

/**
 * ZOMATO-LIKE SCROLLING:
 * We've moved the "Welcome" vanish logic from Java to XML using CoordinatorLayout.
 * Now, when you scroll the ListView, the AppBarLayout handles the collapsing
 * automatically. No more complex ScrollListeners in Java!
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Full screen look
        EdgeToEdge.enable(this);
        
        setContentView(R.layout.activity_main);
        
        // Padding for Notch and Status Bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. DATA PREPARATION: Create enough data to make the list scrollable
        ArrayList<Turfname> turfList = new ArrayList<>();
        turfList.add(new Turfname("Gully Cricket Turf", 500.0, R.mipmap.ic_launcher, 4.5));
        turfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8));
        turfList.add(new Turfname("Local Ground", 300.0, R.mipmap.ic_launcher, 3.2));
        turfList.add(new Turfname("Premium Sports Club", 2000.0, R.mipmap.ic_launcher, 5.0));
        turfList.add(new Turfname("Midnight Turf", 800.0, R.mipmap.ic_launcher, 4.0));
        turfList.add(new Turfname("City Play Area", 600.0, R.mipmap.ic_launcher, 3.8));
        turfList.add(new Turfname("Pro Turf Academy", 1500.0, R.mipmap.ic_launcher, 4.7));
        turfList.add(new Turfname("Old School Ground", 400.0, R.mipmap.ic_launcher, 3.5));
        turfList.add(new Turfname("Riverside Arena", 950.0, R.mipmap.ic_launcher, 4.2));
        turfList.add(new Turfname("The Pitch Hub", 1100.0, R.mipmap.ic_launcher, 4.6));

        // 2. SETUP ADAPTER
        ListView listView = findViewById(R.id.listView);
        customeAdapter adapter = new customeAdapter(turfList, this);
        listView.setAdapter(adapter);

        // NOTE: We don't need the 'setOnScrollListener' anymore. 
        // The XML CoordinatorLayout handles the "vanishing" effect smoothly.

    }

}