package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.repository.StationRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel for station-related screens.
 */
public class StationViewModel extends ViewModel {

    private final StationRepository stationRepository;

    public StationViewModel() {
        this.stationRepository = new StationRepository();
    }

    public LiveData<Resource<List<Station>>> getAllStations() {
        return stationRepository.getAllStations();
    }

    public LiveData<Resource<Station>> getStation(String stationId) {
        return stationRepository.getStation(stationId);
    }

    public LiveData<Resource<String>> addStation(Station station) {
        return stationRepository.addStation(station);
    }

    public LiveData<Resource<Void>> updateStation(Station station) {
        return stationRepository.updateStation(station);
    }

    public LiveData<Resource<Void>> deleteStation(String stationId) {
        return stationRepository.deleteStation(stationId);
    }
}
