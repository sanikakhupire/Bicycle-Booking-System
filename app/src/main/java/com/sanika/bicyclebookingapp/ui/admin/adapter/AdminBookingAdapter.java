package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAdminBookingBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Admin Booking Management.
 */
public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.ViewHolder> {

    public interface OnBookingActionListener {
        void onCancelBooking(Booking booking);
    }

    private final List<Booking> bookings = new ArrayList<>();
    private final OnBookingActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);

    public AdminBookingAdapter(OnBookingActionListener listener) {
        this.listener = listener;
    }

    public void setBookings(List<Booking> newBookings) {
        this.bookings.clear();
        if (newBookings != null) {
            this.bookings.addAll(newBookings);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminBookingBinding binding = ItemAdminBookingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(bookings.get(position));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminBookingBinding binding;

        ViewHolder(ItemAdminBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Booking booking) {
            binding.tvAdminBookingId.setText("Booking #" + (booking.getBookingId() != null ? booking.getBookingId() : "N/A"));

            String status = booking.getStatus() != null ? booking.getStatus().toUpperCase() : Booking.STATUS_PENDING;
            binding.tvAdminBookingStatus.setText(status);

            switch (status) {
                case Booking.STATUS_COMPLETED:
                    binding.tvAdminBookingStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                    binding.tvAdminBookingStatus.setTextColor(Color.parseColor("#2E7D32"));
                    binding.btnCancelBooking.setVisibility(View.GONE);
                    break;
                case Booking.STATUS_ACTIVE:
                    binding.tvAdminBookingStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                    binding.tvAdminBookingStatus.setTextColor(Color.parseColor("#1565C0"));
                    binding.btnCancelBooking.setVisibility(View.VISIBLE);
                    break;
                case Booking.STATUS_CANCELLED:
                case Booking.STATUS_EXPIRED:
                    binding.tvAdminBookingStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                    binding.tvAdminBookingStatus.setTextColor(Color.parseColor("#C62828"));
                    binding.btnCancelBooking.setVisibility(View.GONE);
                    break;
                default: // PENDING
                    binding.tvAdminBookingStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                    binding.tvAdminBookingStatus.setTextColor(Color.parseColor("#E65100"));
                    binding.btnCancelBooking.setVisibility(View.VISIBLE);
                    break;
            }

            String bikeInfo = "Bicycle: " + (booking.getBicycleId() != null ? booking.getBicycleId() : "N/A")
                    + " (" + (booking.getBicycleType() != null ? booking.getBicycleType() : "Standard") + ")"
                    + " • Station: " + (booking.getStationName() != null ? booking.getStationName() : "N/A");
            binding.tvAdminBookingDetails.setText(bikeInfo);

            double fare = booking.getFare();
            String userInfo = "User: " + (booking.getUserId() != null ? booking.getUserId() : "N/A")
                    + (fare > 0 ? " • Fare: " + CurrencyUtil.formatInr(fare) : "");
            binding.tvAdminBookingUser.setText(userInfo);

            if (booking.getCreatedAt() != null) {
                binding.tvAdminBookingDate.setText(dateFormat.format(booking.getCreatedAt()));
            } else {
                binding.tvAdminBookingDate.setText("Recent");
            }

            binding.btnCancelBooking.setOnClickListener(v -> {
                if (listener != null) listener.onCancelBooking(booking);
            });
        }
    }
}
