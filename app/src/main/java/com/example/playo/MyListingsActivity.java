package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the turfs uploaded by the user for rent, loaded from Firestore, with
 * edit/delete actions and a FAB to add a new listing.
 */
public class MyListingsActivity extends AppCompatActivity implements ListingAdapter.ListingActionListener {

    private final ArrayList<TurfListing> listings = new ArrayList<>();
    private ListingAdapter adapter;
    private TextView emptyView;
    private RecyclerView recycler;

    @Override
    protected void onStop() {
        AnimUtils.cancelTree(recycler);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserRole.fetchRole(role -> {
            if (isFinishing()) {
                return;
            }
            if (!"owner".equals(role)) {
                Toast.makeText(this, "Only turf owners can do this", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            setupOwnerScreen();
        });
    }

    private void setupOwnerScreen() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_listings);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- Edge-to-edge: keep the toolbar below the status bar ---
        final int baseToolbarHeight = toolbar.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topInset = systemBars.top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = baseToolbarHeight + topInset;
            v.setLayoutParams(lp);
            return insets;
        });

        recycler = findViewById(R.id.listingsRecycler);
        emptyView = findViewById(R.id.emptyView);
        FloatingActionButton fabAddTurf = findViewById(R.id.fabAddTurf);

        adapter = new ListingAdapter(listings, this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        loadListings();

        // --- Add new turf ---
        fabAddTurf.setOnClickListener(v -> startActivity(new Intent(this, AddTurfActivity.class)));
    }

    /** Loads the signed-in user's turf listings from Firestore. */
    private void loadListings() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        String uid = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("turfs")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listings.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        listings.add(mapToListing(doc));
                    }
                    adapter.notifyDataSetChanged();
                    AnimUtils.staggeredEntrance(recycler);
                    emptyView.setVisibility(listings.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Failed to load listings", Toast.LENGTH_SHORT).show();
                });
    }

    /** Maps a Firestore turf document to a {@link TurfListing}. */
    private TurfListing mapToListing(DocumentSnapshot doc) {
        String turfId = doc.getString("turfId");
        String name = doc.getString("name");
        String location = doc.getString("location");
        Double price = doc.getDouble("price");
        Boolean available = doc.getBoolean("available");
        Double rating = doc.getDouble("rating");
        String sport = doc.getString("sport");
        List<String> imageUrls = (List<String>) doc.get("imageUrls");
        String imageUrl = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null;
        Blob thumbnail = doc.getBlob("thumbnail");

        TurfListing listing = new TurfListing(
                turfId,
                name != null ? name : "Unnamed Turf",
                location != null ? location : "",
                price != null ? price : 0.0,
                available != null ? available : true,
                rating != null ? rating.floatValue() : 0.0f,
                0,
                R.drawable.user,
                imageUrl,
                sport != null ? sport : "");
        if (thumbnail != null) {
            listing.setThumbnailBytes(thumbnail.toBytes());
        }
        return listing;
    }

    /** Opens the turf detail page for the given listing. */
    @Override
    public void onView(TurfListing listing) {
        Intent intent = new Intent(this, ProductPage.class);
        if (listing.getTurfId() != null) {
            intent.putExtra("TURF_ID", listing.getTurfId());
        } else {
            intent.putExtra("TURF_NAME", listing.getTurfName());
            intent.putExtra("TURF_PRICE", "₹ " + listing.getPrice() + " / Hour");
            intent.putExtra("TURF_IMAGE", listing.getImageRes());
            intent.putExtra("TURF_RATING", listing.getRating());
        }
        startActivity(intent);
    }

    /** Edit flow: for now confirms and navigates to the Add/Edit form. */
    @Override
    public void onEdit(TurfListing listing) {
        Intent intent = new Intent(this, AddTurfActivity.class);
        intent.putExtra("turf_name", listing.getTurfName());
        startActivity(intent);
    }

    /** Delete flow: asks for confirmation then deletes from Firestore. */
    @Override
    public void onDelete(TurfListing listing) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage("Delete \"" + listing.getTurfName() + "\"?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (listing.getTurfId() != null) {
                        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                        firestore.collection("turfs").document(listing.getTurfId())
                                .collection("photos").get()
                                .addOnSuccessListener(photos -> {
                                    WriteBatch batch = firestore.batch();
                                    for (DocumentSnapshot photo : photos.getDocuments()) {
                                        batch.delete(photo.getReference());
                                    }
                                    batch.delete(firestore.collection("turfs")
                                            .document(listing.getTurfId()));
                                    batch.commit().addOnSuccessListener(unused -> removeListing(listing))
                                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Failed to delete listing", Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Failed to delete listing photos", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    removeListing(listing);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void removeListing(TurfListing listing) {
        listings.remove(listing);
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(listings.isEmpty() ? View.VISIBLE : View.GONE);
        Toast.makeText(this, getString(R.string.delete), Toast.LENGTH_SHORT).show();
    }
}
