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
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.AutoBillingManager;
import com.sanika.bicyclebookingapp.util.FareCalculator;
import com.sanika.bicyclebookingapp.util.StationProximityChecker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Foreground service that runs during an active ride.
 *
 * Responsibilities:
 * 1. Tracks GPS location every 10 seconds (battery-conscious, displacement threshold)
 * 2. Collects route coordinates for live polyline map display and crash recovery
 * 3. Calculates cumulative distance travelled with noise filtering
 * 4. Computes fare = base_fare + (distance * rate_per_km) + (time * rate_per_min) in INR
 * 5. Updates Firestore periodically with live location, distance, fare, and route points
 * 6. Checks station proximity and broadcasts nearby station docking status
 * 7. Shows persistent notification with live distance and fare
 * 8. Never tracks location when ride is inactive
 */
public class RideTrackingService extends Service {

    public static final String ACTION_START  = "ACTION_START_RIDE";
    public static final String ACTION_STOP   = "ACTION_STOP_RIDE";

    public static final String ACTION_LOCATION_UPDATE   = "com.sanika.bicyclebookingapp.LOCATION_UPDATE";
    public static final String ACTION_STATION_PROXIMITY = "com.sanika.bicyclebookingapp.STATION_PROXIMITY";
    public static final String ACTION_RIDE_ENDED_BY_GPS = "com.sanika.bicyclebookingapp.RIDE_ENDED_BY_GPS";

    public static final String EXTRA_BOOKING_ID  = "bookingId";
    public static final String EXTRA_BICYCLE_ID  = "bicycleId";
    public static final String EXTRA_STATION_ID  = "stationId";
    public static final String EXTRA_START_TIME  = "startTime";

    public static final String EXTRA_LATITUDE          = "latitude";
    public static final String EXTRA_LONGITUDE         = "longitude";
    public static final String EXTRA_DISTANCE_KM       = "distanceKm";
    public static final String EXTRA_CURRENT_FARE      = "currentFare";
    public static final String EXTRA_ELAPSED_MS        = "elapsedMs";
    public static final String EXTRA_NEARBY_STATION_ID = "nearbyStationId";
    public static final String EXTRA_NEARBY_STATION_NAME = "nearbyStationName";
    public static final String EXTRA_NEARBY_DISTANCE_M = "nearbyDistanceM";

    private static final String CHANNEL_ID   = "ride_tracking_channel";
    private static final int    NOTIF_ID     = 2001;

    // Location update intervals
    private static final long LOCATION_INTERVAL_MS = 10_000; // 10 seconds
    private static final long FASTEST_INTERVAL_MS  = 5_000;  // 5 seconds
    private static final float MIN_DISPLACEMENT_M  = 5.0f;   // 5 meters

    // Firestore sync interval
    private static final long FIRESTORE_UPDATE_INTERVAL_MS = 30_000; // 30 seconds

    // Station proximity threshold for dock notification (100m)
    private static final float STATION_PROXIMITY_RADIUS_M = 100f;

    // Maximum coordinates stored in memory/Firestore to prevent payload bloat
    private static final int MAX_ROUTE_POINTS = 500;

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
    private long     lastHourlyBillingMs    = 0;
    private double   depositPaid            = AutoBillingManager.SECURITY_DEPOSIT;
    private boolean  proximityCheckEnabled  = false;

    private final List<GeoPoint> routeCoordinates = new ArrayList<>();
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
            recoverStateFromFirestore();
            startLocationUpdates();
            autoBillingManager = new AutoBillingManager(this);
            lastHourlyBillingMs = System.currentTimeMillis();

        } else if (ACTION_STOP.equals(intent.getAction())) {
            stopRideTracking();
        }

        return START_STICKY;
    }

    /**
     * Recovers existing distance, start time, and route points if service was
     * restarted after app crash or system low-memory kill.
     */
    private void recoverStateFromFirestore() {
        if (bookingId == null) return;

        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) return;

                    Booking booking = snap.toObject(Booking.class);
                    if (booking != null) {
                        if (booking.getStartTime() != null && startTimeMs <= 0) {
                            startTimeMs = booking.getStartTime().getTime();
                        }
                        if (booking.getDistanceKm() > totalDistanceKm) {
                            totalDistanceKm = booking.getDistanceKm();
                        }
                        if (booking.getRoutePoints() != null && !booking.getRoutePoints().isEmpty()) {
                            routeCoordinates.clear();
                            routeCoordinates.addAll(booking.getRoutePoints());
                        }
                        if (booking.getCurrentFare() > 0) {
                            currentFare = booking.getCurrentFare();
                        }
                        updateNotification(currentFare, totalDistanceKm);
                    }
                });
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                .setMinUpdateDistanceMeters(MIN_DISPLACEMENT_M)
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
        // Filter inaccurate GPS readings (only use readings within 35m accuracy)
        if (newLocation.getAccuracy() > 35f) return;

        // Calculate distance from last known location
        if (lastLocation != null) {
            float distanceMeters = lastLocation.distanceTo(newLocation);

            // Filter GPS noise:
            // - Ignore stationary jitter (< 5 meters)
            // - Ignore impossible jumps (> 100 meters in 10 seconds, equivalent to 36 km/h)
            if (distanceMeters >= 5f && distanceMeters <= 100f) {
                totalDistanceKm += distanceMeters / 1000.0;
                addRoutePoint(newLocation.getLatitude(), newLocation.getLongitude());
            }
        } else {
            // First valid GPS fix
            addRoutePoint(newLocation.getLatitude(), newLocation.getLongitude());
        }
        lastLocation = newLocation;

        // Calculate current fare
        long elapsedMs = Math.max(0, System.currentTimeMillis() - startTimeMs);
        currentFare = FareCalculator.calculate(totalDistanceKm, elapsedMs);

        // Broadcast live location to active UI
        broadcastLocationUpdate(newLocation, elapsedMs);

        // Update ongoing notification
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

        // Check station proximity to notify user of docking availability
        checkStationProximity(newLocation);
    }

    private void addRoutePoint(double lat, double lng) {
        if (routeCoordinates.size() >= MAX_ROUTE_POINTS) {
            // Keep points within budget by downsampling (removing every 2nd point)
            List<GeoPoint> downsampled = new ArrayList<>();
            for (int i = 0; i < routeCoordinates.size(); i += 2) {
                downsampled.add(routeCoordinates.get(i));
            }
            routeCoordinates.clear();
            routeCoordinates.addAll(downsampled);
        }
        routeCoordinates.add(new GeoPoint(lat, lng));
    }

    private void broadcastLocationUpdate(Location location, long elapsedMs) {
        Intent broadcast = new Intent(ACTION_LOCATION_UPDATE);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_LATITUDE, location.getLatitude());
        broadcast.putExtra(EXTRA_LONGITUDE, location.getLongitude());
        broadcast.putExtra(EXTRA_DISTANCE_KM, totalDistanceKm);
        broadcast.putExtra(EXTRA_CURRENT_FARE, currentFare);
        broadcast.putExtra(EXTRA_ELAPSED_MS, elapsedMs);
        sendBroadcast(broadcast);
    }

    private void updateFirestore(Location location) {
        if (bookingId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentLocation", new GeoPoint(
                location.getLatitude(), location.getLongitude()));
        updates.put("distanceKm", Math.round(totalDistanceKm * 100.0) / 100.0);
        updates.put("currentFare", currentFare);
        updates.put("routePoints", routeCoordinates);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("bookings").document(bookingId).update(updates);

        // Also update bicycle last location
        if (bicycleId != null) {
            db.collection("bicycles").document(bicycleId)
                    .update("lastLocation", new GeoPoint(
                                    location.getLatitude(), location.getLongitude()),
                            "lastLocationTime", FieldValue.serverTimestamp());
        }
    }

    /**
     * Checks if user is within STATION_PROXIMITY_RADIUS_M of any station.
     * Broadcasts proximity status so the UI can notify the rider and offer 1-tap docking.
     */
    private void checkStationProximity(Location userLocation) {
        StationProximityChecker.findNearestStation(db, userLocation, STATION_PROXIMITY_RADIUS_M,
                (nearestStationId, nearestStationName, distanceMeters) -> {
                    if (nearestStationId != null && distanceMeters <= STATION_PROXIMITY_RADIUS_M) {
                        Intent proxBroadcast = new Intent(ACTION_STATION_PROXIMITY);
                        proxBroadcast.setPackage(getPackageName());
                        proxBroadcast.putExtra(EXTRA_NEARBY_STATION_ID, nearestStationId);
                        proxBroadcast.putExtra(EXTRA_NEARBY_STATION_NAME, nearestStationName);
                        proxBroadcast.putExtra(EXTRA_NEARBY_DISTANCE_M, distanceMeters);
                        sendBroadcast(proxBroadcast);
                    }
                });
    }

    public void stopRideTracking() {
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void updateNotification(double fare, double distanceKm) {
        String fareText = String.format("₹%.2f", fare);
        String distText = String.format("%.2f km", distanceKm);
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIF_ID, buildNotification(distText + " travelled", fareText));
        }
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
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Ride Tracking", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows live fare and route distance during an active ride");
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        super.onDestroy();
    }
}
