package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.PricingConfig;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing dynamic pricing configuration and promotional coupons.
 */
public class PricingRepository {

    public static final String PRICING_COLLECTION = "pricing_config";
    public static final String COUPONS_COLLECTION = "coupons";
    public static final String CONFIG_DOC_ID = "default";

    private final FirebaseFirestore db;
    private static volatile PricingRepository instance;

    private PricingConfig cachedConfig = null;

    public PricingRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static PricingRepository getInstance() {
        if (instance == null) {
            synchronized (PricingRepository.class) {
                if (instance == null) {
                    instance = new PricingRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Gets the active pricing configuration. If none exists in Firestore,
     * writes and initializes the default sample development config.
     */
    public LiveData<Resource<PricingConfig>> getPricingConfig() {
        MutableLiveData<Resource<PricingConfig>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(cachedConfig));

        db.collection(PRICING_COLLECTION).document(CONFIG_DOC_ID)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        PricingConfig config = snap.toObject(PricingConfig.class);
                        if (config != null) {
                            config.setConfigId(snap.getId());
                            cachedConfig = config;
                            result.setValue(Resource.success(config));
                            return;
                        }
                    }

                    // Not found in Firestore: initialize with default sample configuration
                    PricingConfig defaultConfig = PricingConfig.getDefaultSampleConfig();
                    db.collection(PRICING_COLLECTION).document(CONFIG_DOC_ID)
                            .set(defaultConfig.toMap())
                            .addOnSuccessListener(v -> {
                                cachedConfig = defaultConfig;
                                result.setValue(Resource.success(defaultConfig));
                            })
                            .addOnFailureListener(e -> {
                                // Return sample config anyway as fallback
                                cachedConfig = defaultConfig;
                                result.setValue(Resource.success(defaultConfig));
                            });
                })
                .addOnFailureListener(e -> {
                    // Fallback to sample config on network error
                    if (cachedConfig != null) {
                        result.setValue(Resource.success(cachedConfig));
                    } else {
                        PricingConfig sample = PricingConfig.getDefaultSampleConfig();
                        cachedConfig = sample;
                        result.setValue(Resource.success(sample));
                    }
                });

        return result;
    }

    /**
     * Saves updated pricing configuration. Strictly restricted to Admin users.
     */
    public LiveData<Resource<PricingConfig>> savePricingConfig(PricingConfig config) {
        MutableLiveData<Resource<PricingConfig>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (config == null) {
            result.setValue(Resource.error("Invalid configuration", null));
            return result;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        config.setUpdatedBy(uid);
        config.setUpdatedAt(new Date());

        Map<String, Object> map = config.toMap();
        map.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(PRICING_COLLECTION).document(CONFIG_DOC_ID)
                .set(map)
                .addOnSuccessListener(v -> {
                    cachedConfig = config;
                    result.setValue(Resource.success(config));
                })
                .addOnFailureListener(e ->
                        result.setValue(Resource.error("Failed to save pricing: " + e.getMessage(), null)));

        return result;
    }

    public static final String COUPON_USAGES_COLLECTION = "coupon_usages";

    /**
     * Validates a promotional coupon code against Firestore with subtotal check.
     */
    public LiveData<Resource<Coupon>> validateCoupon(String code, long subtotalPaise) {
        return validateCoupon(code, null, subtotalPaise);
    }

    /**
     * Complete Phase 10 validation: checks active status, expiry, min fare,
     * system usage limit, first-ride restriction, and per-user redemption limit.
     */
    public LiveData<Resource<Coupon>> validateCoupon(String code, String userId, long subtotalPaise) {
        MutableLiveData<Resource<Coupon>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (code == null || code.trim().isEmpty()) {
            result.setValue(Resource.error("Please enter a coupon code", null));
            return result;
        }

        String cleanCode = code.trim().toUpperCase();

        // Sample builtin coupon check for offline/test convenience
        if ("WELCOME10".equalsIgnoreCase(cleanCode)) {
            Coupon sampleCoupon = new Coupon("WELCOME10", Coupon.TYPE_FLAT, 1000L, "₹10 Welcome Discount");
            result.setValue(Resource.success(sampleCoupon));
            return result;
        }

        db.collection(COUPONS_COLLECTION)
                .whereEqualTo("code", cleanCode)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        result.setValue(Resource.error("Invalid or inactive coupon code", null));
                        return;
                    }

                    Coupon coupon = snaps.getDocuments().get(0).toObject(Coupon.class);
                    if (coupon == null) {
                        result.setValue(Resource.error("Invalid coupon data", null));
                        return;
                    }
                    coupon.setCouponId(snaps.getDocuments().get(0).getId());

                    // Check basic expiration and system usage limit
                    if (coupon.isExpired()) {
                        result.setValue(Resource.error("This coupon code has expired", null));
                        return;
                    }
                    if (coupon.isUsageLimitReached()) {
                        result.setValue(Resource.error("Coupon redemption limit has been reached", null));
                        return;
                    }
                    if (subtotalPaise < coupon.getMinRideFarePaise()) {
                        result.setValue(Resource.error(
                                "Minimum ride fare of ₹" + String.format(java.util.Locale.ENGLISH, "%.2f", coupon.getMinRideFarePaise() / 100.0)
                                        + " required for this coupon", null));
                        return;
                    }

                    // If userId is provided, evaluate user-specific restrictions (first-ride and per-user limit)
                    if (userId != null && !userId.isEmpty()) {
                        // Check per-user limit
                        db.collection(COUPON_USAGES_COLLECTION)
                                .whereEqualTo("couponCode", cleanCode)
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener(usageSnaps -> {
                                    int userUsages = usageSnaps != null ? usageSnaps.size() : 0;
                                    if (coupon.getPerUserLimit() > 0 && userUsages >= coupon.getPerUserLimit()) {
                                        result.setValue(Resource.error("You have already used this coupon", null));
                                        return;
                                    }

                                    // Check first-ride restriction
                                    if (coupon.isFirstRideOnly()) {
                                        db.collection("bookings")
                                                .whereEqualTo("userId", userId)
                                                .whereEqualTo("status", "COMPLETED")
                                                .get()
                                                .addOnSuccessListener(bookingSnaps -> {
                                                    int completedRides = bookingSnaps != null ? bookingSnaps.size() : 0;
                                                    if (completedRides > 0) {
                                                        result.setValue(Resource.error("This coupon is valid only for first-time riders", null));
                                                        return;
                                                    }
                                                    result.setValue(Resource.success(coupon));
                                                })
                                                .addOnFailureListener(e -> result.setValue(Resource.success(coupon)));
                                    } else {
                                        result.setValue(Resource.success(coupon));
                                    }
                                })
                                .addOnFailureListener(e -> result.setValue(Resource.success(coupon)));
                    } else {
                        result.setValue(Resource.success(coupon));
                    }
                })
                .addOnFailureListener(e ->
                        result.setValue(Resource.error("Failed to check coupon: " + e.getMessage(), null)));

        return result;
    }

    /**
     * Record coupon redemption and increment usage counters.
     */
    public void recordCouponUsage(String couponId, String couponCode, String userId, String bookingId, long discountPaise) {
        if (couponCode == null || userId == null) return;

        Map<String, Object> usage = new HashMap<>();
        usage.put("couponId", couponId);
        usage.put("couponCode", couponCode.toUpperCase());
        usage.put("userId", userId);
        usage.put("bookingId", bookingId);
        usage.put("discountAmountPaise", discountPaise);
        usage.put("usedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(COUPON_USAGES_COLLECTION).add(usage);

        if (couponId != null && !couponId.isEmpty()) {
            db.collection(COUPONS_COLLECTION).document(couponId)
                    .update("currentUsageCount", com.google.firebase.firestore.FieldValue.increment(1));
        }
    }
}
