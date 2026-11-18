package com.sanika.bicyclebookingapp.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.AutoBillingManager;
import com.sanika.bicyclebookingapp.util.FareCalculator;
import com.sanika.bicyclebookingapp.util.StationProximityChecker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Foreground service that runs during an active ride.
 *
 * Responsibilities:
 * 1. Tracks GPS location every 10 seconds
 * 2. Calculates cumulative distance travelled
 * 3. Computes fare = base_fare + (distance * rate_per_km) + (time * rate_per_min)
 * 4. Updates Firestore every 30 seconds with live location, distance, fare
 * 5. Checks if user is near a station (auto-end ride)
 * 6. Shows persistent notification with live fare
 *
 * Ride ends ONLY when:
 * - User scans QR at an authorized station
 * - GPS detects user is within 50m of a station
 */
public class RideTrackingService extends Service {

    public static final String ACTION_START  = "ACTION_START_RIDE";
    public static final String ACTION_STOP   = "ACTION_STOP_RIDE";

    public static final String EXTRA_BOOKING_ID  = "bookingId";
    public static final String EXTRA_BICYCLE_ID  = "bicycleId";
    public static final String EXTRA_STATION_ID  = "stationId";
    public static final String EXTRA_START_TIME  = "startTime";

    private static final String CHANNEL_ID   = "ride_tracking_channel";
    private static final int    NOTIF_ID     = 2001;

    // Location update interval
    private static final long LOCATION_INTERVAL_MS = 10_000; // 10 seconds
    // Firestore update interval
    private static final long FIRESTORE_UPDATE_INTERVAL_MS = 30_000; // 30 seconds
    // Station proximity threshold
    private static final float STATION_RADIUS_METERS = 50f;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;

    private String bookingId;
    private String bicycleId;
    private String stationId;
    private long   startTimeMs;

    private Location lastLocation;
    private double   totalDistanceKm  = 0.0;
    private double   currentFare      = 0.0;
    private long     lastFirestoreUpdateMs  = 0;
    private long     lastHourlyBillingMs    = 0; // tracks last hourly billing check
    private double   depositPaid            = AutoBillingManager.SECURITY_DEPOSIT;

    private AutoBillingManager autoBillingManager;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if (ACTION_START.equals(intent.getAction())) {
            bookingId   = intent.getStringExtra(EXTRA_BOOKING_ID);
            bicycleId   = intent.getStringExtra(EXTRA_BICYCLE_ID);
            stationId   = intent.getStringExtra(EXTRA_STATION_ID);
            startTimeMs = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis());

            startForeground(NOTIF_ID, buildNotification("Ride in progress...", "₹0.00"));
            startLocationUpdates();
            autoBillingManager = new AutoBillingManager(this);
            lastHourlyBillingMs = System.currentTimeMillis();

        } else if (ACTION_STOP.equals(intent.getAction())) {
            stopRideTracking();
        }

        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(5_000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location newLocation = result.getLastLocation();
                if (newLocation == null) return;

                onNewLocation(newLocation);
            }
        };

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void onNewLocation(Location newLocation) {
        // Filter out inaccurate GPS readings
        // Only use location if accuracy is within 50 meters
        if (newLocation.getAccuracy() > 50f) return;

        // Calculate distance from last known location
        if (lastLocation != null) {
            float distanceMeters = lastLocation.distanceTo(newLocation);

            // Filter GPS noise:
            // - Ignore movements less than 5 meters (stationary noise)
            // - Ignore jumps more than 100 meters in 10 seconds (impossible on bicycle)
            if (distanceMeters >= 5f && distanceMeters <= 100f) {
                totalDistanceKm += distanceMeters / 1000.0;
            }
        }
        lastLocation = newLocation;

        // Calculate current fare
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        currentFare = FareCalculator.calculate(totalDistanceKm, elapsedMs);

        // Update notification with live fare
        updateNotification(currentFare, totalDistanceKm);

        // Update Firestore every 30 seconds
        long now = System.currentTimeMillis();
        if (now - lastFirestoreUpdateMs >= FIRESTORE_UPDATE_INTERVAL_MS) {
            lastFirestoreUpdateMs = now;
            updateFirestore(newLocation);
        }

        // Check hourly auto-billing (every 60 minutes)
        if (now - lastHourlyBillingMs >= 60 * 60 * 1000) {
            lastHourlyBillingMs = now;
            if (autoBillingManager != null) {
                autoBillingManager.checkAndBill(
                    bookingId, null, startTimeMs, currentFare, depositPaid);
            }
        }

        // Check if user is near a station → auto-end ride
        checkStationProximity(newLocation);
    }

    private void updateFirestore(Location location) {
        if (bookingId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentLocation", new GeoPoint(
                location.getLatitude(), location.getLongitude()));
        updates.put("distanceKm", totalDistanceKm);
        updates.put("currentFare", currentFare);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("bookings").document(bookingId).update(updates);

        // Also update bicycle last location
        db.collection("bicycles").document(bicycleId)
            .update("lastLocation", new GeoPoint(
                    location.getLatitude(), location.getLongitude()),
                    "lastLocationTime", FieldValue.serverTimestamp());
    }

    /**
     * Checks if user is within STATION_RADIUS_METERS of any station.
     * If yes, automatically ends the ride.
     */
    private void checkStationProximity(Location userLocation) {
        StationProximityChecker.findNearestStation(db, userLocation, STATION_RADIUS_METERS,
            (nearestStationId, nearestStationName, distanceMeters) -> {
                if (nearestStationId != null) {
                    // User is at a station — end the ride automatically
                    endRideAtStation(nearestStationId, nearestStationName, userLocation);
                }
            });
    }

    /**
     * Called when GPS confirms user is at a station.
     * Finalizes the ride and fare.
     */
    private void endRideAtStation(String returnStationId, String returnStationName,
                                   Location dropLocation) {
        if (bookingId == null) return;

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        long durationMinutes = Math.max(1, elapsedMs / 60000);
        double finalFare = FareCalculator.calculate(totalDistanceKm, elapsedMs);

        Map<String, Object> updates = new HashMap<>();
        updates.put("endTime", new Date());
        updates.put("status", "COMPLETED");
        updates.put("fare", finalFare);
        updates.put("durationMinutes", durationMinutes);
        updates.put("distanceKm", totalDistanceKm);
        updates.put("returnStationId", returnStationId);
        updates.put("returnStationName", returnStationName);
        updates.put("dropOffLocation", new GeoPoint(
                dropLocation.getLatitude(), dropLocation.getLongitude()));
        updates.put("endedBy", "GPS_STATION_DETECTION");

        db.collection("bookings").document(bookingId).update(updates)
            .addOnSuccessListener(unused -> {
                // Mark bicycle available at the return station
                db.collection("bicycles").document(bicycleId)
                    .update("available", true,
                            "stationId", returnStationId,
                            "stationName", returnStationName);

                // Increment station available count
                db.collection("stations").document(returnStationId)
                    .update("availableBicycles",
                            com.google.firebase.firestore.FieldValue.increment(1));

                // Broadcast ride ended to UI
                Intent broadcast = new Intent("RIDE_ENDED_BY_GPS");
                broadcast.putExtra("bookingId", bookingId);
                broadcast.putExtra("fare", (float) finalFare);
                broadcast.putExtra("durationMinutes", durationMinutes);
                broadcast.putExtra("distanceKm", (float) totalDistanceKm);
                sendBroadcast(broadcast);

                stopRideTracking();
            });
    }

    public void stopRideTracking() {
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
        stopForeground(true);
        stopSelf();
    }

    private void updateNotification(double fare, double distanceKm) {
        String fareText = String.format("₹%.2f", fare);
        String distText = String.format("%.2f km", distanceKm);
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIF_ID, buildNotification(distText + " travelled", fareText));
    }

    private Notification buildNotification(String content, String fare) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("CycleGo — Ride Active  " + fare)
                .setContentText(content)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Ride Tracking", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows live fare during active ride");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }
}
