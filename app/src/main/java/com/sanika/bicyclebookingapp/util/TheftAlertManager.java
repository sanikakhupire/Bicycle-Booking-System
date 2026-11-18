package com.sanika.bicyclebookingapp.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Real-time theft monitoring system.
 *
 * How it works:
 * 1. Listens to ALL bicycle location updates in real-time via Firestore snapshot listener
 * 2. For each bicycle marked "available" (not in use):
 *    - Compares its lastLocation to its station's location
 *    - If distance > 200 meters → THEFT ALERT
 * 3. Sends a local notification to the admin device
 * 4. Marks the bicycle as "suspectedTheft: true" in Firestore
 *
 * Call startMonitoring() when admin opens the app.
 * Call stopMonitoring() when admin closes the app.
 */
public class TheftAlertManager {

    private static final String CHANNEL_ID = "theft_alert_channel";
    private static final double THEFT_THRESHOLD_KM = 0.2; // 200 meters

    private final Context context;
    private final FirebaseFirestore db;
    private ListenerRegistration bicycleListener;

    // Cache station locations to avoid repeated Firestore reads
    private final Map<String, GeoPoint> stationLocationCache = new HashMap<>();

    public TheftAlertManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        createNotificationChannel();
    }

    /**
     * Start real-time monitoring of all bicycle locations.
     * Triggers alert if any available bicycle moves away from its station.
     */
    public void startMonitoring() {
        // First load all station locations into cache
        db.collection("stations").get().addOnSuccessListener(stationSnaps -> {
            for (QueryDocumentSnapshot doc : stationSnaps) {
                GeoPoint location = doc.getGeoPoint("location");
                if (location != null) {
                    stationLocationCache.put(doc.getId(), location);
                }
            }

            // Now listen to bicycle changes
            bicycleListener = db.collection("bicycles")
                .addSnapshotListener((bicycleSnaps, error) -> {
                    if (error != null || bicycleSnaps == null) return;

                    for (QueryDocumentSnapshot doc : bicycleSnaps) {
                        Boolean available = doc.getBoolean("available");
                        GeoPoint lastLocation = doc.getGeoPoint("lastLocation");
                        String stationId = doc.getString("stationId");
                        String type = doc.getString("type");

                        // Only check available bicycles with GPS data
                        if (!Boolean.TRUE.equals(available)
                                || lastLocation == null
                                || stationId == null) continue;

                        GeoPoint stationLocation = stationLocationCache.get(stationId);
                        if (stationLocation == null) continue;

                        double distance = calculateDistance(
                            lastLocation.getLatitude(), lastLocation.getLongitude(),
                            stationLocation.getLatitude(), stationLocation.getLongitude()
                        );

                        if (distance > THEFT_THRESHOLD_KM) {
                            String bicycleId = doc.getId();
                            String stationName = doc.getString("stationName");

                            // Mark as suspected theft in Firestore
                            db.collection("bicycles").document(bicycleId)
                                .update("suspectedTheft", true,
                                        "theftDistance", distance);

                            // Send local notification to admin
                            sendTheftNotification(bicycleId, type, stationName, distance);
                        }
                    }
                });
        });
    }

    /** Stop monitoring — call when admin logs out or app goes to background. */
    public void stopMonitoring() {
        if (bicycleListener != null) {
            bicycleListener.remove();
            bicycleListener = null;
        }
    }

    private void sendTheftNotification(String bicycleId, String type,
                                        String stationName, double distanceKm) {
        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String title = "⚠️ Theft Alert!";
        String body  = (type != null ? type : "Bicycle") + " "
                + bicycleId.substring(0, 6).toUpperCase()
                + " moved " + String.format("%.0f", distanceKm * 1000) + "m from "
                + (stationName != null ? stationName : "station");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        manager.notify(bicycleId.hashCode(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Theft Alerts",
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts when a bicycle moves away from its station");
            NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
