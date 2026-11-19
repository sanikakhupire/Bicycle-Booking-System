package com.sanika.bicyclebookingapp.ui.ride;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.databinding.FragmentActiveRideBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.service.RideTrackingService;

/**
 * Active ride screen.
 *
 * IMPORTANT: Ride does NOT end manually.
 * It ends ONLY when:
 *   1. User scans QR at an authorized station (QR validation)
 *   2. GPS detects user is within 50m of a station (auto-detection)
 *
 * The RideTrackingService runs in the background and handles both cases.
 * This fragment just displays live data from Firestore.
 */
public class ActiveRideFragment extends Fragment {

    private FragmentActiveRideBinding binding;

    private String bookingId;
    private String bicycleId;
    private String stationId;

    private long startTimeMs;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Receives broadcast from RideTrackingService when ride ends via GPS
    private BroadcastReceiver rideEndedReceiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentActiveRideBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ActiveRideFragmentArgs args = ActiveRideFragmentArgs.fromBundle(getArguments());
        bookingId = args.getBookingId();
        bicycleId = args.getBicycleId();
        stationId = args.getStationId();

        // Hide the manual end ride button — ride ends only at station
        binding.btnEndRide.setVisibility(View.GONE);

        // Show instruction
        binding.tvInstruction.setVisibility(View.VISIBLE);
        binding.tvInstruction.setText(
            "🚲 Ride in progress\n\nReturn to any station to end your ride.\n" +
            "Scan the station QR code OR ride within 50m of a station.");

        // Return bicycle button → opens QR scanner in RETURN mode
        binding.btnReturnBicycle.setOnClickListener(v -> {
            Bundle returnBundle = new Bundle();
            returnBundle.putString("bookingId", bookingId);
            returnBundle.putString("bicycleId", bicycleId);
            returnBundle.putString("stationId", stationId);
            returnBundle.putString("mode", com.sanika.bicyclebookingapp.ui.qr.QrScanFragment.MODE_RETURN);
            Navigation.findNavController(requireView())
                .navigate(com.sanika.bicyclebookingapp.R.id.qrScanFragment, returnBundle);
        });

        // Register broadcast receiver for GPS-based ride end
        registerRideEndedReceiver();

        // Load start time from Firestore, then start tracking service and timer
        FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener(snap -> {
                Booking booking = snap.toObject(Booking.class);
                if (booking != null && booking.getStartTime() != null) {
                    startTimeMs = booking.getStartTime().getTime();
                } else {
                    startTimeMs = System.currentTimeMillis();
                }
                startTrackingService();
                startTimer();
            })
            .addOnFailureListener(e -> {
                startTimeMs = System.currentTimeMillis();
                startTrackingService();
                startTimer();
            });

        // Listen to live Firestore updates for distance
        listenToRideUpdates();
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(requireContext(), RideTrackingService.class);
        serviceIntent.setAction(RideTrackingService.ACTION_START);
        serviceIntent.putExtra(RideTrackingService.EXTRA_BOOKING_ID, bookingId);
        serviceIntent.putExtra(RideTrackingService.EXTRA_BICYCLE_ID, bicycleId);
        serviceIntent.putExtra(RideTrackingService.EXTRA_STATION_ID, stationId);
        serviceIntent.putExtra(RideTrackingService.EXTRA_START_TIME, startTimeMs);
        requireContext().startForegroundService(serviceIntent);
    }

    private void listenToRideUpdates() {
        FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId)
            .addSnapshotListener((snap, error) -> {
                if (snap == null || binding == null) return;
                Double distanceKm = snap.getDouble("distanceKm");
                Double currentFare = snap.getDouble("currentFare");

                if (distanceKm != null) {
                    binding.tvDistance.setText(
                        String.format("Distance: %.2f km", distanceKm));
                }
                if (currentFare != null) {
                    binding.tvEstimatedFare.setText(
                        String.format("Current Fare: ₹%.2f", currentFare));
                }
            });
    }

    private void registerRideEndedReceiver() {
        rideEndedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("RIDE_ENDED_BY_GPS".equals(intent.getAction())) {
                    float fare = intent.getFloatExtra("fare", 0f);
                    long duration = intent.getLongExtra("durationMinutes", 0);
                    navigateToRideComplete(fare, duration);
                }
            }
        };
        IntentFilter filter = new IntentFilter("RIDE_ENDED_BY_GPS");
        requireContext().registerReceiver(rideEndedReceiver, filter,
                Context.RECEIVER_NOT_EXPORTED);
    }

    private void startTimer() {
        if (binding == null) return;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;
                long elapsedMs = System.currentTimeMillis() - startTimeMs;
                long seconds = (elapsedMs / 1000) % 60;
                long minutes = (elapsedMs / 60000) % 60;
                long hours   = elapsedMs / 3600000;
                binding.tvTimer.setText(
                    String.format("%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void navigateToRideComplete(float fare, long durationMinutes) {
        if (binding == null) return;
        timerHandler.removeCallbacks(timerRunnable);
        ActiveRideFragmentDirections.ActionActiveRideToRideComplete action =
                ActiveRideFragmentDirections.actionActiveRideToRideComplete(
                        bookingId, fare, durationMinutes);
        Navigation.findNavController(requireView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        timerHandler.removeCallbacks(timerRunnable);
        if (rideEndedReceiver != null) {
            try {
                requireContext().unregisterReceiver(rideEndedReceiver);
            } catch (Exception ignored) {}
        }
        super.onDestroyView();
        binding = null;
    }
}
