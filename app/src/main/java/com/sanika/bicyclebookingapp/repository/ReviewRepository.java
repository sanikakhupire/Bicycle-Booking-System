package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.RideReview;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository orchestrating ride reviews, bicycle rating aggregations,
 * duplicate review prevention, and administrative review moderation.
 */
public class ReviewRepository {

    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_BOOKINGS = "bookings";
    public static final String COLLECTION_BICYCLES = "bicycles";
    public static final String COLLECTION_STATIONS = "stations";

    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public ReviewRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationRepository = new NotificationRepository(db);
    }

    public ReviewRepository(FirebaseFirestore db, NotificationRepository notificationRepository) {
        this.db = db;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Submit a rating and review for a completed ride.
     * Enforces:
     * 1. Ride must be in COMPLETED status.
     * 2. Only one review per bookingId (no duplicate reviews).
     * 3. Aggregates bicycle rating and total reviews.
     * 4. Flags poor condition / damaged reports to maintenance fleet.
     */
    public LiveData<Resource<String>> submitReview(RideReview review) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (review.getBookingId() == null || review.getBookingId().trim().isEmpty()) {
            result.setValue(Resource.error("Booking ID is required to submit a review.", null));
            return result;
        }

        // Check if booking is completed
        db.collection(COLLECTION_BOOKINGS).document(review.getBookingId()).get().addOnSuccessListener(bookingSnap -> {
            if (!bookingSnap.exists()) {
                result.setValue(Resource.error("Associated booking not found.", null));
                return;
            }

            String bookingStatus = bookingSnap.getString("status");
            if (!Booking.STATUS_COMPLETED.equalsIgnoreCase(bookingStatus)) {
                result.setValue(Resource.error("Reviews are only permitted for completed rides.", null));
                return;
            }

            // Check if review already exists for this bookingId
            db.collection(COLLECTION_REVIEWS)
                    .whereEqualTo("bookingId", review.getBookingId())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(reviewSnaps -> {
                        if (!reviewSnaps.isEmpty()) {
                            result.setValue(Resource.error("A review has already been submitted for this ride.", null));
                            return;
                        }

                        // Create review
                        DocumentReference docRef = db.collection(COLLECTION_REVIEWS).document();
                        review.setReviewId(docRef.getId());
                        review.setCreatedAt(new Date());
                        review.setUpdatedAt(new Date());

                        docRef.set(review).addOnSuccessListener(unused -> {
                            // Update Bicycle average rating
                            updateBicycleRating(review.getBicycleId(), review.getRatingBicycle());

                            // Update Station rating if pickupStationId is provided
                            if (review.getPickupStationId() != null && !review.getPickupStationId().isEmpty()) {
                                updateStationRating(review.getPickupStationId(), review.getRatingStation());
                            }

                            // Trigger maintenance alert if bicycle condition reported as DAMAGED or NEEDS_SERVICE
                            if (RideReview.CONDITION_DAMAGED.equalsIgnoreCase(review.getBicycleCondition())
                                    || RideReview.CONDITION_NEEDS_SERVICE.equalsIgnoreCase(review.getBicycleCondition())
                                    || review.getRatingBicycle() <= 2.0f) {
                                dispatchConditionAlert(review);
                            }

                            result.setValue(Resource.success(review.getReviewId()));
                        }).addOnFailureListener(e -> result.setValue(Resource.error("Failed to save review: " + e.getMessage(), null)));
                    })
                    .addOnFailureListener(e -> result.setValue(Resource.error("Failed to verify duplicate review: " + e.getMessage(), null)));
        }).addOnFailureListener(e -> result.setValue(Resource.error("Failed to verify booking: " + e.getMessage(), null)));

        return result;
    }

    private void updateBicycleRating(String bicycleId, float newRating) {
        if (bicycleId == null || bicycleId.isEmpty()) return;

        DocumentReference bikeRef = db.collection(COLLECTION_BICYCLES).document(bicycleId);
        bikeRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) return;

            Double currentAvg = snap.getDouble("averageRating");
            Long currentCount = snap.getLong("totalRatings");

            double avg = currentAvg != null ? currentAvg : 5.0;
            long count = currentCount != null ? currentCount : 0;

            double updatedAvg = count > 0 ? ((avg * count) + newRating) / (count + 1) : newRating;
            long updatedCount = count + 1;

            bikeRef.update("averageRating", Math.round(updatedAvg * 10.0) / 10.0, "totalRatings", updatedCount);
        });
    }

    private void updateStationRating(String stationId, float newRating) {
        if (stationId == null || stationId.isEmpty()) return;

        DocumentReference stationRef = db.collection(COLLECTION_STATIONS).document(stationId);
        stationRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) return;

            Double currentAvg = snap.getDouble("averageRating");
            Long currentCount = snap.getLong("totalRatings");

            double avg = currentAvg != null ? currentAvg : 5.0;
            long count = currentCount != null ? currentCount : 0;

            double updatedAvg = count > 0 ? ((avg * count) + newRating) / (count + 1) : newRating;
            long updatedCount = count + 1;

            stationRef.update("averageRating", Math.round(updatedAvg * 10.0) / 10.0, "totalRatings", updatedCount);
        });
    }

    private void dispatchConditionAlert(RideReview review) {
        notificationRepository.dispatchNotification(null, new AppNotification(
                "ADMIN",
                AppNotification.TYPE_ADMIN_DAMAGE_REPORT,
                AppNotification.CHANNEL_ADMIN,
                "⚠️ Bicycle Condition Alert (" + review.getBicycleId() + ")",
                "Customer reported condition: " + review.getBicycleCondition() + " (Rating: " + review.getRatingBicycle() + "/5). "
                        + (review.getComment() != null ? review.getComment() : ""),
                AppNotification.TARGET_MAINTENANCE,
                review.getBicycleId()
        ));
    }

    /**
     * Check if a review exists for a given bookingId.
     */
    public LiveData<Resource<RideReview>> getReviewForBooking(String bookingId) {
        MutableLiveData<Resource<RideReview>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("bookingId", bookingId)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        result.setValue(Resource.success(null));
                        return;
                    }
                    RideReview review = snaps.getDocuments().get(0).toObject(RideReview.class);
                    if (review != null) review.setReviewId(snaps.getDocuments().get(0).getId());
                    result.setValue(Resource.success(review));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Get published reviews for a bicycle.
     */
    public LiveData<Resource<List<RideReview>>> getBicycleReviews(String bicycleId) {
        MutableLiveData<Resource<List<RideReview>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("bicycleId", bicycleId)
                .whereEqualTo("status", RideReview.STATUS_PUBLISHED)
                .addSnapshotListener((snaps, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }
                    List<RideReview> list = new ArrayList<>();
                    if (snaps != null) {
                        for (QueryDocumentSnapshot doc : snaps) {
                            RideReview r = doc.toObject(RideReview.class);
                            if (r != null) {
                                r.setReviewId(doc.getId());
                                list.add(r);
                            }
                        }
                    }
                    list.sort((a, b) -> {
                        Date da = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                        Date db = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                        return db.compareTo(da);
                    });
                    result.setValue(Resource.success(list));
                });

        return result;
    }

    /**
     * Get all reviews for admin moderation.
     */
    public LiveData<Resource<List<RideReview>>> getAllReviews() {
        MutableLiveData<Resource<List<RideReview>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_REVIEWS)
                .addSnapshotListener((snaps, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }
                    List<RideReview> list = new ArrayList<>();
                    if (snaps != null) {
                        for (QueryDocumentSnapshot doc : snaps) {
                            RideReview r = doc.toObject(RideReview.class);
                            if (r != null) {
                                r.setReviewId(doc.getId());
                                list.add(r);
                            }
                        }
                    }
                    list.sort((a, b) -> {
                        Date da = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                        Date db = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                        return db.compareTo(da);
                    });
                    result.setValue(Resource.success(list));
                });

        return result;
    }

    /**
     * Admin review moderation (e.g. PUBLISHED, FLAGGED, HIDDEN).
     */
    public LiveData<Resource<Void>> moderateReview(String reviewId, String status, String reason, String adminId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("moderatedBy", adminId);
        updates.put("moderationReason", reason);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_REVIEWS).document(reviewId)
                .update(updates)
                .addOnSuccessListener(v -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
