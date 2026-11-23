package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.Date;

/**
 * Coupon model for Indian promotions and promotional discounts.
 * Supports percentage and flat INR discounts, first-ride restrictions,
 * expiry dates, system-wide usage caps, and per-user limits.
 */
public class Coupon {

    public static final String TYPE_FLAT = "FLAT";
    public static final String TYPE_PERCENTAGE = "PERCENTAGE";

    @DocumentId
    private String couponId;
    private String code;
    private String discountType = TYPE_FLAT;
    private long discountValuePaise = 1000L;       // ₹10.00 flat discount
    private double discountPercentage = 10.0;      // 10%
    private long maxDiscountPaise = 5000L;         // ₹50.00 max discount
    private long minRideFarePaise = 2000L;         // ₹20.00 min fare to apply
    private boolean active = true;
    private String description = "Get ₹10 off on your ride";

    // Advanced eligibility & limit rules
    private boolean firstRideOnly = false;
    private Date expiryDate;
    private int usageLimit = 0;        // 0 = unlimited system-wide
    private int currentUsageCount = 0; // Total times redeemed
    private int perUserLimit = 1;      // Max redemption per individual customer

    public Coupon() {}

    public Coupon(String code, String discountType, long valuePaise, String description) {
        this.code = code != null ? code.toUpperCase() : null;
        this.discountType = discountType;
        this.discountValuePaise = valuePaise;
        this.description = description;
        this.active = true;
    }

    public Coupon(String code, String discountType, long valueOrPercent, long minRideFarePaise, long maxDiscountPaise, boolean active) {
        this.code = code != null ? code.toUpperCase() : null;
        this.discountType = discountType;
        if (TYPE_PERCENTAGE.equalsIgnoreCase(discountType)) {
            this.discountPercentage = valueOrPercent;
        } else {
            this.discountValuePaise = valueOrPercent;
        }
        this.minRideFarePaise = minRideFarePaise;
        this.maxDiscountPaise = maxDiscountPaise;
        this.active = active;
    }

    /**
     * Checks whether the coupon is currently valid and unexpired.
     */
    public boolean isExpired() {
        if (expiryDate == null) return false;
        return new Date().after(expiryDate);
    }

    /**
     * Checks if the coupon has reached its system-wide usage cap.
     */
    public boolean isUsageLimitReached() {
        if (usageLimit <= 0) return false;
        return currentUsageCount >= usageLimit;
    }

    /**
     * Validates eligibility against the subtotal, customer's completed rides count, and prior user usages.
     * Returns null if eligible, or an error reason string if ineligible.
     */
    public String getIneligibilityReason(long subtotalPaise, int userCompletedRides, int userUsageCount) {
        if (!active) {
            return "Coupon code is inactive";
        }
        if (isExpired()) {
            return "Coupon code has expired";
        }
        if (isUsageLimitReached()) {
            return "Coupon usage limit has been reached";
        }
        if (perUserLimit > 0 && userUsageCount >= perUserLimit) {
            return "You have already reached the redemption limit for this coupon";
        }
        if (firstRideOnly && userCompletedRides > 0) {
            return "This coupon is valid only for your first ride";
        }
        if (subtotalPaise < minRideFarePaise) {
            return "Minimum ride fare of ₹" + String.format("%.2f", minRideFarePaise / 100.0) + " required for this coupon";
        }
        return null; // Eligible
    }

    /**
     * Calculates the discount in integer paise for a given subtotal.
     */
    public long calculateDiscountPaise(long subtotalPaise) {
        if (!active || isExpired() || isUsageLimitReached() || subtotalPaise < minRideFarePaise) {
            return 0L;
        }

        if (TYPE_PERCENTAGE.equalsIgnoreCase(discountType)) {
            long calcDiscount = Math.round(subtotalPaise * (discountPercentage / 100.0));
            if (maxDiscountPaise > 0) {
                return Math.min(calcDiscount, maxDiscountPaise);
            }
            return calcDiscount;
        } else {
            // Flat discount capped at the subtotal
            return Math.min(discountValuePaise, subtotalPaise);
        }
    }

    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.toUpperCase() : null; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public long getDiscountValuePaise() { return discountValuePaise; }
    public void setDiscountValuePaise(long discountValuePaise) { this.discountValuePaise = discountValuePaise; }

    public double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }

    public long getMaxDiscountPaise() { return maxDiscountPaise; }
    public void setMaxDiscountPaise(long maxDiscountPaise) { this.maxDiscountPaise = maxDiscountPaise; }

    public long getMinRideFarePaise() { return minRideFarePaise; }
    public void setMinRideFarePaise(long minRideFarePaise) { this.minRideFarePaise = minRideFarePaise; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isFirstRideOnly() { return firstRideOnly; }
    public void setFirstRideOnly(boolean firstRideOnly) { this.firstRideOnly = firstRideOnly; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }

    public int getCurrentUsageCount() { return currentUsageCount; }
    public void setCurrentUsageCount(int currentUsageCount) { this.currentUsageCount = currentUsageCount; }

    public int getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(int perUserLimit) { this.perUserLimit = perUserLimit; }

    public double getDiscountValueInr() { return CurrencyUtil.toRupees(discountValuePaise); }
    public void setDiscountValueInr(double inr) { this.discountValuePaise = CurrencyUtil.toPaise(inr); }
}
