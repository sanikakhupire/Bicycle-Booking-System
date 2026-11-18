package com.sanika.bicyclebookingapp.util;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * One-time data fix utility.
 * Fixes qrCode = null and recalculates availableBicycles counts from actual data.
 */
public class DataFixUtil {

    private DataFixUtil() {}

    /**
     * Fixes two things in one pass:
     * 1. Sets qrCode = documentId for any bicycle where qrCode is null
     * 2. Recalculates availableBicycles for every station by counting actual available bicycles
     *
     * Safe to call multiple times.
     */
    public static void fixAll(Runnable onComplete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Load all bicycles
        db.collection("bicycles")
            .get()
            .addOnSuccessListener(bicycleSnapshots -> {

                // Count available bicycles per station from real data
                Map<String, Integer> availableCountPerStation = new HashMap<>();

                for (QueryDocumentSnapshot doc : bicycleSnapshots) {
                    // Fix qrCode if null
                    String qrCode = doc.getString("qrCode");
                    if (qrCode == null || qrCode.isEmpty()) {
                        doc.getReference().update("qrCode", doc.getId());
                    }

                    // Count available bicycles per station
                    String stationId = doc.getString("stationId");
                    Boolean available = doc.getBoolean("available");
                    if (stationId != null && Boolean.TRUE.equals(available)) {
                        availableCountPerStation.put(
                            stationId,
                            availableCountPerStation.getOrDefault(stationId, 0) + 1
                        );
                    }
                }

                // Step 2: Update each station's availableBicycles with the real count
                db.collection("stations")
                    .get()
                    .addOnSuccessListener(stationSnapshots -> {
                        for (QueryDocumentSnapshot stationDoc : stationSnapshots) {
                            String stationId = stationDoc.getId();
                            int realCount = availableCountPerStation.getOrDefault(stationId, 0);
                            stationDoc.getReference().update("availableBicycles", realCount);
                        }
                        if (onComplete != null) onComplete.run();
                    });
            });
    }
}
