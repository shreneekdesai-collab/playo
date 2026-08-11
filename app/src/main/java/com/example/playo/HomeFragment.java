package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private boolean isBottomNavVisible = true;
    private boolean isToolbarSearchVisible = false;

    private ArrayList<Turfname> fullTurfList = new ArrayList<>();
    private ArrayList<Turfname> filteredTurfList = new ArrayList<>();
    private CustomAdapter customAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        AppCompatEditText toolbarSearchInput = view.findViewById(R.id.toolbarSearchInput);
        View toolbarSearchContainer = view.findViewById(R.id.toolbarSearchContainer);
        AppBarLayout appBarLayout = view.findViewById(R.id.appBarLayout);
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomView);

        // --- Build full data list ---
        fullTurfList.add(new Turfname("Gully Cricket Turf", 500.0, R.mipmap.ic_launcher, 4.5, "Andheri West, Mumbai"));
        fullTurfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8, "Bandra East, Mumbai"));
        fullTurfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8, "Powai, Mumbai"));

        filteredTurfList.addAll(fullTurfList);

        customAdapter = new CustomAdapter(filteredTurfList, turfname -> {
            Intent intent = new Intent(requireContext(), ProductPage.class);
            intent.putExtra("TURF_NAME", turfname.getName());
            intent.putExtra("TURF_PRICE", "₹ " + turfname.getPrice() + " / Hour");
            intent.putExtra("TURF_IMAGE", turfname.getTurfimg());
            intent.putExtra("TURF_RATING", (float) turfname.getRating());
            startActivity(intent);
        });

        recyclerView.setAdapter(customAdapter);

        // --- Toolbar search input text watcher ---
        toolbarSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- AppBarLayout offset listener for Flipkart-style search bar reveal ---
        appBarLayout.addOnOffsetChangedListener((layout, verticalOffset) -> {
            int scrollRange = layout.getTotalScrollRange();
            if (scrollRange == 0) return;
            float collapsePercent = Math.abs(verticalOffset) / (float) scrollRange;

            // Show search bar when welcome header is ~85-100% collapsed
            if (collapsePercent >= 0.85f && !isToolbarSearchVisible) {
                isToolbarSearchVisible = true;
                toolbarSearchContainer.setVisibility(View.VISIBLE);
                toolbarSearchContainer.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start();
            } else if (collapsePercent < 0.85f && isToolbarSearchVisible) {
                isToolbarSearchVisible = false;
                toolbarSearchContainer.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> toolbarSearchContainer.setVisibility(View.GONE))
                        .start();
            }
        });

        // --- Bottom nav auto-hide on scroll (unchanged) ---
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 10 && isBottomNavVisible) {
                    isBottomNavVisible = false;
                    bottomNav.animate()
                            .translationY(bottomNav.getHeight() + 30f)
                            .setDuration(250)
                            .start();
                } else if (dy < -10 && !isBottomNavVisible) {
                    isBottomNavVisible = true;
                    bottomNav.animate()
                            .translationY(0)
                            .setDuration(250)
                            .start();
                }
            }
        });

        return view;
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