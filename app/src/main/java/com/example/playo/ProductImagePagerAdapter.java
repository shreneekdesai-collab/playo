package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * ViewPager2 image gallery adapter. Supports both local drawable resources
 * and remote image URLs (loaded via Glide).
 */
public class ProductImagePagerAdapter extends RecyclerView.Adapter<ProductImagePagerAdapter.ViewHolder> {

    private final List<?> imageList;

    public ProductImagePagerAdapter(List<?> imageList) {
        this.imageList = imageList;
    }

    public ProductImagePagerAdapter(List<Integer> imageResList, boolean unused) {
        this.imageList = imageResList;
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
        Object image = imageList.get(position);
        if (image instanceof byte[]) {
            Glide.with(holder.imageView.getContext())
                    .load((byte[]) image)
                    .placeholder(R.drawable.user)
                    .into(holder.imageView);
        } else if (image instanceof String) {
            Glide.with(holder.imageView.getContext())
                    .load(image)
                    .placeholder(R.drawable.user)
                    .into(holder.imageView);
        } else if (image instanceof Integer) {
            holder.imageView.setImageResource((Integer) image);
        }
    }

    @Override
    public int getItemCount() {
        return imageList != null ? imageList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.productImageView);
        }
    }
}
