package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

/**
 * Coupon model for Indian promotions and promotional discounts.
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

    public Coupon() {}

    public Coupon(String code, String discountType, long valuePaise, String description) {
        this.code = code;
        this.discountType = discountType;
        this.discountValuePaise = valuePaise;
        this.description = description;
        this.active = true;
    }

    public Coupon(String code, String discountType, long valueOrPercent, long minRideFarePaise, long maxDiscountPaise, boolean active) {
        this.code = code;
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
     * Calculates the discount in integer paise for a given subtotal.
     */
    public long calculateDiscountPaise(long subtotalPaise) {
        if (!active || subtotalPaise < minRideFarePaise) {
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
    public void setCode(String code) { this.code = code; }

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

    public double getDiscountValueInr() { return CurrencyUtil.toRupees(discountValuePaise); }
    public void setDiscountValueInr(double inr) { this.discountValuePaise = CurrencyUtil.toPaise(inr); }
}
