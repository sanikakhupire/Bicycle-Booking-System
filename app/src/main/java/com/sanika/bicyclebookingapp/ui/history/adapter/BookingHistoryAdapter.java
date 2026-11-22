package com.sanika.bicyclebookingapp.ui.history.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ItemBookingHistoryBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.util.DateUtil;
import com.sanika.bicyclebookingapp.util.ValidationUtil;

/**
 * RecyclerView adapter for ride history list.
 * - PENDING bookings show a "Cancel Booking" button
 * - COMPLETED / CANCELLED bookings show a "Delete" button
 */
public class BookingHistoryAdapter extends ListAdapter<Booking, BookingHistoryAdapter.HistoryViewHolder> {

    public interface OnActionListener {
        void onCancel(Booking booking);   // for PENDING
        void onDelete(Booking booking);   // for COMPLETED / CANCELLED / EXPIRED
        void onItemClick(Booking booking);// view full booking details
    }

    private final OnActionListener actionListener;

    public BookingHistoryAdapter(OnActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    // No-arg constructor for admin screens
    public BookingHistoryAdapter() {
        super(DIFF_CALLBACK);
        this.actionListener = null;
    }

    private static final DiffUtil.ItemCallback<Booking> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Booking>() {
                @Override
                public boolean areItemsTheSame(@NonNull Booking a, @NonNull Booking b) {
                    return a.getBookingId().equals(b.getBookingId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Booking a, @NonNull Booking b) {
                    return a.getStatus().equals(b.getStatus());
                }
            };

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookingHistoryBinding binding = ItemBookingHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(getItem(position), actionListener);
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemBookingHistoryBinding binding;

        HistoryViewHolder(ItemBookingHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Booking booking, OnActionListener listener) {
            binding.tvStation.setText(booking.getStationName() != null
                    ? booking.getStationName() : "Unknown Station");
            binding.tvDate.setText(DateUtil.formatDateTime(booking.getCreatedAt()));
            binding.tvDuration.setText(ValidationUtil.formatDuration(booking.getDurationMinutes()));
            binding.tvFare.setText(ValidationUtil.formatFare(booking.getFare()));
            binding.tvStatus.setText(booking.getStatus());

            // Status color
            int color;
            if (Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
                color = binding.getRoot().getContext().getColor(R.color.colorCompleted);
            } else if (Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus())) {
                color = binding.getRoot().getContext().getColor(R.color.colorAvailable);
            } else if (Booking.STATUS_CANCELLED.equalsIgnoreCase(booking.getStatus())
                    || Booking.STATUS_EXPIRED.equalsIgnoreCase(booking.getStatus())) {
                color = binding.getRoot().getContext().getColor(R.color.colorUnavailable);
            } else {
                color = binding.getRoot().getContext().getColor(R.color.colorPending);
            }
            binding.tvStatus.setTextColor(color);

            boolean isPending   = Booking.STATUS_PENDING.equalsIgnoreCase(booking.getStatus());
            boolean isCompleted = Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus());
            boolean isCancelled = Booking.STATUS_CANCELLED.equalsIgnoreCase(booking.getStatus());
            boolean isExpired   = Booking.STATUS_EXPIRED.equalsIgnoreCase(booking.getStatus());

            // Cancel button — only for PENDING
            binding.btnCancelBooking.setVisibility(
                    isPending && listener != null ? View.VISIBLE : View.GONE);
            binding.btnCancelBooking.setOnClickListener(v -> {
                if (listener != null) listener.onCancel(booking);
            });

            // Delete button — for COMPLETED, CANCELLED, or EXPIRED
            binding.btnDeleteHistory.setVisibility(
                    (isCompleted || isCancelled || isExpired) && listener != null ? View.VISIBLE : View.GONE);
            binding.btnDeleteHistory.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(booking);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(booking);
            });
        }
    }
}
