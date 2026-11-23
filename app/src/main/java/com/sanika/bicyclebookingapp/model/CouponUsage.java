package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Tracks individual coupon redemptions to prevent reuse and enforce per-user limits.
 */
public class CouponUsage {

    @DocumentId
    private String usageId;
    private String couponId;
    private String couponCode;
    private String userId;
    private String bookingId;
    private long discountAmountPaise;

    @ServerTimestamp
    private Date usedAt;

    public CouponUsage() {}

    public CouponUsage(String couponId, String couponCode, String userId, String bookingId, long discountAmountPaise) {
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.userId = userId;
        this.bookingId = bookingId;
        this.discountAmountPaise = discountAmountPaise;
        this.usedAt = new Date();
    }

    public String getUsageId() { return usageId; }
    public void setUsageId(String usageId) { this.usageId = usageId; }

    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public long getDiscountAmountPaise() { return discountAmountPaise; }
    public void setDiscountAmountPaise(long discountAmountPaise) { this.discountAmountPaise = discountAmountPaise; }

    public Date getUsedAt() { return usedAt; }
    public void setUsedAt(Date usedAt) { this.usedAt = usedAt; }
}
