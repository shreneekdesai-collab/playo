package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProductImagePagerAdapter extends RecyclerView.Adapter<ProductImagePagerAdapter.ViewHolder> {

    private final List<Integer> imageResList;

    public ProductImagePagerAdapter(List<Integer> imageResList) {
        this.imageResList = imageResList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageResource(imageResList.get(position));
    }

    @Override
    public int getItemCount() {
        return imageResList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.productImageView);
        }
    }
}