package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a payment record in Firestore collection "payments".
 */
public class Payment {

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_SUCCESS   = "SUCCESS";
    public static final String STATUS_FAILED    = "FAILED";

    @DocumentId
    private String paymentId;
    private String bookingId;
    private String userId;
    private double amount;
    private String status;
    private String method; // "SIMULATED", "RAZORPAY", etc.

    @ServerTimestamp
    private Date paidAt;

    // Required empty constructor for Firestore
    public Payment() {}

    public Payment(String bookingId, String userId, double amount) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.status = STATUS_PENDING;
        this.method = "SIMULATED";
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Date getPaidAt() { return paidAt; }
    public void setPaidAt(Date paidAt) { this.paidAt = paidAt; }
}
