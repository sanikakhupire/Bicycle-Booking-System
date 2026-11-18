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
        void onDelete(Booking booking);   // for COMPLETED / CANCELLED
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
            switch (booking.getStatus()) {
                case Booking.STATUS_COMPLETED:
                    color = binding.getRoot().getContext().getColor(R.color.colorCompleted);
                    break;
                case Booking.STATUS_ACTIVE:
                    color = binding.getRoot().getContext().getColor(R.color.colorAvailable);
                    break;
                case Booking.STATUS_CANCELLED:
                    color = binding.getRoot().getContext().getColor(R.color.colorUnavailable);
                    break;
                default:
                    color = binding.getRoot().getContext().getColor(R.color.colorPending);
                    break;
            }
            binding.tvStatus.setTextColor(color);

            boolean isPending   = Booking.STATUS_PENDING.equals(booking.getStatus());
            boolean isCompleted = Booking.STATUS_COMPLETED.equals(booking.getStatus());
            boolean isCancelled = Booking.STATUS_CANCELLED.equals(booking.getStatus());

            // Cancel button — only for PENDING
            binding.btnCancelBooking.setVisibility(
                    isPending && listener != null ? View.VISIBLE : View.GONE);
            binding.btnCancelBooking.setOnClickListener(v -> {
                if (listener != null) listener.onCancel(booking);
            });

            // Delete button — only for COMPLETED or CANCELLED
            binding.btnDeleteHistory.setVisibility(
                    (isCompleted || isCancelled) && listener != null ? View.VISIBLE : View.GONE);
            binding.btnDeleteHistory.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(booking);
            });
        }
    }
}
