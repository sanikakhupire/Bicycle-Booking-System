package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.AdminSettings;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.repository.AdminRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;
import java.util.Map;

/**
 * ViewModel for Admin Dashboard, Business Analytics, and Operations.
 */
public class AdminViewModel extends ViewModel {

    private final AdminRepository adminRepository;

    public AdminViewModel() {
        this.adminRepository = AdminRepository.getInstance();
    }

    public AdminViewModel(AdminRepository repository) {
        this.adminRepository = repository;
    }

    public LiveData<Resource<Map<String, Object>>> getDashboardMetrics() {
        return adminRepository.getDashboardMetrics();
    }

    public LiveData<Resource<List<User>>> getAllCustomers() {
        return adminRepository.getAllCustomers();
    }

    public void updateUserRole(String userId, String newRole, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        adminRepository.updateUserRole(userId, newRole, onSuccess, onError);
    }

    public LiveData<Resource<List<Booking>>> getAllBookings() {
        return adminRepository.getAllBookings();
    }

    public void forceCancelBooking(String bookingId, String bicycleId, String stationId, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        adminRepository.forceCancelBooking(bookingId, bicycleId, stationId, onSuccess, onError);
    }

    public LiveData<Resource<List<Coupon>>> getAllCoupons() {
        return adminRepository.getAllCoupons();
    }

    public void saveCoupon(Coupon coupon, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        adminRepository.saveCoupon(coupon, onSuccess, onError);
    }

    public LiveData<Resource<List<SupportTicket>>> getSupportTickets() {
        return adminRepository.getSupportTickets();
    }

    public void resolveSupportTicket(String ticketId, String notes, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        adminRepository.resolveSupportTicket(ticketId, notes, onSuccess, onError);
    }

    public LiveData<Resource<AdminSettings>> getAdminSettings() {
        return adminRepository.getAdminSettings();
    }

    public void saveAdminSettings(AdminSettings settings, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        adminRepository.saveAdminSettings(settings, onSuccess, onError);
    }
}
