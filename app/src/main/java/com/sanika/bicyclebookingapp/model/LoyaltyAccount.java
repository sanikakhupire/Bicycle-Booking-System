package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Customer Loyalty Wallet and Gamification Tier account.
 * Maintains points balance, ride milestones, and tiered benefits.
 */
public class LoyaltyAccount {

    public static final String TIER_BRONZE   = "BRONZE";
    public static final String TIER_SILVER   = "SILVER";
    public static final String TIER_GOLD     = "GOLD";
    public static final String TIER_PLATINUM = "PLATINUM";

    // 100 points = ₹10.00 (1 point = 10 paise)
    public static final int PAISE_PER_POINT = 10;
    // Earn 1 point per ₹10 spent on rides
    public static final int INR_SPENT_PER_EARNED_POINT = 10;

    @DocumentId
    private String userId;
    private long pointsBalance = 0;
    private long lifetimePointsEarned = 0;
    private long lifetimePointsRedeemed = 0;
    private int totalCompletedRides = 0;
    private String tierLevel = TIER_BRONZE;
    private String referralCode;

    // Milestones tracking
    private boolean milestoneBronzeClaimed = false;   // 5 rides: +50 pts
    private boolean milestoneSilverClaimed = false;   // 15 rides: +150 pts
    private boolean milestoneGoldClaimed = false;     // 30 rides: +350 pts
    private boolean milestonePlatinumClaimed = false; // 50 rides: +1000 pts

    @ServerTimestamp
    private Date updatedAt;

    public LoyaltyAccount() {}

    public LoyaltyAccount(String userId, String referralCode) {
        this.userId = userId;
        this.referralCode = referralCode;
        this.pointsBalance = 0;
        this.tierLevel = TIER_BRONZE;
    }

    /**
     * Calculates the rupee equivalent of points at 10 paise per point.
     */
    public double getPointsValueInr() {
        return (pointsBalance * PAISE_PER_POINT) / 100.0;
    }

    /**
     * Update tier based on completed rides or lifetime earned points.
     */
    public void evaluateTier() {
        if (totalCompletedRides >= 50 || lifetimePointsEarned >= 2500) {
            this.tierLevel = TIER_PLATINUM;
        } else if (totalCompletedRides >= 30 || lifetimePointsEarned >= 1000) {
            this.tierLevel = TIER_GOLD;
        } else if (totalCompletedRides >= 15 || lifetimePointsEarned >= 400) {
            this.tierLevel = TIER_SILVER;
        } else {
            this.tierLevel = TIER_BRONZE;
        }
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(long pointsBalance) { this.pointsBalance = pointsBalance; }

    public long getLifetimePointsEarned() { return lifetimePointsEarned; }
    public void setLifetimePointsEarned(long lifetimePointsEarned) { this.lifetimePointsEarned = lifetimePointsEarned; }

    public long getLifetimePointsRedeemed() { return lifetimePointsRedeemed; }
    public void setLifetimePointsRedeemed(long lifetimePointsRedeemed) { this.lifetimePointsRedeemed = lifetimePointsRedeemed; }

    public int getTotalCompletedRides() { return totalCompletedRides; }
    public void setTotalCompletedRides(int totalCompletedRides) { this.totalCompletedRides = totalCompletedRides; }

    public String getTierLevel() { return tierLevel; }
    public void setTierLevel(String tierLevel) { this.tierLevel = tierLevel; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public boolean isMilestoneBronzeClaimed() { return milestoneBronzeClaimed; }
    public void setMilestoneBronzeClaimed(boolean milestoneBronzeClaimed) { this.milestoneBronzeClaimed = milestoneBronzeClaimed; }

    public boolean isMilestoneSilverClaimed() { return milestoneSilverClaimed; }
    public void setMilestoneSilverClaimed(boolean milestoneSilverClaimed) { this.milestoneSilverClaimed = milestoneSilverClaimed; }

    public boolean isMilestoneGoldClaimed() { return milestoneGoldClaimed; }
    public void setMilestoneGoldClaimed(boolean milestoneGoldClaimed) { this.milestoneGoldClaimed = milestoneGoldClaimed; }

    public boolean isMilestonePlatinumClaimed() { return milestonePlatinumClaimed; }
    public void setMilestonePlatinumClaimed(boolean milestonePlatinumClaimed) { this.milestonePlatinumClaimed = milestonePlatinumClaimed; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
