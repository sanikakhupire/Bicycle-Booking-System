package com.sanika.bicyclebookingapp.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentTrackBicyclesBinding;
import com.sanika.bicyclebookingapp.util.TheftDetector;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin screen — real-time map showing all bicycle locations.
 *
 * Marker colors:
 *  🟢 Green  = Available at station
 *  🔵 Blue   = In use (active ride) — moves in real-time
 *  🔴 Red    = Theft alert (available but moved from station)
 *  ⚫ Grey   = No location data yet
 */
public class TrackBicyclesFragment extends Fragment implements OnMapReadyCallback {

    private FragmentTrackBicyclesBinding binding;
    private GoogleMap googleMap;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Map of bicycleId → map marker (so we can update position in real-time)
    private final Map<String, Marker> bicycleMarkers = new HashMap<>();
    private ListenerRegistration bicycleListener;
    private ListenerRegistration bookingListener;

    // Track which bicycles have active rides (bicycleId → userId)
    private final Map<String, String> activeBicycles = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTrackBicyclesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.trackingMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Theft check button
        binding.btnCheckTheft.setOnClickListener(v -> runTheftCheck());

        // Legend
        binding.tvLegend.setText(
            "🟢 Available  🔵 In Use  🔴 Theft Alert  ⚫ No GPS");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Start listening to active bookings first
        listenToActiveBookings();
    }

    /**
     * Listen to all ACTIVE bookings to know which bicycles are in use.
     * Then listen to bicycle locations.
     */
    private void listenToActiveBookings() {
        bookingListener = db.collection("bookings")
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener((snapshots, error) -> {
                activeBicycles.clear();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String bicycleId = doc.getString("bicycleId");
                        String userId = doc.getString("userId");
                        if (bicycleId != null) {
                            activeBicycles.put(bicycleId, userId);
                        }
                    }
                }
                // Now listen to bicycle locations
                listenToBicycleLocations();
            });
    }

    /**
     * Real-time listener on all bicycles.
     * Updates map markers whenever any bicycle's location changes.
     */
    private void listenToBicycleLocations() {
        if (bicycleListener != null) bicycleListener.remove();

        bicycleListener = db.collection("bicycles")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || googleMap == null || snapshots == null) return;

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                boolean hasPoints = false;

                for (QueryDocumentSnapshot doc : snapshots) {
                    String bicycleId = doc.getId();
                    GeoPoint lastLocation = doc.getGeoPoint("lastLocation");
                    GeoPoint stationLocation = null;

                    // Try to get station location for comparison
                    String stationId = doc.getString("stationId");
                    Boolean available = doc.getBoolean("available");
                    String type = doc.getString("type");
                    String stationName = doc.getString("stationName");

                    if (lastLocation == null) {
                        // No GPS data — remove marker if exists
                        removeMarker(bicycleId);
                        continue;
                    }

                    LatLng position = new LatLng(
                            lastLocation.getLatitude(),
                            lastLocation.getLongitude());

                    boundsBuilder.include(position);
                    hasPoints = true;

                    // Determine marker color based on status
                    boolean isInUse = activeBicycles.containsKey(bicycleId);
                    float markerColor;
                    String statusText;

                    if (isInUse) {
                        markerColor = BitmapDescriptorFactory.HUE_AZURE; // Blue = in use
                        statusText = "IN USE";
                    } else if (Boolean.TRUE.equals(available)) {
                        markerColor = BitmapDescriptorFactory.HUE_GREEN; // Green = available
                        statusText = "AVAILABLE";
                    } else {
                        markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                        statusText = "UNAVAILABLE";
                    }

                    String title = (type != null ? type : "Bicycle") + " — " + statusText;
                    String snippet = "Station: " + (stationName != null ? stationName : "Unknown")
                            + "\nID: " + bicycleId.substring(0, 8).toUpperCase();

                    // Update existing marker or create new one
                    if (bicycleMarkers.containsKey(bicycleId)) {
                        Marker marker = bicycleMarkers.get(bicycleId);
                        if (marker != null) {
                            marker.setPosition(position);
                            marker.setTitle(title);
                            marker.setSnippet(snippet);
                            marker.setIcon(BitmapDescriptorFactory.defaultMarker(markerColor));
                        }
                    } else {
                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(title)
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                        if (marker != null) {
                            bicycleMarkers.put(bicycleId, marker);
                        }
                    }
                }

                // Update stats
                binding.tvStats.setText(
                    "Total: " + snapshots.size()
                    + "  |  In Use: " + activeBicycles.size()
                    + "  |  Available: " + (snapshots.size() - activeBicycles.size()));

                // Zoom to fit all markers
                if (hasPoints && bicycleMarkers.size() > 0) {
                    try {
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
                    } catch (Exception ignored) {}
                }
            });
    }

    private void removeMarker(String bicycleId) {
        Marker marker = bicycleMarkers.remove(bicycleId);
        if (marker != null) marker.remove();
    }

    private void runTheftCheck() {
        binding.tvTheftResult.setVisibility(View.VISIBLE);
        binding.tvTheftResult.setText("Checking...");
        binding.tvTheftResult.setTextColor(Color.GRAY);

        TheftDetector.checkForTheft(new TheftDetector.TheftCallback() {
            @Override
            public void onTheftDetected(String bicycleId, String stationName, double distanceKm) {
                // Mark that bicycle's marker red
                Marker marker = bicycleMarkers.get(bicycleId);
                if (marker != null) {
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED));
                    marker.setTitle("⚠️ THEFT ALERT");
                    marker.setSnippet(String.format("%.1f km from %s", distanceKm, stationName));
                    marker.showInfoWindow();
                }

                if (getContext() != null) {
                    binding.tvTheftResult.setText(
                        "⚠️ THEFT ALERT: Bicycle "
                        + bicycleId.substring(0, 6).toUpperCase()
                        + " is " + String.format("%.1f", distanceKm)
                        + "km from " + stationName);
                    binding.tvTheftResult.setTextColor(Color.RED);
                }
            }

            @Override
            public void onCheckComplete(int checked, int thefts) {
                if (thefts == 0 && getContext() != null) {
                    binding.tvTheftResult.setText(
                        "✓ All " + checked + " bicycles are safe");
                    binding.tvTheftResult.setTextColor(Color.parseColor("#4CAF50"));
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        // Remove Firestore listeners to prevent memory leaks
        if (bicycleListener != null) bicycleListener.remove();
        if (bookingListener != null) bookingListener.remove();
        super.onDestroyView();
        binding = null;
    }
}
