package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.RideReview;
import com.sanika.bicyclebookingapp.repository.ReviewRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel for customer ride ratings, bicycle condition reviews, and admin moderation.
 */
public class ReviewViewModel extends ViewModel {

    private final ReviewRepository repository;

    public ReviewViewModel() {
        this.repository = new ReviewRepository();
    }

    public ReviewViewModel(ReviewRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<String>> submitReview(RideReview review) {
        return repository.submitReview(review);
    }

    public LiveData<Resource<RideReview>> getReviewForBooking(String bookingId) {
        return repository.getReviewForBooking(bookingId);
    }

    public LiveData<Resource<List<RideReview>>> getBicycleReviews(String bicycleId) {
        return repository.getBicycleReviews(bicycleId);
    }

    public LiveData<Resource<List<RideReview>>> getAllReviews() {
        return repository.getAllReviews();
    }

    public LiveData<Resource<Void>> moderateReview(String reviewId, String status, String reason, String adminId) {
        return repository.moderateReview(reviewId, status, reason, adminId);
    }
}
