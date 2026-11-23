package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.PaymentConfig;
import com.sanika.bicyclebookingapp.model.Refund;
import com.sanika.bicyclebookingapp.repository.PaymentRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;
import java.util.Map;

/**
 * ViewModel for customer payment history, status tracking, and admin dashboard.
 */
public class PaymentViewModel extends ViewModel {

    private final PaymentRepository paymentRepository;

    public PaymentViewModel() {
        this.paymentRepository = PaymentRepository.getInstance();
    }

    public PaymentViewModel(PaymentRepository repository) {
        this.paymentRepository = repository;
    }

    public LiveData<Resource<List<Payment>>> getUserPayments(String userId) {
        return paymentRepository.getUserPayments(userId);
    }

    public LiveData<Resource<List<Payment>>> getAllPayments() {
        return paymentRepository.getAllPayments();
    }

    public LiveData<Resource<PaymentConfig>> getPaymentConfig() {
        return paymentRepository.getPaymentConfig();
    }

    public LiveData<Resource<Void>> savePaymentConfig(PaymentConfig config) {
        return paymentRepository.savePaymentConfig(config);
    }

    public LiveData<Resource<Map<String, Object>>> getAdminPaymentOverview() {
        return paymentRepository.getAdminPaymentOverview();
    }

    public void requestRefund(String paymentId, String bookingId, String reason, long amountPaise, PaymentRepository.RefundCallback callback) {
        paymentRepository.requestRefund(paymentId, bookingId, reason, amountPaise, callback);
    }
}
