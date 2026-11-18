package com.sanika.bicyclebookingapp.util;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;

/**
 * Theft detection system.
 *
 * Checks for bicycles that:
 * 1. Are marked as "available" (not in use)
 * 2. But have moved significantly from their station location
 * 3. Within the last 5 minutes
 *
 * This indicates potential theft — bicycle is moving without an active booking.
 *
 * In production, this would run as a Firebase Cloud Function every 5 minutes.
 * For this demo, admins can manually trigger it from the admin panel.
 */
public class TheftDetector {

    private static final double THEFT_THRESHOLD_KM = 0.5; // 500 meters

    public interface TheftCallback {
        void onTheftDetected(String bicycleId, String stationName, double distanceKm);
        void onCheckComplete(int bicyclesChecked, int theftsDetected);
    }

    /**
     * Scans all bicycles for potential theft.
     * Compares bicycle's lastLocation with its station's location.
     */
    public static void checkForTheft(TheftCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("bicycles")
            .whereEqualTo("available", true) // Only check "available" bicycles
            .get()
            .addOnSuccessListener(bicycleSnapshots -> {
                int[] checked = {0};
                int[] thefts = {0};
                int total = bicycleSnapshots.size();

                if (total == 0) {
                    callback.onCheckComplete(0, 0);
                    return;
                }

                for (QueryDocumentSnapshot bicycleDoc : bicycleSnapshots) {
                    String bicycleId = bicycleDoc.getId();
                    String stationId = bicycleDoc.getString("stationId");
                    GeoPoint lastLocation = bicycleDoc.getGeoPoint("lastLocation");
                    Date lastLocationTime = bicycleDoc.getDate("lastLocationTime");

                    // Skip if no location data yet
                    if (lastLocation == null || stationId == null) {
                        checked[0]++;
                        if (checked[0] == total) callback.onCheckComplete(checked[0], thefts[0]);
                        continue;
                    }

                    // Skip if location is stale (older than 5 minutes)
                    if (lastLocationTime != null) {
                        long ageMs = new Date().getTime() - lastLocationTime.getTime();
                        if (ageMs > 5 * 60 * 1000) {
                            checked[0]++;
                            if (checked[0] == total) callback.onCheckComplete(checked[0], thefts[0]);
                            continue;
                        }
                    }

                    // Get station location
                    db.collection("stations").document(stationId)
                        .get()
                        .addOnSuccessListener(stationDoc -> {
                            GeoPoint stationLocation = stationDoc.getGeoPoint("location");
                            String stationName = stationDoc.getString("name");

                            if (stationLocation != null) {
                                double distance = calculateDistance(
                                        lastLocation.getLatitude(), lastLocation.getLongitude(),
                                        stationLocation.getLatitude(), stationLocation.getLongitude()
                                );

                                // THEFT DETECTED — bicycle moved > 500m from station
                                if (distance > THEFT_THRESHOLD_KM) {
                                    thefts[0]++;
                                    callback.onTheftDetected(bicycleId, stationName, distance);
                                }
                            }

                            checked[0]++;
                            if (checked[0] == total) callback.onCheckComplete(checked[0], thefts[0]);
                        });
                }
            });
    }

    /** Haversine formula — calculates distance between two GPS coordinates in km. */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
