package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.LoyaltyAccount;
import com.sanika.bicyclebookingapp.model.LoyaltyTransaction;
import com.sanika.bicyclebookingapp.repository.LoyaltyRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel for customer loyalty wallet, rewards, milestone badges, and transaction history.
 */
public class LoyaltyViewModel extends ViewModel {

    private final LoyaltyRepository repository;

    public LoyaltyViewModel() {
        this.repository = new LoyaltyRepository();
    }

    public LoyaltyViewModel(LoyaltyRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<LoyaltyAccount>> getLoyaltyAccount(String userId, String userName) {
        return repository.getLoyaltyAccount(userId, userName);
    }

    public LiveData<Resource<List<LoyaltyTransaction>>> getLoyaltyHistory(String userId) {
        return repository.getLoyaltyHistory(userId);
    }

    public LiveData<Resource<Long>> creditRidePoints(String userId, String bookingId, double fareInr) {
        return repository.creditRidePoints(userId, bookingId, fareInr);
    }

    public LiveData<Resource<Long>> redeemPoints(String userId, long pointsToRedeem, String bookingId) {
        return repository.redeemPoints(userId, pointsToRedeem, bookingId);
    }
}
