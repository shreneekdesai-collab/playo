package com.example.playo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category);

        RecyclerView recyclerView = findViewById(R.id.categoryRecyclerView);
        
        // Use GridLayoutManager for 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Create sample data using the new GameCategory class
        List<GameCategory> categoryList = new ArrayList<>();
        categoryList.add(new GameCategory("Cricket", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Football", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Badminton", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Tennis", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Basketball", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Swimming", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Hockey", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Volleyball", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Table Tennis", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Kabaddi", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Golf", R.mipmap.ic_launcher));
        categoryList.add(new GameCategory("Boxing", R.mipmap.ic_launcher));

        // Initialize and set the adapter
        CategoryAdapter adapter = new CategoryAdapter(categoryList);
        recyclerView.setAdapter(adapter);

        BottomNavigationView bottom = findViewById(R.id.bottomView);
        // Set 'Category' as selected since we are on the category page
        bottom.setSelectedItemId(R.id.Category);

        bottom.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.Home) {
                // If Home is clicked, go back to MainActivity
                finish(); // Just finish this activity to return to the previous one (MainActivity)
                return true;
            }
            if (itemId == R.id.Category) {
                // Already here
                return true;
            }
            return false;
        });
    }
}
