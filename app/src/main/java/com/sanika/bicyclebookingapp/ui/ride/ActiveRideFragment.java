package com.sanika.bicyclebookingapp.ui.ride;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentActiveRideBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.service.RideTrackingService;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Active ride screen with live GPS map tracking, route polyline display,
 * timer recovery, battery-conscious location updates, and secure station return.
 */
public class ActiveRideFragment extends Fragment implements OnMapReadyCallback {

    private FragmentActiveRideBinding binding;
    private BookingViewModel bookingViewModel;

    private String bookingId;
    private String bicycleId;
    private String stationId;

    private GoogleMap googleMap;
    private Polyline routePolyline;
    private Marker riderMarker;
    private final List<LatLng> routePointsList = new ArrayList<>();

    private long startTimeMs = 0;
    private double currentDistanceKm = 0.0;
    private double currentFareInr = 0.0;
    private LatLng latestLocation;

    private String nearbyStationId;
    private String nearbyStationName;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private BroadcastReceiver rideTrackingReceiver;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMapLocation();
                    checkGpsEnabled();
                } else {
                    Toast.makeText(requireContext(),
                            "Location permission needed for route tracking.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentActiveRideBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);

        ActiveRideFragmentArgs args = ActiveRideFragmentArgs.fromBundle(getArguments());
        bookingId = args.getBookingId();
        bicycleId = args.getBicycleId();
        stationId = args.getStationId();

        setupMap();
        setupListeners();
        checkGpsEnabled();
        registerTrackingReceiver();
        recoverAndLoadBooking();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapRide);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Initialize polyline
        PolylineOptions polyOptions = new PolylineOptions()
                .width(12f)
                .color(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                .geodesic(true);
        routePolyline = googleMap.addPolyline(polyOptions);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMapLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Draw already restored points
        if (!routePointsList.isEmpty()) {
            routePolyline.setPoints(new ArrayList<>(routePointsList));
            LatLng lastPoint = routePointsList.get(routePointsList.size() - 1);
            updateRiderMarker(lastPoint);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPoint, 16f));
        }
    }

    private void enableMapLocation() {
        if (googleMap != null && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {}
        }
    }

    private void checkGpsEnabled() {
        if (binding == null) return;
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        binding.cardGpsWarning.setVisibility(isGpsEnabled ? View.GONE : View.VISIBLE);
        binding.btnEnableGps.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });
    }

    private void setupListeners() {
        // Return bicycle button → opens QR scanner in RETURN mode
        binding.btnReturnBicycle.setOnClickListener(v -> {
            Bundle returnBundle = new Bundle();
            returnBundle.putString("bookingId", bookingId);
            returnBundle.putString("bicycleId", bicycleId);
            returnBundle.putString("stationId", stationId != null ? stationId : "");
            returnBundle.putString("mode", com.sanika.bicyclebookingapp.ui.qr.QrScanFragment.MODE_RETURN);
            Navigation.findNavController(requireView())
                    .navigate(R.id.qrScanFragment, returnBundle);
        });

        // 1-Tap Quick Dock button when near a station
        binding.btnDockAtStation.setOnClickListener(v -> confirmQuickDock());

        // Center location FAB
        binding.fabCenterLocation.setOnClickListener(v -> {
            if (googleMap != null && latestLocation != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestLocation, 17f));
            } else {
                Toast.makeText(requireContext(), "Acquiring GPS location...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmQuickDock() {
        if (nearbyStationId == null) return;

        String stationName = nearbyStationName != null ? nearbyStationName : "the station";
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Bicycle Return")
                .setMessage("Are you sure you want to dock your bicycle at " + stationName + " and end your ride?")
                .setPositiveButton("Dock & End Ride", (dialog, which) -> executeEndRide(nearbyStationId, stationName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeEndRide(String returnStationId, String returnStationName) {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnReturnBicycle.setEnabled(false);
        binding.btnDockAtStation.setEnabled(false);

        // Stop tracking service
        Intent stopService = new Intent(requireContext(), RideTrackingService.class);
        stopService.setAction(RideTrackingService.ACTION_STOP);
        requireContext().startService(stopService);

        bookingViewModel.endRide(bookingId, bicycleId, returnStationId)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (binding == null) return;
                    switch (resource.status) {
                        case LOADING:
                            binding.progressBar.setVisibility(View.VISIBLE);
                            break;
                        case SUCCESS:
                            binding.progressBar.setVisibility(View.GONE);
                            if (resource.data != null) {
                                navigateToRideComplete((float) resource.data.getFare(),
                                        resource.data.getDurationMinutes());
                            }
                            break;
                        case ERROR:
                            binding.progressBar.setVisibility(View.GONE);
                            binding.btnReturnBicycle.setEnabled(true);
                            binding.btnDockAtStation.setEnabled(true);
                            binding.tvError.setVisibility(View.VISIBLE);
                            binding.tvError.setText("Failed to end ride: " + resource.message);
                            break;
                    }
                });
    }

    /**
     * Loads the booking document from Firestore to recover start time,
     * route coordinates, and distance after app restart or crash.
     */
    private void recoverAndLoadBooking() {
        if (bookingId == null || bookingId.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("bookings").document(bookingId).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists() || binding == null) return;

                    Booking booking = snap.toObject(Booking.class);
                    if (booking != null) {
                        // Check if ride was already ended
                        if (Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
                            navigateToRideComplete((float) booking.getFare(), booking.getDurationMinutes());
                            return;
                        }

                        if (booking.getStartTime() != null) {
                            startTimeMs = booking.getStartTime().getTime();
                        } else {
                            startTimeMs = System.currentTimeMillis();
                        }

                        if (booking.getDistanceKm() > 0) {
                            currentDistanceKm = booking.getDistanceKm();
                            binding.tvDistance.setText(String.format("%.2f km", currentDistanceKm));
                        }

                        if (booking.getCurrentFare() > 0) {
                            currentFareInr = booking.getCurrentFare();
                            binding.tvEstimatedFare.setText(String.format("₹%.2f", currentFareInr));
                        }

                        // Restore previous route points on the map
                        if (booking.getRoutePoints() != null && !booking.getRoutePoints().isEmpty()) {
                            routePointsList.clear();
                            for (GeoPoint gp : booking.getRoutePoints()) {
                                routePointsList.add(new LatLng(gp.getLatitude(), gp.getLongitude()));
                            }
                            if (routePolyline != null && !routePointsList.isEmpty()) {
                                routePolyline.setPoints(new ArrayList<>(routePointsList));
                                LatLng last = routePointsList.get(routePointsList.size() - 1);
                                latestLocation = last;
                                updateRiderMarker(last);
                                if (googleMap != null) {
                                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(last, 16f));
                                }
                            }
                        }
                    } else {
                        startTimeMs = System.currentTimeMillis();
                    }

                    startTrackingService();
                    startTimer();
                    listenToFirestoreUpdates();
                })
                .addOnFailureListener(e -> {
                    startTimeMs = System.currentTimeMillis();
                    startTrackingService();
                    startTimer();
                    listenToFirestoreUpdates();
                });
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

    private void registerTrackingReceiver() {
        rideTrackingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || binding == null) return;
                String action = intent.getAction();

                if (RideTrackingService.ACTION_LOCATION_UPDATE.equals(action)) {
                    double lat = intent.getDoubleExtra(RideTrackingService.EXTRA_LATITUDE, 0.0);
                    double lng = intent.getDoubleExtra(RideTrackingService.EXTRA_LONGITUDE, 0.0);
                    double dist = intent.getDoubleExtra(RideTrackingService.EXTRA_DISTANCE_KM, 0.0);
                    double fare = intent.getDoubleExtra(RideTrackingService.EXTRA_CURRENT_FARE, 0.0);

                    if (lat != 0.0 && lng != 0.0) {
                        LatLng newPoint = new LatLng(lat, lng);
                        latestLocation = newPoint;
                        routePointsList.add(newPoint);

                        if (routePolyline != null) {
                            routePolyline.setPoints(new ArrayList<>(routePointsList));
                        }
                        updateRiderMarker(newPoint);
                    }

                    currentDistanceKm = dist;
                    currentFareInr = fare;
                    binding.tvDistance.setText(String.format("%.2f km", currentDistanceKm));
                    binding.tvEstimatedFare.setText(String.format("₹%.2f", currentFareInr));

                } else if (RideTrackingService.ACTION_STATION_PROXIMITY.equals(action)) {
                    nearbyStationId = intent.getStringExtra(RideTrackingService.EXTRA_NEARBY_STATION_ID);
                    nearbyStationName = intent.getStringExtra(RideTrackingService.EXTRA_NEARBY_STATION_NAME);
                    float distanceM = intent.getFloatExtra(RideTrackingService.EXTRA_NEARBY_DISTANCE_M, 0f);

                    if (nearbyStationName != null && binding != null) {
                        binding.cardProximityDock.setVisibility(View.VISIBLE);
                        binding.tvProximityStation.setText(String.format(
                                "🅿️ Station Detected: %s (%.0fm away)\nReady to return your bicycle?",
                                nearbyStationName, distanceM));
                    }

                } else if (RideTrackingService.ACTION_RIDE_ENDED_BY_GPS.equals(action)) {
                    float fare = intent.getFloatExtra("fare", 0f);
                    long duration = intent.getLongExtra("durationMinutes", 0);
                    navigateToRideComplete(fare, duration);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(RideTrackingService.ACTION_LOCATION_UPDATE);
        filter.addAction(RideTrackingService.ACTION_STATION_PROXIMITY);
        filter.addAction(RideTrackingService.ACTION_RIDE_ENDED_BY_GPS);
        requireContext().registerReceiver(rideTrackingReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void updateRiderMarker(LatLng point) {
        if (googleMap == null) return;
        if (riderMarker == null) {
            riderMarker = googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        } else {
            riderMarker.setPosition(point);
        }
    }

    private void listenToFirestoreUpdates() {
        if (bookingId == null) return;

        FirebaseFirestore.getInstance().collection("bookings").document(bookingId)
                .addSnapshotListener((snap, error) -> {
                    if (snap == null || binding == null) return;

                    Double dist = snap.getDouble("distanceKm");
                    Double fare = snap.getDouble("currentFare");

                    if (dist != null && dist > currentDistanceKm) {
                        currentDistanceKm = dist;
                        binding.tvDistance.setText(String.format("%.2f km", currentDistanceKm));
                    }
                    if (fare != null && fare > currentFareInr) {
                        currentFareInr = fare;
                        binding.tvEstimatedFare.setText(String.format("₹%.2f", currentFareInr));
                    }

                    String status = snap.getString("status");
                    if (Booking.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                        Double finalFare = snap.getDouble("fare");
                        Long duration = snap.getLong("durationMinutes");
                        navigateToRideComplete(
                                finalFare != null ? finalFare.floatValue() : (float) currentFareInr,
                                duration != null ? duration : 1L);
                    }
                });
    }

    private void startTimer() {
        if (binding == null) return;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;
                long elapsedMs = Math.max(0, System.currentTimeMillis() - startTimeMs);
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
        try {
            Navigation.findNavController(requireView()).navigate(action);
        } catch (Exception ignored) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        checkGpsEnabled();
    }

    @Override
    public void onDestroyView() {
        timerHandler.removeCallbacks(timerRunnable);
        if (rideTrackingReceiver != null) {
            try {
                requireContext().unregisterReceiver(rideTrackingReceiver);
            } catch (Exception ignored) {}
            rideTrackingReceiver = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
