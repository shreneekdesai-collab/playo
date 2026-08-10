package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

/**
 * Shows the turfs uploaded by the user for rent, with edit/delete actions
 * and a FAB to add a new listing.
 */
public class MyListingsActivity extends AppCompatActivity implements ListingAdapter.ListingActionListener {

    private final ArrayList<TurfListing> listings = new ArrayList<>();
    private ListingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_listings);

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recycler = findViewById(R.id.listingsRecycler);
        TextView emptyView = findViewById(R.id.emptyView);
        FloatingActionButton fabAddTurf = findViewById(R.id.fabAddTurf);

        // --- Sample data ---
        listings.add(new TurfListing("Sunrise Cricket Turf", "Hadapsar, Pune", 2500, true, 4.5f, 32, R.drawable.user));
        listings.add(new TurfListing("Elite Futsal Court", "Viman Nagar, Pune", 1800, false, 4.0f, 18, R.drawable.user));

        adapter = new ListingAdapter(listings, this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        emptyView.setVisibility(listings.isEmpty() ? View.VISIBLE : View.GONE);

        // --- Add new turf ---
        fabAddTurf.setOnClickListener(v -> startActivity(new Intent(this, AddTurfActivity.class)));
    }

    /** Edit flow: for now confirms and navigates to the Add/Edit form. */
    @Override
    public void onEdit(TurfListing listing) {
        Intent intent = new Intent(this, AddTurfActivity.class);
        intent.putExtra("turf_name", listing.getTurfName());
        startActivity(intent);
    }

    /** Delete flow: asks for confirmation then removes the row. */
    @Override
    public void onDelete(TurfListing listing) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage("Delete \"" + listing.getTurfName() + "\"?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    listings.remove(listing);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, getString(R.string.delete), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}