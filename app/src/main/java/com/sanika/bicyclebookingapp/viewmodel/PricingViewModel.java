package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.PricingConfig;
import com.sanika.bicyclebookingapp.repository.PricingRepository;
import com.sanika.bicyclebookingapp.util.Resource;

/**
 * ViewModel for pricing configuration and promotional coupon operations.
 */
public class PricingViewModel extends ViewModel {

    private final PricingRepository pricingRepository;

    public PricingViewModel() {
        this.pricingRepository = PricingRepository.getInstance();
    }

    public LiveData<Resource<PricingConfig>> getPricingConfig() {
        return pricingRepository.getPricingConfig();
    }

    public LiveData<Resource<PricingConfig>> savePricingConfig(PricingConfig config) {
        return pricingRepository.savePricingConfig(config);
    }

    public LiveData<Resource<Coupon>> validateCoupon(String code, long subtotalPaise) {
        return pricingRepository.validateCoupon(code, subtotalPaise);
    }
}
