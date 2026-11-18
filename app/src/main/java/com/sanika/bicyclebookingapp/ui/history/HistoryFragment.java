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
                        .setTitle("Delete Record")
                        .setMessage("Remove this ride from your history? This cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> deleteBooking(booking))
                        .setNegativeButton("Cancel", null)
                        .show();
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
     * Deletes the booking document from Firestore.
     * Only deletes the record — does NOT affect bicycle availability
     * since the ride is already completed or cancelled.
     */
    private void deleteBooking(Booking booking) {
        FirebaseFirestore.getInstance()
            .collection("bookings")
            .document(booking.getBookingId())
            .delete()
            .addOnSuccessListener(unused ->
                Toast.makeText(requireContext(),
                    "Deleted from history", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(requireContext(),
                    "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
