package com.example.playo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;

public class CategoryFragment extends Fragment {

    private boolean isBottomNavVisible = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.categoryRecyclerView);
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomView);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

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

        ArrayList<GameCategory> categoryList = new ArrayList<>();
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

        recyclerView.setAdapter(new CategoryAdapter(categoryList));

        return view;
    }
}
