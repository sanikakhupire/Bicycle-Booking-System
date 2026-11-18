package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Firestore operations for the "bicycles" collection.
 */
public class BicycleRepository {

    private static final String BICYCLES_COLLECTION = "bicycles";

    private final FirebaseFirestore db;

    public BicycleRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetch ALL bicycles across all stations (admin use).
     */
    public LiveData<Resource<List<Bicycle>>> getAllBicycles() {
        MutableLiveData<Resource<List<Bicycle>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BICYCLES_COLLECTION)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    result.setValue(Resource.error(error.getMessage(), null));
                    return;
                }
                List<Bicycle> bicycles = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Bicycle bicycle = doc.toObject(Bicycle.class);
                        bicycles.add(bicycle);
                    }
                }
                result.setValue(Resource.success(bicycles));
            });

        return result;
    }

    /**
     * Fetch all available bicycles at a given station (real-time).
     */
    public LiveData<Resource<List<Bicycle>>> getBicyclesByStation(String stationId) {
        MutableLiveData<Resource<List<Bicycle>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BICYCLES_COLLECTION)
            .whereEqualTo("stationId", stationId)
            .whereEqualTo("available", true)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    result.setValue(Resource.error(error.getMessage(), null));
                    return;
                }
                List<Bicycle> bicycles = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Bicycle bicycle = doc.toObject(Bicycle.class);
                        bicycles.add(bicycle);
                    }
                }
                result.setValue(Resource.success(bicycles));
            });

        return result;
    }

    /**
     * Fetch a single bicycle by its ID.
     */
    public LiveData<Resource<Bicycle>> getBicycle(String bicycleId) {
        MutableLiveData<Resource<Bicycle>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BICYCLES_COLLECTION).document(bicycleId)
            .get()
            .addOnSuccessListener(snapshot -> {
                Bicycle bicycle = snapshot.toObject(Bicycle.class);
                if (bicycle != null) {
                    result.setValue(Resource.success(bicycle));
                } else {
                    result.setValue(Resource.error("Bicycle not found", null));
                }
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Fetch bicycle by QR code string.
     */
    public LiveData<Resource<Bicycle>> getBicycleByQrCode(String qrCode) {
        MutableLiveData<Resource<Bicycle>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BICYCLES_COLLECTION)
            .whereEqualTo("qrCode", qrCode)
            .limit(1)
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!snapshots.isEmpty()) {
                    Bicycle bicycle = snapshots.getDocuments().get(0).toObject(Bicycle.class);
                    result.setValue(Resource.success(bicycle));
                } else {
                    result.setValue(Resource.error("Invalid QR code", null));
                }
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Add a new bicycle (admin only).
     * After creation, sets qrCode = document ID and increments station availableBicycles count.
     */
    public LiveData<Resource<String>> addBicycle(Bicycle bicycle) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BICYCLES_COLLECTION)
            .add(bicycle)
            .addOnSuccessListener(ref -> {
                String docId = ref.getId();
                // Set qrCode = document ID
                ref.update("qrCode", docId);

                // Increment station's availableBicycles count
                if (bicycle.getStationId() != null) {
                    db.collection("stations").document(bicycle.getStationId())
                        .update("availableBicycles",
                                com.google.firebase.firestore.FieldValue.increment(1));
                }

                result.setValue(Resource.success(docId));
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Update bicycle availability. Also syncs station availableBicycles count.
     */
    public LiveData<Resource<Void>> updateAvailability(String bicycleId, boolean isAvailable) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // Read current state first to know if count needs changing
        db.collection(BICYCLES_COLLECTION).document(bicycleId)
            .get()
            .addOnSuccessListener(snapshot -> {
                Bicycle bicycle = snapshot.toObject(Bicycle.class);
                boolean wasAvailable = bicycle != null && bicycle.isAvailable();

                snapshot.getReference().update("available", isAvailable)
                    .addOnSuccessListener(unused -> {
                        // Sync station count only if state actually changed
                        if (bicycle != null && bicycle.getStationId() != null
                                && wasAvailable != isAvailable) {
                            long delta = isAvailable ? 1 : -1;
                            db.collection("stations").document(bicycle.getStationId())
                                .update("availableBicycles",
                                        com.google.firebase.firestore.FieldValue.increment(delta));
                        }
                        result.setValue(Resource.success(null));
                    })
                    .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Delete a bicycle (admin only). Also decrements station availableBicycles if it was available.
     */
    public LiveData<Resource<Void>> deleteBicycle(String bicycleId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // Read bicycle first to get stationId and availability
        db.collection(BICYCLES_COLLECTION).document(bicycleId)
            .get()
            .addOnSuccessListener(snapshot -> {
                Bicycle bicycle = snapshot.toObject(Bicycle.class);
                snapshot.getReference().delete()
                    .addOnSuccessListener(unused -> {
                        // If it was available, decrement station count
                        if (bicycle != null && bicycle.isAvailable()
                                && bicycle.getStationId() != null) {
                            db.collection("stations").document(bicycle.getStationId())
                                .update("availableBicycles",
                                        com.google.firebase.firestore.FieldValue.increment(-1));
                        }
                        result.setValue(Resource.success(null));
                    })
                    .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
