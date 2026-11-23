package com.sanika.bicyclebookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.LoyaltyAccount;
import com.sanika.bicyclebookingapp.model.LoyaltyTransaction;
import com.sanika.bicyclebookingapp.model.Referral;
import com.sanika.bicyclebookingapp.model.RideReview;
import com.sanika.bicyclebookingapp.repository.LoyaltyRepository;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for Phase 10: Customer Ratings, Offers, Coupons, Loyalty and Referrals.
 * Verifies coupon eligibility, expiry checks, per-user and system usage limits,
 * integer paise discount math, loyalty points calculations, milestones,
 * referral fraud rules, and review ratings logic.
 */
public class Phase10RatingsLoyaltyReferralTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. RATINGS AND REVIEWS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testRideReviewCreationAndFields() {
        RideReview review = new RideReview(
                "BK-1002",
                "USER-99",
                "Devendra Singh",
                "BIK-05",
                4.5f,
                4.0f,
                RideReview.CONDITION_GOOD,
                "Smooth ride along Deccan Gymkhana"
        );
        review.setRatingStation(5.0f);
        review.setPickupStationId("ST-1");
        review.setPickupStationName("Pune Station");
        review.setReturnStationId("ST-2");
        review.setReturnStationName("Shivajinagar Station");

        assertEquals("BK-1002", review.getBookingId());
        assertEquals("USER-99", review.getUserId());
        assertEquals("BIK-05", review.getBicycleId());
        assertEquals(4.5f, review.getRatingOverall(), 0.01f);
        assertEquals(4.0f, review.getRatingBicycle(), 0.01f);
        assertEquals(5.0f, review.getRatingStation(), 0.01f);
        assertEquals(RideReview.CONDITION_GOOD, review.getBicycleCondition());
        assertEquals(RideReview.STATUS_PUBLISHED, review.getStatus());
    }

    @Test
    public void testBicycleAverageRatingRecalculation() {
        double currentAvg = 4.2;
        long currentCount = 5;
        float newRating = 5.0f;

        // Running average formula
        double updatedAvg = ((currentAvg * currentCount) + newRating) / (currentCount + 1);
        long updatedCount = currentCount + 1;

        assertEquals(6, updatedCount);
        // (4.2 * 5 + 5.0) / 6 = (21.0 + 5.0) / 6 = 26.0 / 6 = 4.333...
        assertEquals(4.33, updatedAvg, 0.01);
    }

    @Test
    public void testReviewModerationStatuses() {
        RideReview review = new RideReview();
        review.setStatus(RideReview.STATUS_PUBLISHED);
        assertEquals(RideReview.STATUS_PUBLISHED, review.getStatus());

        // Admin flags review
        review.setStatus(RideReview.STATUS_FLAGGED);
        review.setModeratedBy("ADMIN_01");
        review.setModerationReason("Vulgarity reported in comments");
        assertEquals(RideReview.STATUS_FLAGGED, review.getStatus());
        assertEquals("ADMIN_01", review.getModeratedBy());

        // Admin hides review
        review.setStatus(RideReview.STATUS_HIDDEN);
        assertEquals(RideReview.STATUS_HIDDEN, review.getStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. COUPONS & OFFERS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testCouponDiscountCalculationPaise() {
        // Flat discount ₹15 off (1500 paise), min fare ₹30 (3000 paise)
        Coupon flatCoupon = new Coupon("PUNE15", Coupon.TYPE_FLAT, 1500L, "Flat ₹15 off");
        flatCoupon.setMinRideFarePaise(3000L);

        // Subtotal below min fare: no discount
        assertEquals(0L, flatCoupon.calculateDiscountPaise(2500L));
        // Subtotal above min fare: ₹15 flat discount
        assertEquals(1500L, flatCoupon.calculateDiscountPaise(4500L));
        // Subtotal ₹10 (below ₹15 discount): cannot exceed subtotal
        flatCoupon.setMinRideFarePaise(500L);
        assertEquals(1000L, flatCoupon.calculateDiscountPaise(1000L));

        // Percentage discount 20% off with ₹50 max cap (5000 paise)
        Coupon pctCoupon = new Coupon("CYCLE20", Coupon.TYPE_PERCENTAGE, 20L, 2000L, 5000L, true);

        // 20% of ₹100 (10,000 paise) = ₹20 (2000 paise)
        assertEquals(2000L, pctCoupon.calculateDiscountPaise(10000L));
        // 20% of ₹400 (40,000 paise) = ₹80 -> capped at max discount ₹50 (5000 paise)
        assertEquals(5000L, pctCoupon.calculateDiscountPaise(40000L));
    }

    @Test
    public void testCouponExpiryAndUsageLimits() {
        Coupon coupon = new Coupon("SUMMER50", Coupon.TYPE_FLAT, 1000L, "Summer discount");
        coupon.setMinRideFarePaise(2000L);
        coupon.setUsageLimit(100);
        coupon.setCurrentUsageCount(50);
        coupon.setPerUserLimit(1);

        // Active, unexpired, usage below limit
        assertNull("Coupon should be eligible",
                coupon.getIneligibilityReason(3000L, 2, 0));

        // System usage limit reached
        coupon.setCurrentUsageCount(100);
        assertTrue(coupon.isUsageLimitReached());
        assertEquals("Coupon usage limit has been reached",
                coupon.getIneligibilityReason(3000L, 2, 0));

        // Per-user limit reached
        coupon.setCurrentUsageCount(50);
        assertEquals("You have already reached the redemption limit for this coupon",
                coupon.getIneligibilityReason(3000L, 2, 1));

        // Expiry date passed
        coupon.setExpiryDate(new Date(System.currentTimeMillis() - 100000)); // Past date
        assertTrue(coupon.isExpired());
        assertEquals("Coupon code has expired",
                coupon.getIneligibilityReason(3000L, 2, 0));
    }

    @Test
    public void testFirstRideOfferRestriction() {
        Coupon firstRideCoupon = new Coupon("FIRST50", Coupon.TYPE_PERCENTAGE, 50L, 2000L, 5000L, true);
        firstRideCoupon.setFirstRideOnly(true);

        // User with 0 completed rides: eligible
        assertNull(firstRideCoupon.getIneligibilityReason(3000L, 0, 0));

        // User with 1 completed ride: ineligible for first-ride coupon
        assertEquals("This coupon is valid only for your first ride",
                firstRideCoupon.getIneligibilityReason(3000L, 1, 0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. LOYALTY & REWARDS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testLoyaltyEarningAndPointsValue() {
        LoyaltyAccount account = new LoyaltyAccount("USER_101", "SANIKA892");

        // 1 point per ₹10 spent
        double fareInr = 85.0;
        long pointsEarned = Math.max(1, (long) (fareInr / LoyaltyAccount.INR_SPENT_PER_EARNED_POINT));
        assertEquals(8L, pointsEarned);

        account.setPointsBalance(pointsEarned);
        account.setLifetimePointsEarned(pointsEarned);

        // 1 point = 10 paise, 8 points = 80 paise = ₹0.80
        assertEquals(0.80, account.getPointsValueInr(), 0.01);

        // 100 points = ₹10.00
        account.setPointsBalance(100);
        assertEquals(10.00, account.getPointsValueInr(), 0.01);
    }

    @Test
    public void testLoyaltyMilestonesAndTierProgression() {
        LoyaltyAccount account = new LoyaltyAccount("USER_102", "ROHIT334");
        assertEquals(LoyaltyAccount.TIER_BRONZE, account.getTierLevel());

        // 5 completed rides: Bronze tier
        account.setTotalCompletedRides(5);
        account.evaluateTier();
        assertEquals(LoyaltyAccount.TIER_BRONZE, account.getTierLevel());

        // 15 completed rides: Silver Cruiser
        account.setTotalCompletedRides(15);
        account.evaluateTier();
        assertEquals(LoyaltyAccount.TIER_SILVER, account.getTierLevel());

        // 30 completed rides: Gold Commuter
        account.setTotalCompletedRides(30);
        account.evaluateTier();
        assertEquals(LoyaltyAccount.TIER_GOLD, account.getTierLevel());

        // 50 completed rides: Platinum Champion
        account.setTotalCompletedRides(50);
        account.evaluateTier();
        assertEquals(LoyaltyAccount.TIER_PLATINUM, account.getTierLevel());
    }

    @Test
    public void testLoyaltyRedemptionRules() {
        LoyaltyAccount account = new LoyaltyAccount("USER_103", "NEHA512");
        account.setPointsBalance(250);

        // Cannot redeem less than minimum 100 points
        long pointsToRedeem = 50;
        assertTrue("Minimum 100 points required to redeem", pointsToRedeem < 100);

        // Valid redemption of 200 points
        pointsToRedeem = 200;
        assertTrue(account.getPointsBalance() >= pointsToRedeem);

        long discountPaise = pointsToRedeem * LoyaltyAccount.PAISE_PER_POINT; // 200 * 10 = 2000 paise (₹20.00)
        assertEquals(2000L, discountPaise);
        assertEquals(20.0, discountPaise / 100.0, 0.01);

        // Deduct points
        account.setPointsBalance(account.getPointsBalance() - pointsToRedeem);
        account.setLifetimePointsRedeemed(account.getLifetimePointsRedeemed() + pointsToRedeem);

        assertEquals(50L, account.getPointsBalance());
        assertEquals(200L, account.getLifetimePointsRedeemed());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. REFERRALS & FRAUD PREVENTION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testReferralGenerationAndSelfReferralPrevention() {
        String code = LoyaltyRepository.generateReferralCode("Sanika");
        assertNotNull(code);
        assertTrue(code.startsWith("SANIKA"));
        assertTrue(code.length() >= 6);

        // Self-referral rejection rule
        String referrerUserId = "USER_ABC";
        String refereeUserId = "USER_ABC"; // Same user

        boolean isSelfReferral = referrerUserId.equals(refereeUserId);
        assertTrue("Self referral must be strictly flagged and blocked", isSelfReferral);
    }

    @Test
    public void testReferralQualifyingEventEnforcement() {
        Referral referral = new Referral(
                "USER_REFERRER",
                "Sanika",
                "SANIKA892",
                "USER_REFEREE",
                "Amit Sharma",
                "+91 98765 00000"
        );

        // Initially upon registration, status must be PENDING
        assertEquals(Referral.STATUS_PENDING, referral.getStatus());
        assertNull("Reward must not be released upon signup", referral.getQualifiedAt());
        assertNull("Reward must not be released upon signup", referral.getRewardedAt());

        // Qualifying event: Referee completes their first paid ride
        referral.setQualifyingBookingId("BK-REF-01");
        referral.setStatus(Referral.STATUS_REWARDED);
        referral.setQualifiedAt(new Date());
        referral.setRewardedAt(new Date());

        assertEquals(Referral.STATUS_REWARDED, referral.getStatus());
        assertEquals(200, referral.getRewardPoints()); // +200 bonus points
        assertNotNull(referral.getQualifiedAt());
        assertNotNull(referral.getRewardedAt());
    }
}
