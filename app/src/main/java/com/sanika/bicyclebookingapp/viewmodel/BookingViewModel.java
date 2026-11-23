package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.repository.BookingRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;
import java.util.Map;

/**
 * ViewModel for booking lifecycle screens.
 */
public class BookingViewModel extends ViewModel {

    private final BookingRepository bookingRepository;

    public BookingViewModel() {
        this.bookingRepository = new BookingRepository();
    }

    public LiveData<Resource<Booking>> createBooking(String userId, String bicycleId,
                                                      String stationId, String stationName) {
        return bookingRepository.createBooking(userId, bicycleId, stationId, stationName);
    }

    public LiveData<Resource<Booking>> createBooking(String userId, String bicycleId,
                                                      String stationId, String stationName,
                                                      String bicycleType, String couponCode,
                                                      double depositPaid, Map<String, Object> pricingSnapshot) {
        return bookingRepository.createBooking(userId, bicycleId, stationId, stationName,
                bicycleType, couponCode, depositPaid, pricingSnapshot);
    }

    public LiveData<Resource<Invoice>> getInvoice(String invoiceId) {
        return bookingRepository.getInvoice(invoiceId);
    }

    public LiveData<Resource<Invoice>> getBookingInvoice(String bookingId) {
        return bookingRepository.getBookingInvoice(bookingId);
    }

    public LiveData<Resource<Void>> startRide(String bookingId) {
        return bookingRepository.startRide(bookingId);
    }

    public LiveData<Resource<Booking>> endRide(String bookingId, String bicycleId, String stationId) {
        return bookingRepository.endRide(bookingId, bicycleId, stationId);
    }

    public LiveData<Resource<List<Booking>>> getUserBookings(String userId) {
        return bookingRepository.getUserBookings(userId);
    }

    public LiveData<Resource<Booking>> getActiveBooking(String userId) {
        return bookingRepository.getActiveBooking(userId);
    }

    public LiveData<Resource<List<Booking>>> getAllActiveRides() {
        return bookingRepository.getAllActiveRides();
    }

    public LiveData<Resource<Void>> cancelBooking(String bookingId, String bicycleId, String stationId) {
        return bookingRepository.cancelBooking(bookingId, bicycleId, stationId);
    }

    public LiveData<Resource<Void>> expireBooking(String bookingId, String bicycleId, String stationId) {
        return bookingRepository.expireBooking(bookingId, bicycleId, stationId);
    }
}
