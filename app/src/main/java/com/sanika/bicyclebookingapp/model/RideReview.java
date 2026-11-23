package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Represents a customer rating and review for a completed ride.
 * Captures overall experience, bicycle condition, and station feedback.
 */
public class RideReview {

    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_FLAGGED   = "FLAGGED";
    public static final String STATUS_HIDDEN    = "HIDDEN";

    public static final String CONDITION_EXCELLENT     = "EXCELLENT";
    public static final String CONDITION_GOOD          = "GOOD";
    public static final String CONDITION_FAIR          = "FAIR";
    public static final String CONDITION_NEEDS_SERVICE = "NEEDS_SERVICE";
    public static final String CONDITION_DAMAGED       = "DAMAGED";

    @DocumentId
    private String reviewId;
    private String bookingId; // Unique 1-to-1 constraint with completed booking
    private String userId;
    private String userName;

    private String bicycleId;
    private String pickupStationId;
    private String pickupStationName;
    private String returnStationId;
    private String returnStationName;

    private float ratingOverall = 5.0f; // 1.0 to 5.0
    private float ratingBicycle = 5.0f;
    private float ratingStation = 5.0f;

    private String bicycleCondition = CONDITION_EXCELLENT;
    private String comment;
    private String status = STATUS_PUBLISHED;

    @ServerTimestamp
    private Date createdAt;
    private Date updatedAt;
    private String moderatedBy;
    private String moderationReason;

    public RideReview() {}

    public RideReview(String bookingId, String userId, String userName, String bicycleId,
                      float ratingOverall, float ratingBicycle, String bicycleCondition, String comment) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.userName = userName;
        this.bicycleId = bicycleId;
        this.ratingOverall = ratingOverall;
        this.ratingBicycle = ratingBicycle;
        this.bicycleCondition = bicycleCondition != null ? bicycleCondition : CONDITION_GOOD;
        this.comment = comment;
        this.status = STATUS_PUBLISHED;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getPickupStationId() { return pickupStationId; }
    public void setPickupStationId(String pickupStationId) { this.pickupStationId = pickupStationId; }

    public String getPickupStationName() { return pickupStationName; }
    public void setPickupStationName(String pickupStationName) { this.pickupStationName = pickupStationName; }

    public String getReturnStationId() { return returnStationId; }
    public void setReturnStationId(String returnStationId) { this.returnStationId = returnStationId; }

    public String getReturnStationName() { return returnStationName; }
    public void setReturnStationName(String returnStationName) { this.returnStationName = returnStationName; }

    public float getRatingOverall() { return ratingOverall; }
    public void setRatingOverall(float ratingOverall) { this.ratingOverall = ratingOverall; }

    public float getRatingBicycle() { return ratingBicycle; }
    public void setRatingBicycle(float ratingBicycle) { this.ratingBicycle = ratingBicycle; }

    public float getRatingStation() { return ratingStation; }
    public void setRatingStation(float ratingStation) { this.ratingStation = ratingStation; }

    public String getBicycleCondition() { return bicycleCondition; }
    public void setBicycleCondition(String bicycleCondition) { this.bicycleCondition = bicycleCondition; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getModeratedBy() { return moderatedBy; }
    public void setModeratedBy(String moderatedBy) { this.moderatedBy = moderatedBy; }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
}
