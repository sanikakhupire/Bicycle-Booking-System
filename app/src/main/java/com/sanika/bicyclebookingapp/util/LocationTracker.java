package com.sanika.bicyclebookingapp.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated Superseded by {@link com.sanika.bicyclebookingapp.service.RideTrackingService}.
 * Preserved for historical reference and backward compatibility.
 */
@Deprecated
public class LocationTracker {

    private final FusedLocationProviderClient fusedClient;
    private final FirebaseFirestore db;
    private LocationCallback locationCallback;
    private final String bookingId;
    private final String bicycleId;

    // Update location every 30 seconds during ride
    private static final long INTERVAL_MS = 30_000;

    public LocationTracker(Context context, String bookingId, String bicycleId) {
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
        this.db = FirebaseFirestore.getInstance();
        this.bookingId = bookingId;
        this.bicycleId = bicycleId;
    }

    /** Start sending location updates to Firestore. */
    public void startTracking(Context context) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
                .setMinUpdateIntervalMillis(15_000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                android.location.Location location = result.getLastLocation();
                if (location == null) return;

                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                // Update booking's current location
                Map<String, Object> bookingUpdate = new HashMap<>();
                bookingUpdate.put("currentLocation", geoPoint);
                db.collection("bookings").document(bookingId).update(bookingUpdate);

                // Update bicycle's last known location
                Map<String, Object> bicycleUpdate = new HashMap<>();
                bicycleUpdate.put("lastLocation", geoPoint);
                bicycleUpdate.put("lastLocationTime",
                        com.google.firebase.firestore.FieldValue.serverTimestamp());
                db.collection("bicycles").document(bicycleId).update(bicycleUpdate);
            }
        };

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    /** Stop location updates — call when ride ends. */
    public void stopTracking() {
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }
}
