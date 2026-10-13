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
    private RecyclerView recyclerView;
    private BottomNavigationView bottomNav;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        recyclerView = view.findViewById(R.id.categoryRecyclerView);
        bottomNav = requireActivity().findViewById(R.id.bottomView);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

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
        AnimUtils.staggeredEntrance(recyclerView);

        return view;
    }

    @Override
    public void onDestroyView() {
        AnimUtils.cancelTree(recyclerView);
        BottomNavHelper.cancel(bottomNav);
        recyclerView = null;
        bottomNav = null;
        super.onDestroyView();
    }
}
