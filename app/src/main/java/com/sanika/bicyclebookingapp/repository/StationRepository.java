package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Firestore operations for the "stations" collection.
 */
public class StationRepository {

    private static final String STATIONS_COLLECTION = "stations";

    private final FirebaseFirestore db;

    public StationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetch all stations from Firestore (real-time listener).
     */
    public LiveData<Resource<List<Station>>> getAllStations() {
        MutableLiveData<Resource<List<Station>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(STATIONS_COLLECTION)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    result.setValue(Resource.error(error.getMessage(), null));
                    return;
                }
                List<Station> stations = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Station station = doc.toObject(Station.class);
                        stations.add(station);
                    }
                }
                result.setValue(Resource.success(stations));
            });

        return result;
    }

    /**
     * Fetch a single station by ID.
     */
    public LiveData<Resource<Station>> getStation(String stationId) {
        MutableLiveData<Resource<Station>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(STATIONS_COLLECTION).document(stationId)
            .get()
            .addOnSuccessListener(snapshot -> {
                Station station = snapshot.toObject(Station.class);
                if (station != null) {
                    result.setValue(Resource.success(station));
                } else {
                    result.setValue(Resource.error("Station not found", null));
                }
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Add a new station (admin only).
     */
    public LiveData<Resource<String>> addStation(Station station) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(STATIONS_COLLECTION)
            .add(station)
            .addOnSuccessListener(ref -> result.setValue(Resource.success(ref.getId())))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Update an existing station (admin only).
     */
    public LiveData<Resource<Void>> updateStation(Station station) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(STATIONS_COLLECTION).document(station.getStationId())
            .set(station)
            .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Delete a station (admin only).
     */
    public LiveData<Resource<Void>> deleteStation(String stationId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(STATIONS_COLLECTION).document(stationId)
            .delete()
            .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
