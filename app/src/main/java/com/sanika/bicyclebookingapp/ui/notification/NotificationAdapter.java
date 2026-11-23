package com.sanika.bicyclebookingapp.ui.notification;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAppNotificationBinding;
import com.sanika.bicyclebookingapp.model.AppNotification;

import java.util.Date;

/**
 * Adapter for In-App Notification Center list items with read/unread visual styling
 * and deep link click handling.
 */
public class NotificationAdapter extends ListAdapter<AppNotification, NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    private final OnNotificationClickListener clickListener;

    public NotificationAdapter(OnNotificationClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<AppNotification> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AppNotification>() {
                @Override
                public boolean areItemsTheSame(@NonNull AppNotification a, @NonNull AppNotification b) {
                    return a.getNotificationId() != null && a.getNotificationId().equals(b.getNotificationId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AppNotification a, @NonNull AppNotification b) {
                    return a.isRead() == b.isRead()
                            && (a.getTitle() != null && a.getTitle().equals(b.getTitle()))
                            && (a.getMessage() != null && a.getMessage().equals(b.getMessage()));
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppNotificationBinding binding = ItemAppNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppNotificationBinding binding;

        ViewHolder(ItemAppNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AppNotification notification, OnNotificationClickListener clickListener) {
            // Unread styling
            boolean isUnread = !notification.isRead();
            binding.viewUnreadDot.setVisibility(isUnread ? View.VISIBLE : View.GONE);
            binding.tvTitle.setTypeface(null, isUnread ? Typeface.BOLD : Typeface.NORMAL);
            binding.tvTitle.setTextColor(isUnread ? Color.parseColor("#1565C0") : Color.parseColor("#212121"));

            binding.tvTitle.setText(notification.getTitle() != null ? notification.getTitle() : "Notification");
            binding.tvMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");

            // Relative time string
            if (notification.getCreatedAt() != null) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                        notification.getCreatedAt().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                );
                binding.tvTimestamp.setText(timeAgo);
            } else {
                binding.tvTimestamp.setText("Just now");
            }

            // Category tag
            String channel = notification.getChannelId() != null ? notification.getChannelId() : "";
            switch (channel) {
                case AppNotification.CHANNEL_BOOKINGS:
                    binding.tvCategoryTag.setText("BOOKING");
                    binding.tvCategoryTag.setTextColor(Color.parseColor("#1976D2"));
                    break;
                case AppNotification.CHANNEL_RIDES:
                    binding.tvCategoryTag.setText("RIDE");
                    binding.tvCategoryTag.setTextColor(Color.parseColor("#2E7D32"));
                    break;
                case AppNotification.CHANNEL_PAYMENTS:
                    binding.tvCategoryTag.setText("PAYMENT");
                    binding.tvCategoryTag.setTextColor(Color.parseColor("#E65100"));
                    break;
                case AppNotification.CHANNEL_ALERTS:
                    binding.tvCategoryTag.setText("ALERT");
                    binding.tvCategoryTag.setTextColor(Color.parseColor("#C62828"));
                    break;
                case AppNotification.CHANNEL_ADMIN:
                    binding.tvCategoryTag.setText("ADMIN OPS");
                    binding.tvCategoryTag.setTextColor(Color.parseColor("#6A1B9A"));
                    break;
                default:
                    binding.tvCategoryTag.setText("UPDATE");
                    binding.tvCategoryTag.setTextColor(Color.parseColor("#616161"));
                    break;
            }

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) clickListener.onNotificationClick(notification);
            });
        }
    }
}
