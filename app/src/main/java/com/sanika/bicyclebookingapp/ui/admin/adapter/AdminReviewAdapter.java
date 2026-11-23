package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAdminReviewBinding;
import com.sanika.bicyclebookingapp.model.RideReview;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Admin Customer Review Moderation.
 */
public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.ViewHolder> {

    public interface OnReviewModerationListener {
        void onModerate(RideReview review, String newStatus, String reason);
    }

    private final List<RideReview> reviews = new ArrayList<>();
    private final OnReviewModerationListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);

    public AdminReviewAdapter(OnReviewModerationListener listener) {
        this.listener = listener;
    }

    public void setReviews(List<RideReview> newReviews) {
        this.reviews.clear();
        if (newReviews != null) {
            this.reviews.addAll(newReviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminReviewBinding binding = ItemAdminReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminReviewBinding binding;

        ViewHolder(ItemAdminReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RideReview review) {
            binding.tvReviewUserName.setText(review.getUserName() != null ? review.getUserName() : "Customer");
            binding.tvRatingOverall.setText(String.format(Locale.ENGLISH, "★ %.1f Overall", review.getRatingOverall()));
            binding.tvRatingBicycle.setText(String.format(Locale.ENGLISH, "★ %.1f Bike", review.getRatingBicycle()));
            binding.tvBicycleCondition.setText(review.getBicycleCondition() != null ? review.getBicycleCondition() : "GOOD");

            StringBuilder contextStr = new StringBuilder();
            if (review.getBicycleId() != null) contextStr.append("Bicycle: ").append(review.getBicycleId());
            if (review.getPickupStationName() != null && review.getReturnStationName() != null) {
                if (contextStr.length() > 0) contextStr.append(" • ");
                contextStr.append(review.getPickupStationName()).append(" → ").append(review.getReturnStationName());
            }
            binding.tvReviewContext.setText(contextStr.toString());

            binding.tvReviewComment.setText(review.getComment() != null ? review.getComment() : "");

            if (review.getCreatedAt() != null) {
                binding.tvReviewDate.setText(dateFormat.format(review.getCreatedAt()));
            } else {
                binding.tvReviewDate.setText("");
            }

            // Status styling
            String status = review.getStatus() != null ? review.getStatus().toUpperCase() : RideReview.STATUS_PUBLISHED;
            binding.tvReviewStatus.setText(status);

            if (RideReview.STATUS_PUBLISHED.equalsIgnoreCase(status)) {
                binding.tvReviewStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                binding.tvReviewStatus.setTextColor(Color.parseColor("#2E7D32"));
                binding.btnToggleVisibility.setText("Hide Review");
            } else if (RideReview.STATUS_FLAGGED.equalsIgnoreCase(status)) {
                binding.tvReviewStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                binding.tvReviewStatus.setTextColor(Color.parseColor("#C62828"));
                binding.btnToggleVisibility.setText("Hide Review");
            } else { // HIDDEN
                binding.tvReviewStatus.setBackgroundColor(Color.parseColor("#EEEEEE"));
                binding.tvReviewStatus.setTextColor(Color.parseColor("#757575"));
                binding.btnToggleVisibility.setText("Publish");
            }

            binding.btnToggleVisibility.setOnClickListener(v -> {
                if (listener != null) {
                    String nextStatus = RideReview.STATUS_PUBLISHED.equalsIgnoreCase(status)
                            ? RideReview.STATUS_HIDDEN : RideReview.STATUS_PUBLISHED;
                    listener.onModerate(review, nextStatus, "Admin toggled visibility");
                }
            });

            binding.btnFlagReview.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onModerate(review, RideReview.STATUS_FLAGGED, "Admin flagged for inappropriate content");
                }
            });
        }
    }
}
