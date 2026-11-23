package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Tracks customer invitation referrals and qualifying ride completion events.
 * Strictly prevents self-referrals and guarantees reward release ONLY upon first ride completion.
 */
public class Referral {

    public static final String STATUS_PENDING   = "PENDING";   // Referee signed up with code, has not completed ride
    public static final String STATUS_QUALIFIED = "QUALIFIED"; // Referee completed first valid paid ride
    public static final String STATUS_REWARDED  = "REWARDED";  // Referrer credited with reward points / coupons

    public static final int DEFAULT_REFERRER_REWARD_POINTS = 200; // ₹20 worth of points
    public static final String REFEREE_WELCOME_COUPON_CODE = "WELCOME20"; // 20% discount coupon

    @DocumentId
    private String referralId;
    private String referrerUserId;
    private String referrerName;
    private String referrerCode;

    private String refereeUserId;
    private String refereeName;
    private String refereePhone;

    private String status = STATUS_PENDING;
    private String qualifyingBookingId;
    private int rewardPoints = DEFAULT_REFERRER_REWARD_POINTS;

    @ServerTimestamp
    private Date createdAt;
    private Date qualifiedAt;
    private Date rewardedAt;

    public Referral() {}

    public Referral(String referrerUserId, String referrerName, String referrerCode,
                    String refereeUserId, String refereeName, String refereePhone) {
        this.referrerUserId = referrerUserId;
        this.referrerName = referrerName;
        this.referrerCode = referrerCode;
        this.refereeUserId = refereeUserId;
        this.refereeName = refereeName;
        this.refereePhone = refereePhone;
        this.status = STATUS_PENDING;
        this.rewardPoints = DEFAULT_REFERRER_REWARD_POINTS;
        this.createdAt = new Date();
    }

    public String getReferralId() { return referralId; }
    public void setReferralId(String referralId) { this.referralId = referralId; }

    public String getReferrerUserId() { return referrerUserId; }
    public void setReferrerUserId(String referrerUserId) { this.referrerUserId = referrerUserId; }

    public String getReferrerName() { return referrerName; }
    public void setReferrerName(String referrerName) { this.referrerName = referrerName; }

    public String getReferrerCode() { return referrerCode; }
    public void setReferrerCode(String referrerCode) { this.referrerCode = referrerCode; }

    public String getRefereeUserId() { return refereeUserId; }
    public void setRefereeUserId(String refereeUserId) { this.refereeUserId = refereeUserId; }

    public String getRefereeName() { return refereeName; }
    public void setRefereeName(String refereeName) { this.refereeName = refereeName; }

    public String getRefereePhone() { return refereePhone; }
    public void setRefereePhone(String refereePhone) { this.refereePhone = refereePhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQualifyingBookingId() { return qualifyingBookingId; }
    public void setQualifyingBookingId(String qualifyingBookingId) { this.qualifyingBookingId = qualifyingBookingId; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getQualifiedAt() { return qualifiedAt; }
    public void setQualifiedAt(Date qualifiedAt) { this.qualifiedAt = qualifiedAt; }

    public Date getRewardedAt() { return rewardedAt; }
    public void setRewardedAt(Date rewardedAt) { this.rewardedAt = rewardedAt; }
}
