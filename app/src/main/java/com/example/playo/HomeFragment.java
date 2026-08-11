package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.AppCompatEditText;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private boolean isBottomNavVisible = true;
    private boolean isSearchVisible = false;

    private ArrayList<Turfname> fullTurfList = new ArrayList<>();
    private ArrayList<Turfname> filteredTurfList = new ArrayList<>();
    private CustomAdapter customAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        ImageView searchIcon = view.findViewById(R.id.searchIcon);
        AppCompatEditText searchInput = view.findViewById(R.id.searchInput);
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

        // --- Search icon click: toggle search input ---
        searchIcon.setOnClickListener(v -> {
            if (isSearchVisible) {
                // Hide search input
                AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
                fadeOut.setDuration(200);
                fadeOut.setFillAfter(true);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {
                        searchInput.setVisibility(View.GONE);
                        searchInput.clearAnimation();
                        searchInput.setText("");
                    }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
                searchInput.startAnimation(fadeOut);
                isSearchVisible = false;
            } else {
                // Show search input
                searchInput.setVisibility(View.VISIBLE);
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.setFillAfter(true);
                searchInput.startAnimation(fadeIn);
                searchInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
                isSearchVisible = true;
            }
        });

        // --- Search input text watcher ---
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
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