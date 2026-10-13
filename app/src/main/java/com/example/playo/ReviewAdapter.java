package com.example.playo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private final List<Review> reviews;
    private final String currentUserId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    public ReviewAdapter(List<Review> reviews, String currentUserId) {
        this.reviews = reviews;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        PressEffect.attach(view);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.name.setText(review.getUserName());
        holder.rating.setRating(review.getRating());

        if (review.getCreatedAt() == null) {
            holder.date.setText("");
        } else {
            holder.date.setText(dateFormat.format(review.getCreatedAt().toDate()));
        }

        String comment = review.getComment();
        if (comment == null || comment.trim().isEmpty()) {
            holder.comment.setVisibility(View.GONE);
        } else {
            holder.comment.setVisibility(View.VISIBLE);
            holder.comment.setText(comment);
        }

        boolean isOwnReview = currentUserId != null && currentUserId.equals(review.getUserId());
        holder.yourReview.setVisibility(isOwnReview ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView yourReview;
        final TextView date;
        final TextView comment;
        final RatingBar rating;

        ReviewViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.reviewUserName);
            yourReview = itemView.findViewById(R.id.reviewOwnerLabel);
            date = itemView.findViewById(R.id.reviewDate);
            comment = itemView.findViewById(R.id.reviewComment);
            rating = itemView.findViewById(R.id.reviewRating);
        }
    }
}
