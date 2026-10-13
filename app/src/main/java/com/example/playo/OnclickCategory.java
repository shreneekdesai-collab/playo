package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnclickCategory extends AppCompatActivity {

    private String categoryName;
    private RecyclerView recyclerView;
    private CustomAdapter adapter;
    private View stateContainer;
    private View loadingView;
    private View emptyView;
    private View errorView;
    private View retryButton;

    @Override
    protected void onStop() {
        AnimUtils.cancelTree(recyclerView);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.onclick_category);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        TextView textView = findViewById(R.id.textView);
        if (categoryName != null) {
            textView.setText(categoryName);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        stateContainer = findViewById(R.id.categoryStateContainer);
        loadingView = findViewById(R.id.categoryProgress);
        emptyView = findViewById(R.id.emptyView);
        errorView = findViewById(R.id.categoryErrorView);
        retryButton = findViewById(R.id.categoryRetryButton);

        adapter = new CustomAdapter(new ArrayList<>(), turfname -> {
            Intent intent = new Intent(OnclickCategory.this, ProductPage.class);
            if (turfname.getTurfId() != null) {
                intent.putExtra("TURF_ID", turfname.getTurfId());
            }
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        retryButton.setOnClickListener(v -> loadTurfs());
        loadTurfs();

        BottomNavHelper.setup(this, findViewById(R.id.bottomView), R.id.Category);
    }

    private void loadTurfs() {
        showLoadingState();
        if (categoryName == null || categoryName.trim().isEmpty()) {
            showEmptyState();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("turfs")
                .whereEqualTo("sport", categoryName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    ArrayList<Turfname> turfs = mapAndSortTurfs(snapshot);
                    adapter.updateList(turfs);
                    AnimUtils.staggeredEntrance(recyclerView);
                    if (turfs.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideStateContainer();
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e("OnclickCategory", "Failed to load category turfs", error);
                    ErrorLogger.log(this, "Category", "load_turfs", error);
                    showErrorState();
                });
    }

    private ArrayList<Turfname> mapAndSortTurfs(QuerySnapshot snapshot) {
        List<DocumentSnapshot> documents = new ArrayList<>(snapshot.getDocuments());
        Collections.sort(documents, (left, right) -> {
            Timestamp leftTime = left.getTimestamp("createdAt");
            Timestamp rightTime = right.getTimestamp("createdAt");
            if (leftTime == null && rightTime == null) return 0;
            if (leftTime == null) return 1;
            if (rightTime == null) return -1;
            return rightTime.compareTo(leftTime);
        });

        ArrayList<Turfname> turfs = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Double price = document.getDouble("price");
            Double rating = document.getDouble("rating");
            List<String> imageUrls = (List<String>) document.get("imageUrls");
            String imageUrl = imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null;
            String turfId = document.getString("turfId");
            Turfname turf = new Turfname(
                    valueOrEmpty(document.getString("name")),
                    price != null ? price : 0.0,
                    R.mipmap.ic_launcher,
                    rating != null ? rating : 0.0,
                    valueOrEmpty(document.getString("location")),
                    valueOrEmpty(document.getString("sport")),
                    turfId != null ? turfId : document.getId(),
                    imageUrl);
            Blob thumbnail = document.getBlob("thumbnail");
            if (thumbnail != null) {
                turf.setThumbnailBytes(thumbnail.toBytes());
            }
            turfs.add(turf);
        }
        return turfs;
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private void showLoadingState() {
        AnimUtils.cancel(stateContainer);
        AnimUtils.cancel(recyclerView);
        stateContainer.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAlpha(Motion.enabled(this) ? 0f : 1f);
        setShimmerRunning(true);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        setShimmerRunning(false);
        AnimUtils.cancel(stateContainer);
        AnimUtils.cancel(recyclerView);
        recyclerView.setAlpha(1f);
        stateContainer.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    private void showErrorState() {
        setShimmerRunning(false);
        AnimUtils.cancel(stateContainer);
        AnimUtils.cancel(recyclerView);
        recyclerView.setAlpha(1f);
        stateContainer.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private void hideStateContainer() {
        setShimmerRunning(false);
        if (Motion.enabled(this)) {
            AnimUtils.fadeOut(stateContainer);
            AnimUtils.fadeIn(recyclerView);
        } else {
            stateContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAlpha(1f);
        }
    }

    private void setShimmerRunning(boolean running) {
        if (!(loadingView instanceof com.facebook.shimmer.ShimmerFrameLayout)) {
            return;
        }
        com.facebook.shimmer.ShimmerFrameLayout shimmer =
                (com.facebook.shimmer.ShimmerFrameLayout) loadingView;
        if (running && Motion.enabled(this)) {
            shimmer.startShimmer();
        } else {
            shimmer.stopShimmer();
        }
    }
}
