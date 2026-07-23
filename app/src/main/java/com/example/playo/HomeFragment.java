package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private boolean isBottomNavVisible = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomView);

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

        ArrayList<Turfname> turfList = new ArrayList<>();
        turfList.add(new Turfname("Gully Cricket Turf", 500.0, R.mipmap.ic_launcher, 4.5));
        turfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8));
        turfList.add(new Turfname("Olympic Football Arena", 1200.0, R.mipmap.ic_launcher, 4.8));

        CustomAdapter adapter = new CustomAdapter(turfList, turfname -> {
            Intent intent = new Intent(requireContext(), ProductPage.class);
            intent.putExtra("TURF_NAME", turfname.getName());
            intent.putExtra("TURF_PRICE", "₹ " + turfname.getPrice() + " / Hour");
            intent.putExtra("TURF_IMAGE", turfname.getTurfimg());
            intent.putExtra("TURF_RATING", (float) turfname.getRating());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        return view;
    }
}
