package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles booking lifecycle: create, start ride, end ride, fare calculation.
 *
 * Firestore relationships:
 *  - bookings/{bookingId} references users/{userId} and bicycles/{bicycleId}
 *  - payments/{paymentId} references bookings/{bookingId}
 *  - When a booking is created, bicycle.isAvailable is set to false (transaction)
 *  - When a ride ends, bicycle.isAvailable is set back to true
 */
public class BookingRepository {

    private static final String BOOKINGS_COLLECTION  = "bookings";
    private static final String BICYCLES_COLLECTION  = "bicycles";
    private static final String STATIONS_COLLECTION  = "stations";
    private static final String PAYMENTS_COLLECTION  = "payments";

    private final FirebaseFirestore db;
    private final FirebaseFunctions functions;

    public BookingRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.functions = FirebaseFunctions.getInstance();
    }

    /**
     * Create a booking and mark bicycle as unavailable — all in a Firestore transaction.
     */
    public LiveData<Resource<Booking>> createBooking(String userId, String bicycleId,
                                                      String stationId, String stationName) {
        MutableLiveData<Resource<Booking>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.runTransaction(transaction -> {
            // Read bicycle document
            var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
            var bicycleSnap = transaction.get(bicycleRef);

            Boolean available = bicycleSnap.getBoolean("available");
            if (available == null || !available) {
                // Use RuntimeException so it doesn't require a 'throws' declaration
                throw new RuntimeException("Bicycle is no longer available");
            }

            // Mark bicycle as unavailable
            transaction.update(bicycleRef, "available", false);

            // Decrement station available count
            var stationRef = db.collection(STATIONS_COLLECTION).document(stationId);
            transaction.update(stationRef, "availableBicycles", FieldValue.increment(-1));

            return null;
        }).addOnSuccessListener(unused -> {
            // Now create the booking document
            Booking booking = new Booking(userId, bicycleId, stationId, stationName);
            db.collection(BOOKINGS_COLLECTION)
                .add(booking)
                .addOnSuccessListener(ref -> {
                    booking.setBookingId(ref.getId());
                    result.setValue(Resource.success(booking));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
        }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Start the ride — sets startTime and status to ACTIVE.
     */
    public LiveData<Resource<Void>> startRide(String bookingId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("startTime", new Date());
        updates.put("status", Booking.STATUS_ACTIVE);

        db.collection(BOOKINGS_COLLECTION).document(bookingId)
            .update(updates)
            .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * End the ride — calculates fare, updates booking, and moves bicycle to return station.
     *
     * After this call, the bicycle document will have:
     *   available: true
     *   stationId: <returnStationId>
     *   stationName: <returnStationName>
     *   lastLocation: <station GeoPoint>
     *   suspectedTheft: false
     */
    public LiveData<Resource<Booking>> endRide(String bookingId, String bicycleId,
                                                String returnStationId) {
        MutableLiveData<Resource<Booking>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // First fetch the return station details (name + location)
        db.collection(STATIONS_COLLECTION).document(returnStationId)
            .get()
            .addOnSuccessListener(stationSnap -> {
                String returnStationName = stationSnap.getString("name");
                com.google.firebase.firestore.GeoPoint stationGeoPoint =
                        stationSnap.getGeoPoint("location");

                // Try Cloud Function first, fall back to local calculation
                Map<String, Object> data = new HashMap<>();
                data.put("bookingId", bookingId);

                functions.getHttpsCallable("calculateFare")
                    .call(data)
                    .addOnSuccessListener(httpsResult -> {
                        Map<String, Object> response =
                                (Map<String, Object>) httpsResult.getData();
                        double fare = ((Number) response.get("fare")).doubleValue();
                        long durationMinutes =
                                ((Number) response.get("durationMinutes")).longValue();
                        finalizeRide(result, bookingId, bicycleId,
                                returnStationId, returnStationName,
                                stationGeoPoint, fare, durationMinutes);
                    })
                    .addOnFailureListener(e ->
                        // Cloud Function unavailable — calculate locally
                        calculateFareLocallyAndFinalize(result, bookingId, bicycleId,
                                returnStationId, returnStationName, stationGeoPoint)
                    );
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(
                    "Could not find return station: " + e.getMessage(), null)));

        return result;
    }

    /**
     * Writes all end-ride data to Firestore in a single transaction:
     * - Booking: status=COMPLETED, endTime, fare, durationMinutes, returnStation
     * - Bicycle: available=true, stationId, stationName, lastLocation = station location
     * - Station: availableBicycles incremented
     */
    private void finalizeRide(MutableLiveData<Resource<Booking>> result,
                               String bookingId, String bicycleId,
                               String returnStationId, String returnStationName,
                               com.google.firebase.firestore.GeoPoint stationGeoPoint,
                               double fare, long durationMinutes) {

        db.runTransaction(transaction -> {
            // 1. Update booking
            var bookingRef = db.collection(BOOKINGS_COLLECTION).document(bookingId);
            transaction.update(bookingRef,
                "endTime",            new Date(),
                "status",             Booking.STATUS_COMPLETED,
                "fare",               fare,
                "durationMinutes",    durationMinutes,
                "returnStationId",    returnStationId,
                "returnStationName",  returnStationName
            );

            // 2. Update bicycle — move it to the return station
            var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
            Map<String, Object> bicycleUpdates = new HashMap<>();
            bicycleUpdates.put("available",      true);
            bicycleUpdates.put("stationId",      returnStationId);
            bicycleUpdates.put("stationName",    returnStationName);
            bicycleUpdates.put("suspectedTheft", false);
            // Update lastLocation to the station's coordinates
            if (stationGeoPoint != null) {
                bicycleUpdates.put("lastLocation", stationGeoPoint);
            }
            transaction.update(bicycleRef, bicycleUpdates);

            // 3. Increment return station's available count
            var stationRef = db.collection(STATIONS_COLLECTION).document(returnStationId);
            transaction.update(stationRef, "availableBicycles", FieldValue.increment(1));

            return null;

        }).addOnSuccessListener(unused -> {
            // Create payment record
            Payment payment = new Payment(bookingId, null, fare);
            payment.setStatus(Payment.STATUS_SUCCESS);
            db.collection(PAYMENTS_COLLECTION).add(payment);

            // Build and return booking directly with known values
            // (don't re-fetch — Firestore may not have synced yet)
            Booking completedBooking = new Booking();
            completedBooking.setBookingId(bookingId);
            completedBooking.setFare(fare);
            completedBooking.setDurationMinutes(durationMinutes);
            completedBooking.setStatus(Booking.STATUS_COMPLETED);
            result.setValue(Resource.success(completedBooking));

        }).addOnFailureListener(e ->
            result.setValue(Resource.error(e.getMessage(), null)));
    }

    /**
     * Local fare calculation fallback when Cloud Function is unavailable.
     */
    private void calculateFareLocallyAndFinalize(
            MutableLiveData<Resource<Booking>> result,
            String bookingId, String bicycleId,
            String returnStationId, String returnStationName,
            com.google.firebase.firestore.GeoPoint stationGeoPoint) {

        db.collection(BOOKINGS_COLLECTION).document(bookingId)
            .get()
            .addOnSuccessListener(snap -> {
                Booking booking = snap.toObject(Booking.class);
                if (booking == null) {
                    result.setValue(Resource.error("Booking not found", null));
                    return;
                }

                long startMs = booking.getStartTime() != null
                        ? booking.getStartTime().getTime()
                        : System.currentTimeMillis();
                long elapsedMs = new Date().getTime() - startMs;
                long durationMinutes = Math.max(1, elapsedMs / 60000);

                // Get distance from booking if tracked, else estimate from time
                double distanceKm = booking.getDistanceKm() > 0
                        ? booking.getDistanceKm() : 0;

                double fare = com.sanika.bicyclebookingapp.util.FareCalculator
                        .calculate(distanceKm, elapsedMs);

                finalizeRide(result, bookingId, bicycleId,
                        returnStationId, returnStationName,
                        stationGeoPoint, fare, durationMinutes);
            })
            .addOnFailureListener(e ->
                result.setValue(Resource.error(e.getMessage(), null)));
    }

    /**
     * Get all bookings for a user (ride history), ordered by creation time.
     */
    public LiveData<Resource<List<Booking>>> getUserBookings(String userId) {
        MutableLiveData<Resource<List<Booking>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BOOKINGS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    result.setValue(Resource.error(error.getMessage(), null));
                    return;
                }
                List<Booking> bookings = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Booking booking = doc.toObject(Booking.class);
                        bookings.add(booking);
                    }
                }
                result.setValue(Resource.success(bookings));
            });

        return result;
    }

    /**
     * Get the user's current active booking (if any).
     * Auto-cancels PENDING bookings older than 10 minutes.
     */
    public LiveData<Resource<Booking>> getActiveBooking(String userId) {
        MutableLiveData<Resource<Booking>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BOOKINGS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereIn("status", List.of(Booking.STATUS_PENDING, Booking.STATUS_ACTIVE))
            .limit(1)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    result.setValue(Resource.error(error.getMessage(), null));
                    return;
                }
                if (snapshots != null && !snapshots.isEmpty()) {
                    Booking booking = snapshots.getDocuments().get(0).toObject(Booking.class);
                    if (booking != null) {
                        // Auto-cancel PENDING bookings older than 10 minutes
                        if (Booking.STATUS_PENDING.equals(booking.getStatus())
                                && booking.getCreatedAt() != null) {
                            long ageMs = new Date().getTime() - booking.getCreatedAt().getTime();
                            if (ageMs > 10 * 60 * 1000) { // 10 minutes
                                cancelBooking(booking.getBookingId(),
                                        booking.getBicycleId(), booking.getStationId());
                                result.setValue(Resource.success(null));
                                return;
                            }
                        }
                        result.setValue(Resource.success(booking));
                    }
                } else {
                    result.setValue(Resource.success(null));
                }
            });

        return result;
    }

    /**
     * Get all active rides (admin view).
     */
    public LiveData<Resource<List<Booking>>> getAllActiveRides() {
        MutableLiveData<Resource<List<Booking>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(BOOKINGS_COLLECTION)
            .whereEqualTo("status", Booking.STATUS_ACTIVE)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    result.setValue(Resource.error(error.getMessage(), null));
                    return;
                }
                List<Booking> bookings = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        bookings.add(doc.toObject(Booking.class));
                    }
                }
                result.setValue(Resource.success(bookings));
            });

        return result;
    }

    /**
     * Cancel a pending booking.
     */
    public LiveData<Resource<Void>> cancelBooking(String bookingId, String bicycleId, String stationId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.runTransaction(transaction -> {
            var bookingRef = db.collection(BOOKINGS_COLLECTION).document(bookingId);
            transaction.update(bookingRef, "status", Booking.STATUS_CANCELLED);

            var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
            transaction.update(bicycleRef, "available", true);

            var stationRef = db.collection(STATIONS_COLLECTION).document(stationId);
            transaction.update(stationRef, "availableBicycles", FieldValue.increment(1));

            return null;
        }).addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
          .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
