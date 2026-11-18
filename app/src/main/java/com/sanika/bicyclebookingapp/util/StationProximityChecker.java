package com.sanika.bicyclebookingapp.util;

import android.location.Location;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Checks whether a given GPS location is within range of any station.
 * Used to auto-end rides when user returns bicycle to a station.
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

    /**
     * Queries all stations and finds if userLocation is within radiusMeters of any.
     * Calls callback with the nearest station found, or null if none within range.
     */
    public static void findNearestStation(FirebaseFirestore db,
                                           Location userLocation,
                                           float radiusMeters,
                                           ProximityCallback callback) {
        db.collection("stations")
            .get()
            .addOnSuccessListener(snapshots -> {
                String nearestId   = null;
                String nearestName = null;
                float  nearestDist = Float.MAX_VALUE;

                for (QueryDocumentSnapshot doc : snapshots) {
                    GeoPoint geoPoint = doc.getGeoPoint("location");
                    if (geoPoint == null) continue;

                    // Build a Location object for distance calculation
                    Location stationLocation = new Location("station");
                    stationLocation.setLatitude(geoPoint.getLatitude());
                    stationLocation.setLongitude(geoPoint.getLongitude());

                    float distance = userLocation.distanceTo(stationLocation);

                    if (distance <= radiusMeters && distance < nearestDist) {
                        nearestDist = distance;
                        nearestId   = doc.getId();
                        nearestName = doc.getString("name");
                    }
                }

                callback.onResult(nearestId, nearestName, nearestDist);
            })
            .addOnFailureListener(e -> callback.onResult(null, null, Float.MAX_VALUE));
    }
}
