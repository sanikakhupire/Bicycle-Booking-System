package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Immutable ledger entry for Points Earned, Redeemed, Milestone Bonuses, and Referral Credits.
 */
public class LoyaltyTransaction {

    public static final String TYPE_EARNED          = "EARNED";
    public static final String TYPE_REDEEMED        = "REDEEMED";
    public static final String TYPE_MILESTONE_BONUS = "MILESTONE_BONUS";
    public static final String TYPE_REFERRAL_BONUS  = "REFERRAL_BONUS";

    @DocumentId
    private String transactionId;
    private String userId;
    private String type = TYPE_EARNED;
    private long points = 0; // Positive for additions, negative for redemptions
    private String description;
    private String bookingId;

    @ServerTimestamp
    private Date timestamp;

    public LoyaltyTransaction() {}

    public LoyaltyTransaction(String userId, String type, long points, String description, String bookingId) {
        this.userId = userId;
        this.type = type;
        this.points = points;
        this.description = description;
        this.bookingId = bookingId;
        this.timestamp = new Date();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
