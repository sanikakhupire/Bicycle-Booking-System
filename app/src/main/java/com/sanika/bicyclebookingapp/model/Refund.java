package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.Date;

/**
 * Refund Record Model.
 *
 * Tracks refund lifecycle for deposits, cancellations, overpayments, or customer disputes.
 * Supports:
 * - Status tracking: PENDING -> PROCESSED / FAILED
 * - Integer paise precision
 * - Gateway refund ID association (Razorpay rfrnd_...) or internal wallet credit
 */
public class Refund {

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED    = "FAILED";

    public static final String REASON_DEPOSIT_RETURN  = "SECURITY_DEPOSIT_RETURN";
    public static final String REASON_CANCELLATION    = "BOOKING_CANCELLATION";
    public static final String REASON_EXCESS_PAYMENT  = "EXCESS_PAYMENT";
    public static final String REASON_CUSTOMER_DISPUTE = "CUSTOMER_DISPUTE";

    @DocumentId
    private String refundId;
    private String paymentId;
    private String bookingId;
    private String userId;

    private long amountPaise = 0L;
    private String currency = CurrencyUtil.CURRENCY_CODE;
    private String reason = REASON_DEPOSIT_RETURN;
    private String status = STATUS_PENDING;

    private String gatewayRefundId; // Razorpay refund ID
    private String processedBy = "SYSTEM";
    private String notes;

    @ServerTimestamp
    private Date createdAt;
    private Date processedAt;

    public Refund() {}

    public Refund(String paymentId, String bookingId, String userId, long amountPaise, String reason) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.amountPaise = amountPaise;
        this.reason = reason;
        this.status = STATUS_PENDING;
        this.currency = CurrencyUtil.CURRENCY_CODE;
    }

    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(long amountPaise) { this.amountPaise = amountPaise; }
    public double getAmountInr() { return CurrencyUtil.toRupees(amountPaise); }
    public double getAmount() { return getAmountInr(); }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGatewayRefundId() { return gatewayRefundId; }
    public void setGatewayRefundId(String gatewayRefundId) { this.gatewayRefundId = gatewayRefundId; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getProcessedAt() { return processedAt; }
    public void setProcessedAt(Date processedAt) { this.processedAt = processedAt; }

    public String getFormattedAmount() {
        return CurrencyUtil.formatInrFromPaise(amountPaise);
    }
}
