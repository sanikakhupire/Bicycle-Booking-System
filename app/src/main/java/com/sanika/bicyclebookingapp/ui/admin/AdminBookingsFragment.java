package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminBookingsBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminBookingAdapter;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Booking Management screen for Administrators.
 */
public class AdminBookingsFragment extends Fragment {

    private FragmentAdminBookingsBinding binding;
    private AdminViewModel adminViewModel;
    private AdminBookingAdapter adapter;

    private List<Booking> allBookings = new ArrayList<>();
    private String currentStatusFilter = "ALL";
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBookingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupRecyclerView();
        setupSearchAndFilters();
        loadBookings();
    }

    private void setupRecyclerView() {
        adapter = new AdminBookingAdapter(this::confirmCancelBooking);
        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookings.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
        binding.etSearchBooking.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.chipGroupBookingStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipBookPending) {
                currentStatusFilter = Booking.STATUS_PENDING;
            } else if (checkedId == R.id.chipBookActive) {
                currentStatusFilter = Booking.STATUS_ACTIVE;
            } else if (checkedId == R.id.chipBookCompleted) {
                currentStatusFilter = Booking.STATUS_COMPLETED;
            } else if (checkedId == R.id.chipBookCancelled) {
                currentStatusFilter = Booking.STATUS_CANCELLED;
            } else {
                currentStatusFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void loadBookings() {
        binding.progressBarBookings.setVisibility(View.VISIBLE);

        adminViewModel.getAllBookings().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarBookings.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                allBookings = resource.data;
                applyFilters();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        List<Booking> filtered = new ArrayList<>();

        for (Booking b : allBookings) {
            String status = b.getStatus() != null ? b.getStatus().toUpperCase() : Booking.STATUS_PENDING;

            boolean matchesStatus;
            if ("ALL".equals(currentStatusFilter)) {
                matchesStatus = true;
            } else if (Booking.STATUS_CANCELLED.equals(currentStatusFilter)) {
                matchesStatus = Booking.STATUS_CANCELLED.equalsIgnoreCase(status) || Booking.STATUS_EXPIRED.equalsIgnoreCase(status);
            } else {
                matchesStatus = currentStatusFilter.equalsIgnoreCase(status);
            }

            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || (b.getBookingId() != null && b.getBookingId().toLowerCase().contains(currentSearchQuery))
                    || (b.getBicycleId() != null && b.getBicycleId().toLowerCase().contains(currentSearchQuery))
                    || (b.getStationName() != null && b.getStationName().toLowerCase().contains(currentSearchQuery))
                    || (b.getUserId() != null && b.getUserId().toLowerCase().contains(currentSearchQuery));

            if (matchesStatus && matchesSearch) {
                filtered.add(b);
            }
        }

        adapter.setBookings(filtered);
        binding.tvEmptyBookings.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmCancelBooking(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Force Cancel Booking")
                .setMessage("Are you sure you want to cancel Booking #" + booking.getBookingId()
                        + " and immediately release Bicycle " + booking.getBicycleId() + " back to station inventory?")
                .setPositiveButton("Cancel & Release", (dialog, which) -> {
                    adminViewModel.forceCancelBooking(
                            booking.getBookingId(),
                            booking.getBicycleId(),
                            booking.getStationId(),
                            () -> Toast.makeText(requireContext(), "Booking cancelled and bicycle released!", Toast.LENGTH_SHORT).show(),
                            err -> Toast.makeText(requireContext(), "Error: " + err, Toast.LENGTH_LONG).show()
                    );
                })
                .setNegativeButton("Dismiss", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
