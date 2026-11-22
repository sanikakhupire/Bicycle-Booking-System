package com.sanika.bicyclebookingapp.ui.station;

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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentStationDetailBinding;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.ui.station.adapter.BicycleAdapter;
import com.sanika.bicyclebookingapp.viewmodel.BicycleViewModel;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;
import com.sanika.bicyclebookingapp.viewmodel.StationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows station info, fleet discovery by type, and bicycle details.
 * Prevents duplicate bookings and booking of unavailable/damaged bicycles.
 */
public class StationDetailFragment extends Fragment {

    private FragmentStationDetailBinding binding;
    private StationViewModel stationViewModel;
    private BicycleViewModel bicycleViewModel;
    private BookingViewModel bookingViewModel;
    private BicycleAdapter adapter;
    private String stationId;
    private String stationName;
    private final List<Bicycle> allBicycles = new ArrayList<>();
    private String selectedTypeFilter = "All";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStationDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stationId = StationDetailFragmentArgs.fromBundle(getArguments()).getStationId();

        stationViewModel = new ViewModelProvider(this).get(StationViewModel.class);
        bicycleViewModel = new ViewModelProvider(this).get(BicycleViewModel.class);
        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);

        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnRetryBicycles.setOnClickListener(v -> loadBicycles());

        setupFilterChips();
        setupBicycleList();
        loadStation();
        loadBicycles();
    }

    private void setupFilterChips() {
        binding.chipGroupBicycleType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipStandard) {
                selectedTypeFilter = "Standard";
            } else if (checkedId == R.id.chipElectric) {
                selectedTypeFilter = "Electric";
            } else if (checkedId == R.id.chipMountain) {
                selectedTypeFilter = "Mountain";
            } else {
                selectedTypeFilter = "All";
            }
            filterBicycles();
        });
    }

    private void filterBicycles() {
        if ("All".equalsIgnoreCase(selectedTypeFilter)) {
            adapter.submitList(new ArrayList<>(allBicycles));
            updateEmptyState(allBicycles.isEmpty(), null);
        } else {
            List<Bicycle> filtered = new ArrayList<>();
            for (Bicycle b : allBicycles) {
                if (b.getType() != null && b.getType().equalsIgnoreCase(selectedTypeFilter)) {
                    filtered.add(b);
                }
            }
            adapter.submitList(filtered);
            updateEmptyState(filtered.isEmpty(), "No " + selectedTypeFilter + " bicycles available at this station.");
        }
    }

    private void updateEmptyState(boolean isEmpty, String customMessage) {
        if (binding == null) return;
        if (isEmpty) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.tvNoBicycles.setText(customMessage != null ? customMessage : getString(R.string.error_no_bicycles));
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void loadStation() {
        stationViewModel.getStation(stationId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null && binding != null) {
                stationName = resource.data.getName();
                binding.tvStationName.setText(stationName);
                binding.tvAddress.setText(resource.data.getAddress());
                binding.tvAvailableCount.setText(
                        resource.data.getAvailableBicycles() + " / "
                        + resource.data.getTotalBicycles() + " bicycles available");
            }
        });
    }

    private void setupBicycleList() {
        adapter = new BicycleAdapter(new BicycleAdapter.OnBicycleClickListener() {
            @Override
            public void onBicycleClick(Bicycle bicycle) {
                attemptBooking(bicycle);
            }

            @Override
            public void onBicycleDetails(Bicycle bicycle) {
                showBicycleDetailsDialog(bicycle);
            }
        });

        binding.rvBicycles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBicycles.setAdapter(adapter);
    }

    private void loadBicycles() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);

        bicycleViewModel.getBicyclesByStation(stationId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    allBicycles.clear();
                    if (resource.data != null) {
                        allBicycles.addAll(resource.data);
                    }
                    filterBicycles();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    updateEmptyState(true, resource.message);
                    break;
            }
        });
    }

    private void attemptBooking(Bicycle bicycle) {
        // Prevent booking damaged or unavailable bicycles
        if (!bicycle.isAvailable()) {
            Toast.makeText(requireContext(),
                "This bicycle is currently " + bicycle.getStatus().toLowerCase() + " and cannot be booked.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user already has an active or pending booking
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(requireContext(), "Please sign in to book a bicycle", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        bookingViewModel.getActiveBooking(uid).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);

            if (resource.isSuccess() && resource.data != null) {
                Booking active = resource.data;
                new AlertDialog.Builder(requireContext())
                    .setTitle("Active Reservation Exists")
                    .setMessage("You already have an ongoing " + active.getStatus().toLowerCase()
                            + " booking at " + active.getStationName()
                            + ".\n\nYou cannot hold multiple bicycles simultaneously.")
                    .setPositiveButton("View Active Booking", (d, w) -> {
                        if (Booking.STATUS_PENDING.equalsIgnoreCase(active.getStatus())) {
                            Bundle args = new Bundle();
                            args.putString("bookingId", active.getBookingId());
                            args.putString("bicycleId", active.getBicycleId());
                            args.putString("stationId", active.getStationId());
                            args.putString("mode", "UNLOCK");
                            Navigation.findNavController(requireView()).navigate(R.id.qrScanFragment, args);
                        } else {
                            Bundle args = new Bundle();
                            args.putString("bookingId", active.getBookingId());
                            args.putString("bicycleId", active.getBicycleId());
                            args.putString("stationId", active.getStationId());
                            Navigation.findNavController(requireView()).navigate(R.id.activeRideFragment, args);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            } else {
                // User is clear to proceed
                StationDetailFragmentDirections.ActionStationDetailToBookingConfirm action =
                        StationDetailFragmentDirections.actionStationDetailToBookingConfirm(
                                bicycle.getBicycleId(), stationId, stationName != null ? stationName : "");
                Navigation.findNavController(requireView()).navigate(action);
            }
        });
    }

    private void showBicycleDetailsDialog(Bicycle bicycle) {
        String shortId = bicycle.getBicycleId() != null && bicycle.getBicycleId().length() >= 8
                ? bicycle.getBicycleId().substring(0, 8).toUpperCase()
                : (bicycle.getBicycleId() != null ? bicycle.getBicycleId() : "—");

        String type = bicycle.getType() != null ? bicycle.getType() : "Standard";
        String status = bicycle.getStatus() != null ? bicycle.getStatus() : (bicycle.isAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        String specs;
        if ("Electric".equalsIgnoreCase(type)) {
            specs = "• Motor: 250W Hub Motor\n• Battery Range: ~35 km\n• Assist Speed: 25 km/h max\n• Pedal assist with digital lock";
        } else if ("Mountain".equalsIgnoreCase(type)) {
            specs = "• Gears: 21-Speed Shimano\n• Suspension: Front Fork Suspension\n• Brakes: Dual Disc Brakes\n• Terrain: City & Rough trails";
        } else {
            specs = "• Gears: Single Speed / Urban Drive\n• Frame: Lightweight Alloy\n• Brakes: V-Brakes with safety reflector\n• Terrain: Urban Commutes";
        }

        String message = String.format(
            "Model: %s\nStation: %s\nStatus: %s\n\nSpecifications:\n%s\n\nPricing (INR):\n• Base Fee: ₹5.00\n• Rental Rate: ₹1.00 / minute\n• Distance Rate: ₹8.00 / km\n• Minimum Fare: ₹10.00\n• Security Deposit: ₹100.00 (Refundable)",
            type, stationName != null ? stationName : "Station", status, specs
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
            .setTitle("Bicycle #" + shortId + " (" + type + ")")
            .setMessage(message);

        if (bicycle.isAvailable()) {
            builder.setPositiveButton("Book This Bicycle", (d, w) -> attemptBooking(bicycle));
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
