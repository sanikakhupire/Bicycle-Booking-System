package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.Referral;
import com.sanika.bicyclebookingapp.repository.ReferralRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel for customer referral invitations, tracking, and bonus awards.
 */
public class ReferralViewModel extends ViewModel {

    private final ReferralRepository repository;

    public ReferralViewModel() {
        this.repository = new ReferralRepository();
    }

    public ReferralViewModel(ReferralRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<String>> registerReferral(String referralCode, String refereeUserId, String refereeName, String refereePhone) {
        return repository.registerReferral(referralCode, refereeUserId, refereeName, refereePhone);
    }

    public LiveData<Resource<List<Referral>>> getCustomerReferrals(String referrerUserId) {
        return repository.getCustomerReferrals(referrerUserId);
    }

    public void onRefereeRideCompleted(String refereeUserId, String bookingId) {
        repository.onRefereeRideCompleted(refereeUserId, bookingId);
    }
}
