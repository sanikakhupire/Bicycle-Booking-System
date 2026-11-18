package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.repository.BicycleRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel for bicycle-related screens.
 */
public class BicycleViewModel extends ViewModel {

    private final BicycleRepository bicycleRepository;

    public BicycleViewModel() {
        this.bicycleRepository = new BicycleRepository();
    }

    public LiveData<Resource<List<Bicycle>>> getAllBicycles() {
        return bicycleRepository.getAllBicycles();
    }

    public LiveData<Resource<List<Bicycle>>> getBicyclesByStation(String stationId) {
        return bicycleRepository.getBicyclesByStation(stationId);
    }

    public LiveData<Resource<Bicycle>> getBicycle(String bicycleId) {
        return bicycleRepository.getBicycle(bicycleId);
    }

    public LiveData<Resource<Bicycle>> getBicycleByQrCode(String qrCode) {
        return bicycleRepository.getBicycleByQrCode(qrCode);
    }

    public LiveData<Resource<String>> addBicycle(Bicycle bicycle) {
        return bicycleRepository.addBicycle(bicycle);
    }

    public LiveData<Resource<Void>> updateAvailability(String bicycleId, boolean isAvailable) {
        return bicycleRepository.updateAvailability(bicycleId, isAvailable);
    }

    public LiveData<Resource<Void>> deleteBicycle(String bicycleId) {
        return bicycleRepository.deleteBicycle(bicycleId);
    }
}
