package com.example.playo;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

/**
 * Horizontal RecyclerView adapter showing the selected turf photos as small
 * thumbnails with a remove button each, plus an "Add Photos" tile at the end
 * that triggers the photo picker.
 */
public class PhotoPreviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD = 0;
    private static final int TYPE_PHOTO = 1;

    /** Callbacks for adding and removing photos. */
    public interface Listener {
        void onAddPhoto();
        void onRemovePhoto(int position);
    }

    private final List<Uri> photos;
    private final Listener listener;

    public PhotoPreviewAdapter(List<Uri> photos, Listener listener) {
        this.photos = photos;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == photos.size() ? TYPE_ADD : TYPE_PHOTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            return new AddViewHolder(inflater.inflate(R.layout.item_add_photo, parent, false));
        }
        return new PhotoViewHolder(inflater.inflate(R.layout.item_photo_preview, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PhotoViewHolder) {
            PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
            photoHolder.image.setImageURI(photos.get(position));
            AnimUtils.slideUpFadeIn(holder.itemView, Math.min(position, Motion.STAGGER_MAX - 1)
                    * Motion.STAGGER_STEP);
            PressEffect.attach(photoHolder.removeButton);
            photoHolder.removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemovePhoto(position);
                }
            });
        } else if (holder instanceof AddViewHolder) {
            AnimUtils.slideUpFadeIn(holder.itemView, Math.min(position, Motion.STAGGER_MAX - 1)
                    * Motion.STAGGER_STEP);
            PressEffect.attach(holder.itemView);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddPhoto();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return photos.size() + 1;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final MaterialButton removeButton;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.photoPreviewImage);
            removeButton = itemView.findViewById(R.id.btnRemovePhoto);
        }
    }

    static class AddViewHolder extends RecyclerView.ViewHolder {
        AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
