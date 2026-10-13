package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.AppCompatEditText;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Blob;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private boolean isBottomNavVisible = true;
    private boolean isToolbarSearchVisible = false;
    private float searchCollapsePercent;

    private final ArrayList<Turfname> fullTurfList = new ArrayList<>();
    private final ArrayList<Turfname> filteredTurfList = new ArrayList<>();
    private CustomAdapter customAdapter;
    private RecyclerView recyclerView;
    private View stateContainer;
    private View loadingView;
    private View emptyView;
    private View errorView;
    private View retryButton;
    private View fragmentView;
    private AppCompatEditText toolbarSearchInput;
    private View toolbarSearchContainer;
    private BottomNavigationView bottomNav;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        fragmentView = view;

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        stateContainer = view.findViewById(R.id.homeStateContainer);
        loadingView = view.findViewById(R.id.homeProgress);
        emptyView = view.findViewById(R.id.homeEmptyView);
        errorView = view.findViewById(R.id.homeErrorView);
        retryButton = view.findViewById(R.id.homeRetryButton);
        PressEffect.attach(retryButton);

        toolbarSearchInput = view.findViewById(R.id.toolbarSearchInput);
        toolbarSearchContainer = view.findViewById(R.id.toolbarSearchContainer);
        AppBarLayout appBarLayout = view.findViewById(R.id.appBarLayout);
        bottomNav = requireActivity().findViewById(R.id.bottomView);
        AnimUtils.fadeIn(view.findViewById(R.id.textView));
        AnimUtils.fadeIn(view.findViewById(R.id.homeWelcomeSubtitle), Motion.STAGGER_STEP);

        customAdapter = new CustomAdapter(filteredTurfList, turfname -> {
            if (!isAdded()) {
                return;
            }
            Intent intent = new Intent(requireContext(), ProductPage.class);
            if (turfname.getTurfId() != null) {
                intent.putExtra("TURF_ID", turfname.getTurfId());
            }
            startActivity(intent);
        });

        recyclerView.setAdapter(customAdapter);
        retryButton.setOnClickListener(v -> loadTurfs());

        // --- Toolbar search input text watcher ---
        toolbarSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
                updateSearchClearButton();
                updateToolbarSearchVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        toolbarSearchInput.setOnFocusChangeListener((v, hasFocus) -> updateToolbarSearchVisibility());
        toolbarSearchInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP
                    && toolbarSearchInput.getCompoundDrawablesRelative()[2] != null
                    && event.getX() >= toolbarSearchInput.getWidth()
                    - toolbarSearchInput.getTotalPaddingRight()) {
                toolbarSearchInput.setText("");
                return true;
            }
            return false;
        });
        updateSearchClearButton();

        // --- AppBarLayout offset listener for Flipkart-style search bar reveal ---
        appBarLayout.addOnOffsetChangedListener((layout, verticalOffset) -> {
            int scrollRange = layout.getTotalScrollRange();
            if (scrollRange == 0) return;
            searchCollapsePercent = Math.abs(verticalOffset) / (float) scrollRange;
            updateToolbarSearchVisibility();
        });

        // --- Bottom nav auto-hide on scroll (unchanged) ---
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 10 && isBottomNavVisible) {
                    isBottomNavVisible = false;
                    BottomNavHelper.hide(bottomNav);
                } else if (dy < -10 && !isBottomNavVisible) {
                    isBottomNavVisible = true;
                    BottomNavHelper.show(bottomNav);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && fragmentView != null) {
            loadTurfs();
        }
    }

    @Override
    public void onDestroyView() {
        AnimUtils.cancel(toolbarSearchContainer);
        AnimUtils.cancelTree(recyclerView);
        BottomNavHelper.cancel(bottomNav);
        fragmentView = null;
        recyclerView = null;
        customAdapter = null;
        toolbarSearchInput = null;
        toolbarSearchContainer = null;
        super.onDestroyView();
    }

    private boolean shouldShowToolbarSearch() {
        return toolbarSearchInput != null
                && (searchCollapsePercent >= 0.85f
                || toolbarSearchInput.hasFocus()
                || toolbarSearchInput.length() > 0);
    }

    private void updateToolbarSearchVisibility() {
        if (toolbarSearchContainer == null) {
            return;
        }
        boolean shouldShow = shouldShowToolbarSearch();
        if (shouldShow == isToolbarSearchVisible) {
            return;
        }

        View container = toolbarSearchContainer;
        AnimUtils.cancel(container);
        if (!Motion.enabled(container.getContext())) {
            isToolbarSearchVisible = shouldShow;
            container.setAlpha(1f);
            container.setTranslationY(0f);
            container.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
            return;
        }
        if (shouldShow) {
            isToolbarSearchVisible = true;
            container.setVisibility(View.VISIBLE);
            container.setTranslationY(-4 * getResources().getDisplayMetrics().density);
            container.animate().alpha(1f).translationY(0)
                    .setDuration(Motion.DURATION_MEDIUM)
                    .setInterpolator(Motion.FAST_OUT_SLOW_IN).start();
        } else {
            isToolbarSearchVisible = false;
            container.animate()
                    .alpha(0f)
                    .translationY(-4 * getResources().getDisplayMetrics().density)
                    .setDuration(Motion.DURATION_MEDIUM)
                    .setInterpolator(Motion.EASE_OUT)
                    .withEndAction(() -> {
                        if (toolbarSearchContainer == container && !shouldShowToolbarSearch()) {
                            container.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    private void updateSearchClearButton() {
        if (toolbarSearchInput == null) {
            return;
        }
        int clearIcon = toolbarSearchInput.length() > 0 ? R.drawable.ic_close : 0;
        toolbarSearchInput.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, clearIcon, 0);
    }

    private void loadTurfs() {
        if (!isAdded() || fragmentView == null) {
            return;
        }
        showLoadingState();
        FirebaseFirestore.getInstance()
                .collection("turfs")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || fragmentView == null) {
                        return;
                    }
                    fullTurfList.clear();
                    fullTurfList.addAll(mapAndSortTurfs(snapshot));
                    filterList(toolbarSearchText());
                    if (toolbarSearchText().trim().isEmpty()) {
                        AnimUtils.staggeredEntrance(recyclerView);
                    }
                    if (fullTurfList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideStateContainer();
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e("HomeFragment", "Failed to load turfs", error);
                    ErrorLogger.log(requireContext(), "Home", "load_turfs", error);
                    if (isAdded() && fragmentView != null) {
                        showErrorState();
                    }
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
            turfs.add(mapTurf(document));
        }
        return turfs;
    }

    private Turfname mapTurf(DocumentSnapshot document) {
        Double price = document.getDouble("price");
        Double rating = document.getDouble("rating");
        List<String> imageUrls = (List<String>) document.get("imageUrls");
        String imageUrl = imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null;
        Blob thumbnail = document.getBlob("thumbnail");
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
        if (thumbnail != null) {
            turf.setThumbnailBytes(thumbnail.toBytes());
        }
        return turf;
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private String toolbarSearchText() {
        View search = fragmentView != null ? fragmentView.findViewById(R.id.toolbarSearchInput) : null;
        return search instanceof AppCompatEditText
                ? ((AppCompatEditText) search).getText().toString() : "";
    }

    private void showLoadingState() {
        AnimUtils.cancel(stateContainer);
        AnimUtils.cancel(recyclerView);
        stateContainer.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAlpha(Motion.enabled(requireContext()) ? 0f : 1f);
        setShimmerRunning(loadingView, true);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        setShimmerRunning(loadingView, false);
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
        setShimmerRunning(loadingView, false);
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
        setShimmerRunning(loadingView, false);
        if (Motion.enabled(requireContext())) {
            AnimUtils.fadeOut(stateContainer);
            AnimUtils.fadeIn(recyclerView);
        } else {
            stateContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAlpha(1f);
        }
    }

    private void setShimmerRunning(View shimmerView, boolean running) {
        if (!(shimmerView instanceof com.facebook.shimmer.ShimmerFrameLayout)) {
            return;
        }
        com.facebook.shimmer.ShimmerFrameLayout shimmer =
                (com.facebook.shimmer.ShimmerFrameLayout) shimmerView;
        if (running && Motion.enabled(requireContext())) {
            shimmer.startShimmer();
        } else {
            shimmer.stopShimmer();
        }
    }

    private void filterList(String query) {
        ArrayList<Turfname> newList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        if (lowerQuery.isEmpty()) {
            newList.addAll(fullTurfList);
        } else {
            for (Turfname turf : fullTurfList) {
                if (turf.getName().toLowerCase().contains(lowerQuery)
                        || (turf.getLocation() != null && turf.getLocation().toLowerCase().contains(lowerQuery))) {
                    newList.add(turf);
                }
            }
        }
        customAdapter.updateList(newList);
    }
}
