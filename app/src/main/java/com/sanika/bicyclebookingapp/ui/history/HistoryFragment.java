package com.sanika.bicyclebookingapp.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.databinding.FragmentHistoryBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.ui.history.adapter.BookingHistoryAdapter;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private BookingViewModel bookingViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);

        BookingHistoryAdapter adapter = new BookingHistoryAdapter(
            new BookingHistoryAdapter.OnActionListener() {
                @Override
                public void onCancel(Booking booking) {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Cancel Booking")
                        .setMessage("Cancel your booking at " + booking.getStationName() + "?")
                        .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                            bookingViewModel.cancelBooking(
                                booking.getBookingId(),
                                booking.getBicycleId(),
                                booking.getStationId()
                            ).observe(getViewLifecycleOwner(), resource -> {
                                if (resource.isError()) {
                                    Toast.makeText(requireContext(),
                                        "Failed: " + resource.message,
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Keep", null)
                        .show();
                }

                @Override
                public void onDelete(Booking booking) {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Hide Record")
                        .setMessage("Hide this ride from your history?")
                        .setPositiveButton("Hide", (dialog, which) -> deleteBooking(booking))
                        .setNegativeButton("Cancel", null)
                        .show();
                }

                @Override
                public void onItemClick(Booking booking) {
                    showBookingDetails(booking);
                }
            }
        );

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        bookingViewModel.getUserBookings(uid).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        adapter.submitList(resource.data);
                        binding.tvEmpty.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    if (resource.message != null && resource.message.contains("index")) {
                        binding.tvEmpty.setText(
                            "Setting up ride history..\n\nPlease open this link to enable it:\n"
                            + resource.message);
                    } else {
                        binding.tvEmpty.setText("Could not load history.\n" + resource.message);
                    }
                    break;
            }
        });
    }

    /**
     * Flags the booking document as hiddenByUser in Firestore.
     * Preserves the official booking record for accounting and admin logs,
     * while cleanly hiding it from the user's personal history list.
     */
    private void deleteBooking(Booking booking) {
        FirebaseFirestore.getInstance()
            .collection("bookings")
            .document(booking.getBookingId())
            .update("hiddenByUser", true)
            .addOnSuccessListener(unused ->
                Toast.makeText(requireContext(),
                    "Removed from history", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(requireContext(),
                    "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showBookingDetails(Booking booking) {
        String shortId = booking.getBookingId() != null && booking.getBookingId().length() >= 8
                ? booking.getBookingId().substring(0, 8).toUpperCase()
                : (booking.getBookingId() != null ? booking.getBookingId() : "—");
        String formattedRef = "BK-" + shortId;

        String bikeId = booking.getBicycleId() != null && booking.getBicycleId().length() >= 8
                ? booking.getBicycleId().substring(0, 8).toUpperCase()
                : (booking.getBicycleId() != null ? booking.getBicycleId() : "—");

        String returnStation = booking.getReturnStationName() != null && !booking.getReturnStationName().isEmpty()
                ? booking.getReturnStationName()
                : (Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus()) ? "In transit" : "—");

        String startTimeStr = booking.getStartTime() != null
                ? com.sanika.bicyclebookingapp.util.DateUtil.formatDateTime(booking.getStartTime())
                : (Booking.STATUS_PENDING.equalsIgnoreCase(booking.getStatus()) ? "Reserved (Pending Unlock)" : "—");

        String endTimeStr = booking.getEndTime() != null
                ? com.sanika.bicyclebookingapp.util.DateUtil.formatDateTime(booking.getEndTime())
                : "—";

        String depositStr = booking.getDepositAmount() > 0
                ? com.sanika.bicyclebookingapp.util.CurrencyUtil.formatInr(booking.getDepositAmount())
                : "None";

        String discountStr = booking.getDiscount() > 0
                ? "\nDiscount: -" + com.sanika.bicyclebookingapp.util.CurrencyUtil.formatInr(booking.getDiscount())
                : "";

        String details = String.format(
            "Booking Reference: %s\nStatus: %s\n\nBicycle: #%s\nPickup Station: %s\nReturn Station: %s\n\nBooked At: %s\nStarted: %s\nEnded: %s\n\nDuration: %s\nDistance: %.2f km\n\nFinal Fare: %s%s\nSecurity Deposit: %s",
            formattedRef,
            booking.getStatus(),
            bikeId,
            booking.getStationName() != null ? booking.getStationName() : "Station",
            returnStation,
            booking.getCreatedAt() != null ? com.sanika.bicyclebookingapp.util.DateUtil.formatDateTime(booking.getCreatedAt()) : "—",
            startTimeStr,
            endTimeStr,
            com.sanika.bicyclebookingapp.util.ValidationUtil.formatDuration(booking.getDurationMinutes()),
            booking.getDistanceKm(),
            com.sanika.bicyclebookingapp.util.ValidationUtil.formatFare(booking.getFare()),
            discountStr,
            depositStr
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
            .setTitle("Booking " + formattedRef)
            .setMessage(details);

        if (Booking.STATUS_PENDING.equalsIgnoreCase(booking.getStatus())) {
            builder.setPositiveButton("Scan & Unlock", (dialog, which) -> {
                Bundle args = new Bundle();
                args.putString("bookingId", booking.getBookingId());
                args.putString("bicycleId", booking.getBicycleId());
                args.putString("stationId", booking.getStationId());
                args.putString("mode", "UNLOCK");
                androidx.navigation.Navigation.findNavController(requireView())
                        .navigate(com.sanika.bicyclebookingapp.R.id.qrScanFragment, args);
            });
            builder.setNegativeButton("Cancel Reservation", (dialog, which) -> {
                bookingViewModel.cancelBooking(
                    booking.getBookingId(),
                    booking.getBicycleId(),
                    booking.getStationId()
                ).observe(getViewLifecycleOwner(), resource -> {
                    if (resource.isSuccess()) {
                        Toast.makeText(requireContext(), "Reservation cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            builder.setNeutralButton("Close", null);
        } else if (Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus())) {
            builder.setPositiveButton("View Active Ride", (dialog, which) -> {
                Bundle args = new Bundle();
                args.putString("bookingId", booking.getBookingId());
                args.putString("bicycleId", booking.getBicycleId());
                args.putString("stationId", booking.getStationId() != null ? booking.getStationId() : "");
                androidx.navigation.Navigation.findNavController(requireView())
                        .navigate(com.sanika.bicyclebookingapp.R.id.activeRideFragment, args);
            });
            builder.setNegativeButton("Close", null);
        } else {
            builder.setPositiveButton("Close", null);
        }

        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
