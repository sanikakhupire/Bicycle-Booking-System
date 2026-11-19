package com.sanika.bicyclebookingapp.util;

import android.location.Location;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether a given GPS location is within range of any station.
 * Uses an in-memory cache to prevent excessive Firestore reads (queried every 10s by RideTrackingService).
 */
public class StationProximityChecker {

    public interface ProximityCallback {
        /**
         * Called with the nearest station if within range, or null if none found.
         *
         * @param stationId    Firestore document ID of nearest station (null if none)
         * @param stationName  Name of nearest station (null if none)
         * @param distanceM    Distance in metres to nearest station
         */
        void onResult(String stationId, String stationName, float distanceM);
    }

    private static class CachedStation {
        final String id;
        final String name;
        final double latitude;
        final double longitude;

        CachedStation(String id, String name, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final List<CachedStation> cachedStations = new ArrayList<>();
    private static long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Queries stations (using cache if fresh) and finds if userLocation is within radiusMeters of any.
     * Calls callback with the nearest station found, or null if none within range.
     */
    public static void findNearestStation(FirebaseFirestore db,
                                           Location userLocation,
                                           float radiusMeters,
                                           ProximityCallback callback) {
        long now = System.currentTimeMillis();
        if (!cachedStations.isEmpty() && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            findInCache(userLocation, radiusMeters, callback);
        } else {
            db.collection("stations")
                .get()
                .addOnSuccessListener(snapshots -> {
                    cachedStations.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        GeoPoint geoPoint = doc.getGeoPoint("location");
                        if (geoPoint == null) continue;
                        cachedStations.add(new CachedStation(
                                doc.getId(),
                                doc.getString("name"),
                                geoPoint.getLatitude(),
                                geoPoint.getLongitude()
                        ));
                    }
                    cacheTimestamp = System.currentTimeMillis();
                    findInCache(userLocation, radiusMeters, callback);
                })
                .addOnFailureListener(e -> callback.onResult(null, null, Float.MAX_VALUE));
        }
    }

    private static void findInCache(Location userLocation, float radiusMeters,
                                    ProximityCallback callback) {
        String nearestId = null;
        String nearestName = null;
        float nearestDist = Float.MAX_VALUE;

        for (CachedStation station : cachedStations) {
            Location stationLoc = new Location("station");
            stationLoc.setLatitude(station.latitude);
            stationLoc.setLongitude(station.longitude);

            float distance = userLocation.distanceTo(stationLoc);
            if (distance <= radiusMeters && distance < nearestDist) {
                nearestDist = distance;
                nearestId = station.id;
                nearestName = station.name;
            }
        }

        callback.onResult(nearestId, nearestName, nearestDist);
    }

    /** Clear the station cache. Call when stations are added/updated. */
    public static void clearCache() {
        cachedStations.clear();
        cacheTimestamp = 0;
    }
}
