package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.PricingConfig;
import com.sanika.bicyclebookingapp.service.BillingService;
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
     * Prevents duplicate/simultaneous active or pending bookings for the same user.
     * Enforces single-bicycle reservation with 10-minute hold expiry.
     */
    public LiveData<Resource<Booking>> createBooking(String userId, String bicycleId,
                                                      String stationId, String stationName) {
        return createBooking(userId, bicycleId, stationId, stationName, "Standard", null, 0.0, null);
    }

    public LiveData<Resource<Booking>> createBooking(String userId, String bicycleId,
                                                      String stationId, String stationName,
                                                      String bicycleType, String couponCode,
                                                      double depositPaid, Map<String, Object> pricingSnapshot) {
        MutableLiveData<Resource<Booking>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // 1. Guard against simultaneous active/pending bookings by the same user
        db.collection(BOOKINGS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereIn("status", List.of(Booking.STATUS_PENDING, Booking.STATUS_ACTIVE))
            .get()
            .addOnSuccessListener(querySnapshots -> {
                Booking existingExpiredPending = null;
                boolean hasLiveBooking = false;

                if (querySnapshots != null && !querySnapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) {
                            if (Booking.STATUS_PENDING.equalsIgnoreCase(b.getStatus()) && b.isExpired()) {
                                existingExpiredPending = b;
                            } else {
                                hasLiveBooking = true;
                                break;
                            }
                        }
                    }
                }

                if (hasLiveBooking) {
                    result.setValue(Resource.error(
                        "You already have an active or reserved booking. Please complete or cancel it before booking another.", null));
                    return;
                }

                // If previous reservation expired, release it cleanly
                if (existingExpiredPending != null) {
                    expireBooking(existingExpiredPending.getBookingId(),
                            existingExpiredPending.getBicycleId(),
                            existingExpiredPending.getStationId());
                }

                // Expiry time set to 10 minutes from now
                Date expiresAt = new Date(System.currentTimeMillis() + 10 * 60 * 1000);

                // 2. Atomic reservation transaction on bicycle and station
                db.runTransaction(transaction -> {
                    var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
                    var bicycleSnap = transaction.get(bicycleRef);

                    Boolean available = bicycleSnap.getBoolean("available");
                    Boolean underMaintenance = bicycleSnap.getBoolean("underMaintenance");
                    String status = bicycleSnap.getString("status");

                    if (available == null || !available || Boolean.TRUE.equals(underMaintenance)
                            || (status != null && !Bicycle.STATUS_AVAILABLE.equalsIgnoreCase(status))) {
                        throw new RuntimeException("Bicycle is no longer available or under maintenance");
                    }

                    // Mark bicycle as RESERVED
                    Map<String, Object> bikeUpdates = new HashMap<>();
                    bikeUpdates.put("available", false);
                    bikeUpdates.put("status", Bicycle.STATUS_RESERVED);
                    transaction.update(bicycleRef, bikeUpdates);

                    // Decrement station available count
                    var stationRef = db.collection(STATIONS_COLLECTION).document(stationId);
                    transaction.update(stationRef, "availableBicycles", FieldValue.increment(-1));

                    return null;
                }).addOnSuccessListener(unused -> {
                    // 3. Create the booking document with PENDING status and expiresAt timestamp
                    Booking booking = new Booking(userId, bicycleId, stationId, stationName);
                    booking.setStatus(Booking.STATUS_PENDING);
                    booking.setExpiresAt(expiresAt);
                    booking.setBicycleType(bicycleType != null ? bicycleType : "Standard");
                    booking.setCouponCode(couponCode);
                    booking.setDepositAmount(depositPaid);
                    if (pricingSnapshot != null && !pricingSnapshot.isEmpty()) {
                        booking.setPricingSnapshot(pricingSnapshot);
                    }

                    db.collection(BOOKINGS_COLLECTION)
                        .add(booking)
                        .addOnSuccessListener(ref -> {
                            booking.setBookingId(ref.getId());
                            result.setValue(Resource.success(booking));
                        })
                        .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
                }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

            })
            .addOnFailureListener(e -> result.setValue(Resource.error("Failed to verify user booking state: " + e.getMessage(), null)));

        return result;
    }

    /**
     * Start the ride — validates reservation, ensures caller is owner,
     * updates booking to ACTIVE and bicycle to IN_USE atomically.
     */
    public LiveData<Resource<Void>> startRide(String bookingId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.runTransaction(transaction -> {
            var bookingRef = db.collection(BOOKINGS_COLLECTION).document(bookingId);
            var bookingSnap = transaction.get(bookingRef);

            if (!bookingSnap.exists()) {
                throw new RuntimeException("Booking not found");
            }

            String currentStatus = bookingSnap.getString("status");
            if (!Booking.STATUS_PENDING.equalsIgnoreCase(currentStatus)) {
                throw new RuntimeException("Cannot start ride: booking is " + currentStatus);
            }

            Date expiresAt = bookingSnap.getDate("expiresAt");
            Date createdAt = bookingSnap.getDate("createdAt");
            long now = System.currentTimeMillis();
            boolean isExpired = (expiresAt != null && now > expiresAt.getTime())
                    || (createdAt != null && (now - createdAt.getTime()) > 10 * 60 * 1000);

            String bicycleId = bookingSnap.getString("bicycleId");
            String stationId = bookingSnap.getString("stationId");

            if (isExpired) {
                // Auto-expire booking
                transaction.update(bookingRef, "status", Booking.STATUS_EXPIRED);
                if (bicycleId != null) {
                    var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
                    transaction.update(bicycleRef, "available", true, "status", Bicycle.STATUS_AVAILABLE);
                }
                if (stationId != null) {
                    var stationRef = db.collection(STATIONS_COLLECTION).document(stationId);
                    transaction.update(stationRef, "availableBicycles", FieldValue.increment(1));
                }
                throw new RuntimeException("Reservation has expired. Bicycle has been released.");
            }

            // Update booking to ACTIVE
            transaction.update(bookingRef,
                    "startTime", new Date(),
                    "status", Booking.STATUS_ACTIVE);

            // Update bicycle to IN_USE
            if (bicycleId != null) {
                var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
                transaction.update(bicycleRef,
                        "status", Bicycle.STATUS_IN_USE,
                        "available", false);
            }

            return null;
        }).addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
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
                        double distanceKm = response.containsKey("distanceKm")
                                ? ((Number) response.get("distanceKm")).doubleValue() : 0.0;
                        double subtotal = response.containsKey("subtotal")
                                ? ((Number) response.get("subtotal")).doubleValue() : fare;
                        double discount = response.containsKey("discount")
                                ? ((Number) response.get("discount")).doubleValue() : 0.0;
                        double tax = response.containsKey("tax")
                                ? ((Number) response.get("tax")).doubleValue() : 0.0;
                        String invoiceId = response.containsKey("invoiceId")
                                ? (String) response.get("invoiceId") : ("INV-" + System.currentTimeMillis());
                        finalizeRide(result, bookingId, bicycleId,
                                returnStationId, returnStationName,
                                stationGeoPoint, fare, durationMinutes, distanceKm,
                                invoiceId, subtotal, discount, tax);
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
                               double fare, long durationMinutes, double distanceKm,
                               String invoiceId, double subtotal, double discount, double tax) {

        db.runTransaction(transaction -> {
            var bookingRef = db.collection(BOOKINGS_COLLECTION).document(bookingId);
            com.google.firebase.firestore.DocumentSnapshot bookingSnap = transaction.get(bookingRef);
            if (!bookingSnap.exists()) {
                throw new RuntimeException("Booking not found");
            }
            String currentStatus = bookingSnap.getString("status");
            if (!Booking.STATUS_ACTIVE.equalsIgnoreCase(currentStatus)) {
                throw new RuntimeException("Ride is not active (current status: " + currentStatus + ")");
            }

            Double trackedDist = bookingSnap.getDouble("distanceKm");
            double finalDistance = (trackedDist != null && trackedDist > 0) ? trackedDist : distanceKm;

            // 1. Update booking
            Map<String, Object> bookingUpdates = new HashMap<>();
            bookingUpdates.put("endTime", new Date());
            bookingUpdates.put("status", Booking.STATUS_COMPLETED);
            bookingUpdates.put("fare", fare);
            bookingUpdates.put("durationMinutes", durationMinutes);
            bookingUpdates.put("distanceKm", finalDistance);
            bookingUpdates.put("returnStationId", returnStationId);
            bookingUpdates.put("returnStationName", returnStationName);
            bookingUpdates.put("endedBy", "RETURN_VALIDATED");
            if (invoiceId != null) {
                bookingUpdates.put("invoiceId", invoiceId);
            }
            bookingUpdates.put("subtotal", subtotal);
            bookingUpdates.put("discount", discount);
            bookingUpdates.put("tax", tax);

            if (stationGeoPoint != null) {
                bookingUpdates.put("dropOffLocation", stationGeoPoint);
            }
            transaction.update(bookingRef, bookingUpdates);

            // 2. Update bicycle — move it to the return station
            var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
            Map<String, Object> bicycleUpdates = new HashMap<>();
            bicycleUpdates.put("available",      true);
            bicycleUpdates.put("status",         Bicycle.STATUS_AVAILABLE);
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

            return finalDistance;

        }).addOnSuccessListener(finalDistance -> {
            // Create payment record
            Payment payment = new Payment(bookingId, null, fare);
            payment.setStatus(Payment.STATUS_SUCCESS);
            db.collection(PAYMENTS_COLLECTION).add(payment);

            // Build and return booking directly with known values
            // (don't re-fetch — Firestore may not have synced yet)
            Booking completedBooking = new Booking();
            completedBooking.setBookingId(bookingId);
            completedBooking.setBicycleId(bicycleId);
            completedBooking.setFare(fare);
            completedBooking.setDurationMinutes(durationMinutes);
            completedBooking.setDistanceKm(finalDistance != null ? (double) finalDistance : distanceKm);
            completedBooking.setReturnStationId(returnStationId);
            completedBooking.setReturnStationName(returnStationName);
            completedBooking.setInvoiceId(invoiceId);
            completedBooking.setSubtotal(subtotal);
            completedBooking.setDiscount(discount);
            completedBooking.setTax(tax);
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
                if (!Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus())) {
                    result.setValue(Resource.error("Ride is not currently active.", null));
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

                PricingConfig config = parsePricingConfigFromSnapshot(booking.getPricingSnapshot());
                Coupon coupon = null;
                if (booking.getCouponCode() != null && !booking.getCouponCode().isEmpty()) {
                    coupon = new Coupon(booking.getCouponCode(), Coupon.TYPE_FLAT, 1000L, "Discount");
                }

                Invoice invoice = BillingService.getInstance().generateInvoice(
                        config,
                        booking,
                        durationMinutes,
                        distanceKm,
                        coupon,
                        booking.getDepositAmount()
                );

                db.collection("invoices").document(invoice.getInvoiceId()).set(invoice);

                finalizeRide(result, bookingId, bicycleId,
                        returnStationId, returnStationName,
                        stationGeoPoint, invoice.getTotalAmountInr(), durationMinutes, distanceKm,
                        invoice.getInvoiceId(), invoice.getSubtotalInr(), invoice.getDiscountInr(), invoice.getTotalTaxInr());
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
                        Boolean hidden = doc.getBoolean("hiddenByUser");
                        if (Boolean.TRUE.equals(hidden)) continue;
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
                        // Auto-expire PENDING bookings if past expiration window
                        if (Booking.STATUS_PENDING.equalsIgnoreCase(booking.getStatus()) && booking.isExpired()) {
                            expireBooking(booking.getBookingId(),
                                    booking.getBicycleId(), booking.getStationId());
                            result.setValue(Resource.success(null));
                            return;
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
     * Cancel a pending booking. Releases reserved bicycle back to AVAILABLE.
     */
    public LiveData<Resource<Void>> cancelBooking(String bookingId, String bicycleId, String stationId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.runTransaction(transaction -> {
            var bookingRef = db.collection(BOOKINGS_COLLECTION).document(bookingId);
            var bookingSnap = transaction.get(bookingRef);

            if (bookingSnap.exists()) {
                String currentStatus = bookingSnap.getString("status");
                if (Booking.STATUS_ACTIVE.equalsIgnoreCase(currentStatus)
                        || Booking.STATUS_COMPLETED.equalsIgnoreCase(currentStatus)) {
                    throw new RuntimeException("Cannot cancel a ride that is already " + currentStatus);
                }
            }

            transaction.update(bookingRef, "status", Booking.STATUS_CANCELLED);

            var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
            Map<String, Object> bikeUpdates = new HashMap<>();
            bikeUpdates.put("available", true);
            bikeUpdates.put("status", Bicycle.STATUS_AVAILABLE);
            transaction.update(bicycleRef, bikeUpdates);

            var stationRef = db.collection(STATIONS_COLLECTION).document(stationId);
            transaction.update(stationRef, "availableBicycles", FieldValue.increment(1));

            return null;
        }).addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
          .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Mark an expired pending booking as EXPIRED and release bicycle back to AVAILABLE.
     */
    public LiveData<Resource<Void>> expireBooking(String bookingId, String bicycleId, String stationId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.runTransaction(transaction -> {
            var bookingRef = db.collection(BOOKINGS_COLLECTION).document(bookingId);
            var bookingSnap = transaction.get(bookingRef);

            if (bookingSnap.exists()) {
                String currentStatus = bookingSnap.getString("status");
                // Only pending bookings can transition to EXPIRED
                if (Booking.STATUS_PENDING.equalsIgnoreCase(currentStatus)) {
                    transaction.update(bookingRef, "status", Booking.STATUS_EXPIRED);

                    var bicycleRef = db.collection(BICYCLES_COLLECTION).document(bicycleId);
                    Map<String, Object> bikeUpdates = new HashMap<>();
                    bikeUpdates.put("available", true);
                    bikeUpdates.put("status", Bicycle.STATUS_AVAILABLE);
                    transaction.update(bicycleRef, bikeUpdates);

                    var stationRef = db.collection(STATIONS_COLLECTION).document(stationId);
                    transaction.update(stationRef, "availableBicycles", FieldValue.increment(1));
                }
            }

            return null;
        }).addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
          .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    public LiveData<Resource<Invoice>> getInvoice(String invoiceId) {
        MutableLiveData<Resource<Invoice>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (invoiceId == null || invoiceId.isEmpty()) {
            result.setValue(Resource.error("Invoice ID is required", null));
            return result;
        }

        db.collection("invoices").document(invoiceId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Invoice inv = snap.toObject(Invoice.class);
                        if (inv != null) {
                            inv.setInvoiceId(snap.getId());
                            result.setValue(Resource.success(inv));
                            return;
                        }
                    }
                    result.setValue(Resource.error("Invoice not found", null));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    public LiveData<Resource<Invoice>> getBookingInvoice(String bookingId) {
        MutableLiveData<Resource<Invoice>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (bookingId == null || bookingId.isEmpty()) {
            result.setValue(Resource.error("Booking ID is required", null));
            return result;
        }

        db.collection("invoices").whereEqualTo("bookingId", bookingId).limit(1).get()
                .addOnSuccessListener(snaps -> {
                    if (snaps != null && !snaps.isEmpty()) {
                        Invoice inv = snaps.getDocuments().get(0).toObject(Invoice.class);
                        if (inv != null) {
                            inv.setInvoiceId(snaps.getDocuments().get(0).getId());
                            result.setValue(Resource.success(inv));
                            return;
                        }
                    }
                    result.setValue(Resource.error("Invoice not found", null));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    private PricingConfig parsePricingConfigFromSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return PricingConfig.getDefaultSampleConfig();
        }
        PricingConfig config = new PricingConfig();
        if (snapshot.get("baseFeePaise") instanceof Number) {
            config.setBaseFeePaise(((Number) snapshot.get("baseFeePaise")).longValue());
        }
        if (snapshot.get("ratePerMinutePaise") instanceof Number) {
            config.setRatePerMinutePaise(((Number) snapshot.get("ratePerMinutePaise")).longValue());
        }
        if (snapshot.get("ratePerHourPaise") instanceof Number) {
            config.setRatePerHourPaise(((Number) snapshot.get("ratePerHourPaise")).longValue());
        }
        if (snapshot.get("minimumFarePaise") instanceof Number) {
            config.setMinimumFarePaise(((Number) snapshot.get("minimumFarePaise")).longValue());
        }
        if (snapshot.get("maximumDurationMinutes") instanceof Number) {
            config.setMaximumDurationMinutes(((Number) snapshot.get("maximumDurationMinutes")).intValue());
        }
        if (snapshot.get("lateFeePerHourPaise") instanceof Number) {
            config.setLateFeePerHourPaise(((Number) snapshot.get("lateFeePerHourPaise")).longValue());
        }
        if (snapshot.get("taxEnabled") instanceof Boolean) {
            config.setTaxEnabled((Boolean) snapshot.get("taxEnabled"));
        }
        if (snapshot.get("taxPercentage") instanceof Number) {
            config.setTaxPercentage(((Number) snapshot.get("taxPercentage")).doubleValue());
        }
        if (snapshot.get("standardCategoryMultiplier") instanceof Number) {
            config.setStandardCategoryMultiplier(((Number) snapshot.get("standardCategoryMultiplier")).doubleValue());
        }
        if (snapshot.get("electricCategoryMultiplier") instanceof Number) {
            config.setElectricCategoryMultiplier(((Number) snapshot.get("electricCategoryMultiplier")).doubleValue());
        }
        if (snapshot.get("mountainCategoryMultiplier") instanceof Number) {
            config.setMountainCategoryMultiplier(((Number) snapshot.get("mountainCategoryMultiplier")).doubleValue());
        }
        return config;
    }
}
