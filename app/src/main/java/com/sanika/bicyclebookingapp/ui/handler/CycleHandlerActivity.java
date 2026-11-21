package com.sanika.bicyclebookingapp.ui.handler;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.databinding.ActivityCycleHandlerBinding;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.ui.auth.AuthActivity;
import com.sanika.bicyclebookingapp.ui.handler.adapter.HandlerBicycleAdapter;
import com.sanika.bicyclebookingapp.util.FareCalculator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cycle Handler Portal for ground staff:
 * - Station fleet status & maintenance management
 * - Booking verification and manual ride dispatch
 * - Bicycle check-in, docking & ride completion verification
 */
public class CycleHandlerActivity extends AppCompatActivity {

    private ActivityCycleHandlerBinding binding;
    private FirebaseFirestore db;
    private HandlerBicycleAdapter bicycleAdapter;

    private final List<Station> stationsList = new ArrayList<>();
    private Station selectedStation;
    private Booking activeVerifiedBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCycleHandlerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // 1. Verify Authentication & Role
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        binding.getRoot().setVisibility(View.INVISIBLE);
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(snap -> {
                    User user = snap.toObject(User.class);
                    if (user != null && (user.isHandler() || user.isAdmin())) {
                        binding.getRoot().setVisibility(View.VISIBLE);
                        initUi();
                    } else {
                        Toast.makeText(this, "Access denied. Cycle Handler privileges required.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void initUi() {
        setupRecyclerView();
        setupStationSpinner();
        setupBookingVerification();
        setupLogout();
    }

    private void setupRecyclerView() {
        bicycleAdapter = new HandlerBicycleAdapter(this::toggleBicycleMaintenance);
        binding.rvStationFleet.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStationFleet.setAdapter(bicycleAdapter);
    }

    private void setupStationSpinner() {
        db.collection("stations").get().addOnSuccessListener(snapshots -> {
            stationsList.clear();
            List<String> stationNames = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots) {
                Station station = doc.toObject(Station.class);
                if (station != null) {
                    station.setStationId(doc.getId());
                    stationsList.add(station);
                    stationNames.add(station.getName());
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_dropdown_item, stationNames);
            binding.spinnerStation.setAdapter(adapter);

            binding.spinnerStation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedStation = stationsList.get(position);
                    updateStationInfo(selectedStation);
                    loadStationFleet(selectedStation.getStationId());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void updateStationInfo(Station station) {
        if (station == null) return;
        binding.tvAvailableCount.setText("Available: " + station.getAvailableBicycles());
        binding.tvCapacityCount.setText("Capacity: " + station.getTotalBicycles());
    }

    private void loadStationFleet(String stationId) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyFleet.setVisibility(View.GONE);

        db.collection("bicycles")
                .whereEqualTo("stationId", stationId)
                .addSnapshotListener((snapshots, error) -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (error != null || snapshots == null) return;

                    List<Bicycle> bicycles = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Bicycle b = doc.toObject(Bicycle.class);
                        if (b != null) {
                            b.setBicycleId(doc.getId());
                            bicycles.add(b);
                        }
                    }

                    bicycleAdapter.submitList(bicycles);
                    binding.tvEmptyFleet.setVisibility(bicycles.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void toggleBicycleMaintenance(Bicycle bicycle) {
        if (bicycle == null || selectedStation == null) return;

        boolean willBeInMaintenance = !bicycle.isUnderMaintenance();
        String message = willBeInMaintenance
                ? "Flag this bicycle for maintenance and remove from active fleet?"
                : "Return this bicycle to active fleet?";

        new AlertDialog.Builder(this)
                .setTitle("Update Bicycle Status")
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    db.runTransaction(transaction -> {
                        var bikeRef = db.collection("bicycles").document(bicycle.getBicycleId());
                        var stationRef = db.collection("stations").document(selectedStation.getStationId());

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("underMaintenance", willBeInMaintenance);
                        updates.put("available", !willBeInMaintenance);
                        transaction.update(bikeRef, updates);

                        // If moving into maintenance, decrement available bicycles count; if back to active, increment
                        long delta = willBeInMaintenance ? -1L : 1L;
                        transaction.update(stationRef, "availableBicycles", FieldValue.increment(delta));

                        return null;
                    }).addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Bicycle status updated.", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBookingVerification() {
        binding.btnVerifyBooking.setOnClickListener(v -> {
            String bookingId = binding.etBookingId.getText() != null
                    ? binding.etBookingId.getText().toString().trim() : "";
            if (TextUtils.isEmpty(bookingId)) {
                binding.etBookingId.setError("Enter Booking ID");
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);
            db.collection("bookings").document(bookingId).get()
                    .addOnSuccessListener(doc -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (!doc.exists()) {
                            Toast.makeText(this, "Booking not found.", Toast.LENGTH_SHORT).show();
                            binding.layoutBookingDetails.setVisibility(View.GONE);
                            return;
                        }

                        activeVerifiedBooking = doc.toObject(Booking.class);
                        if (activeVerifiedBooking != null) {
                            activeVerifiedBooking.setBookingId(doc.getId());
                            displayBookingDetails(activeVerifiedBooking);
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error fetching booking: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });

        binding.btnStartRide.setOnClickListener(v -> {
            if (activeVerifiedBooking == null) return;

            db.collection("bookings").document(activeVerifiedBooking.getBookingId())
                    .update("status", Booking.STATUS_ACTIVE, "startTime", new Date())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Ride officially started!", Toast.LENGTH_SHORT).show();
                        binding.btnStartRide.setEnabled(false);
                        binding.btnCompleteReturn.setEnabled(true);
                        binding.tvBookingStatus.setText("Status: ACTIVE (Dispatched)");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Failed to start ride: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        binding.btnCompleteReturn.setOnClickListener(v -> {
            if (activeVerifiedBooking == null || selectedStation == null) return;

            long startMs = activeVerifiedBooking.getStartTime() != null
                    ? activeVerifiedBooking.getStartTime().getTime()
                    : System.currentTimeMillis();
            long elapsedMs = System.currentTimeMillis() - startMs;
            long durationMinutes = Math.max(1, elapsedMs / 60000);
            double distanceKm = activeVerifiedBooking.getDistanceKm() > 0
                    ? activeVerifiedBooking.getDistanceKm() : 0.0;
            double fare = FareCalculator.calculate(distanceKm, elapsedMs);

            new AlertDialog.Builder(this)
                    .setTitle("Complete Ride & Dock")
                    .setMessage(String.format("Dock bicycle at %s?\nDuration: %d min\nCalculated Fare: ₹%.2f",
                            selectedStation.getName(), durationMinutes, fare))
                    .setPositiveButton("Complete", (dialog, which) -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", Booking.STATUS_COMPLETED);
                        updates.put("endTime", new Date());
                        updates.put("durationMinutes", durationMinutes);
                        updates.put("fare", fare);
                        updates.put("returnStationId", selectedStation.getStationId());
                        updates.put("returnStationName", selectedStation.getName());
                        updates.put("endedBy", "CYCLE_HANDLER");

                        db.runTransaction(transaction -> {
                            var bookingRef = db.collection("bookings").document(activeVerifiedBooking.getBookingId());
                            transaction.update(bookingRef, updates);

                            var bikeRef = db.collection("bicycles").document(activeVerifiedBooking.getBicycleId());
                            transaction.update(bikeRef,
                                    "available", true,
                                    "stationId", selectedStation.getStationId(),
                                    "stationName", selectedStation.getName());

                            var stationRef = db.collection("stations").document(selectedStation.getStationId());
                            transaction.update(stationRef, "availableBicycles", FieldValue.increment(1));

                            return null;
                        }).addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Bicycle docked & ride completed!", Toast.LENGTH_LONG).show();
                            binding.layoutBookingDetails.setVisibility(View.GONE);
                            binding.btnStartRide.setEnabled(false);
                            binding.btnCompleteReturn.setEnabled(false);
                            binding.etBookingId.setText("");
                            activeVerifiedBooking = null;
                        }).addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to complete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void displayBookingDetails(Booking booking) {
        binding.layoutBookingDetails.setVisibility(View.VISIBLE);
        binding.tvBookingStatus.setText("Status: " + booking.getStatus());
        binding.tvRiderDetails.setText("User ID: " + booking.getUserId());
        binding.tvBicycleDetails.setText("Bicycle: " + booking.getBicycleId() +
                " | Station: " + booking.getStationName());

        boolean isPending = Booking.STATUS_PENDING.equalsIgnoreCase(booking.getStatus());
        boolean isActive = Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus());

        binding.btnStartRide.setEnabled(isPending);
        binding.btnCompleteReturn.setEnabled(isActive);
    }

    private void setupLogout() {
        binding.btnLogoutHandler.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
