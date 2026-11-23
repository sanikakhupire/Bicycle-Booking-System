package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.LoyaltyAccount;
import com.sanika.bicyclebookingapp.model.LoyaltyTransaction;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Repository orchestrating customer loyalty points, ride earnings, redemptions,
 * gamification milestones, and transactional ledgers.
 */
public class LoyaltyRepository {

    public static final String COLLECTION_LOYALTY = "loyalty_accounts";
    public static final String SUBCOLLECTION_TRANSACTIONS = "transactions";

    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public LoyaltyRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationRepository = new NotificationRepository(db);
    }

    public LoyaltyRepository(FirebaseFirestore db, NotificationRepository notificationRepository) {
        this.db = db;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Get or initialize customer loyalty account.
     */
    public LiveData<Resource<LoyaltyAccount>> getLoyaltyAccount(String userId, String userName) {
        MutableLiveData<Resource<LoyaltyAccount>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        DocumentReference accountRef = db.collection(COLLECTION_LOYALTY).document(userId);
        accountRef.addSnapshotListener((snap, error) -> {
            if (error != null) {
                result.setValue(Resource.error(error.getMessage(), null));
                return;
            }

            if (snap != null && snap.exists()) {
                LoyaltyAccount account = snap.toObject(LoyaltyAccount.class);
                if (account != null) {
                    account.setUserId(snap.getId());
                    result.setValue(Resource.success(account));
                    return;
                }
            }

            // Initialize new loyalty account if none exists
            String refCode = generateReferralCode(userName);
            LoyaltyAccount newAccount = new LoyaltyAccount(userId, refCode);
            newAccount.setUpdatedAt(new Date());

            accountRef.set(newAccount).addOnSuccessListener(v -> {
                result.setValue(Resource.success(newAccount));
            }).addOnFailureListener(e -> {
                result.setValue(Resource.error("Failed to initialize loyalty account: " + e.getMessage(), null));
            });
        });

        return result;
    }

    /**
     * Generates a unique user referral code like SANIKA482.
     */
    public static String generateReferralCode(String name) {
        String base = (name != null && name.trim().length() >= 3)
                ? name.trim().replaceAll("[^a-zA-Z]", "").toUpperCase(Locale.US)
                : "RIDER";
        if (base.length() > 6) base = base.substring(0, 6);
        int rand = 100 + new Random().nextInt(900);
        return base + rand;
    }

    /**
     * Stream loyalty points ledger history.
     */
    public LiveData<Resource<List<LoyaltyTransaction>>> getLoyaltyHistory(String userId) {
        MutableLiveData<Resource<List<LoyaltyTransaction>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_LOYALTY).document(userId)
                .collection(SUBCOLLECTION_TRANSACTIONS)
                .addSnapshotListener((snaps, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<LoyaltyTransaction> list = new ArrayList<>();
                    if (snaps != null) {
                        for (QueryDocumentSnapshot doc : snaps) {
                            LoyaltyTransaction t = doc.toObject(LoyaltyTransaction.class);
                            if (t != null) {
                                t.setTransactionId(doc.getId());
                                list.add(t);
                            }
                        }
                    }
                    list.sort((a, b) -> {
                        Date da = a.getTimestamp() != null ? a.getTimestamp() : new Date(0);
                        Date db = b.getTimestamp() != null ? b.getTimestamp() : new Date(0);
                        return db.compareTo(da);
                    });
                    result.setValue(Resource.success(list));
                });

        return result;
    }

    /**
     * Credit loyalty points upon completed ride.
     * Earns 1 point per ₹10 spent (e.g. ₹85 fare = 8 points).
     * Also checks and awards ride milestone bonuses.
     */
    public LiveData<Resource<Long>> creditRidePoints(String userId, String bookingId, double fareInr) {
        MutableLiveData<Resource<Long>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        long pointsToEarn = Math.max(1, (long) (fareInr / LoyaltyAccount.INR_SPENT_PER_EARNED_POINT));
        DocumentReference accountRef = db.collection(COLLECTION_LOYALTY).document(userId);

        accountRef.get().addOnSuccessListener(snap -> {
            LoyaltyAccount parsed = snap.exists() ? snap.toObject(LoyaltyAccount.class) : null;
            final LoyaltyAccount account = parsed != null ? parsed : new LoyaltyAccount(userId, generateReferralCode("RIDER"));

            account.setPointsBalance(account.getPointsBalance() + pointsToEarn);
            account.setLifetimePointsEarned(account.getLifetimePointsEarned() + pointsToEarn);
            int newTotalRides = account.getTotalCompletedRides() + 1;
            account.setTotalCompletedRides(newTotalRides);

            // Record ride earnings transaction
            LoyaltyTransaction rideTxn = new LoyaltyTransaction(
                    userId,
                    LoyaltyTransaction.TYPE_EARNED,
                    pointsToEarn,
                    String.format(Locale.ENGLISH, "Earned from Ride #%s (₹%.2f)", bookingId.substring(0, Math.min(8, bookingId.length())).toUpperCase(), fareInr),
                    bookingId
            );
            accountRef.collection(SUBCOLLECTION_TRANSACTIONS).add(rideTxn);

            // Check milestone rewards
            long milestoneBonus = evaluateAndAwardMilestones(account, accountRef);
            account.evaluateTier();

            final long totalEarned = pointsToEarn + milestoneBonus;

            accountRef.set(account).addOnSuccessListener(v -> {
                // Dispatch notification
                notificationRepository.dispatchNotification(null, new AppNotification(
                        userId,
                        AppNotification.TYPE_LOYALTY_POINTS_UPDATE,
                        AppNotification.CHANNEL_ALERTS,
                        "🌟 Loyalty Points Credited!",
                        String.format(Locale.ENGLISH, "You earned +%d points from your ride. Current Balance: %d pts (₹%.2f value)",
                                totalEarned, account.getPointsBalance(), account.getPointsValueInr()),
                        AppNotification.TARGET_LOYALTY,
                        userId
                ));
                result.setValue(Resource.success(totalEarned));
            }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
        }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    private long evaluateAndAwardMilestones(LoyaltyAccount account, DocumentReference accountRef) {
        int rides = account.getTotalCompletedRides();
        long totalBonus = 0;

        if (rides >= 5 && !account.isMilestoneBronzeClaimed()) {
            account.setMilestoneBronzeClaimed(true);
            account.setPointsBalance(account.getPointsBalance() + 50);
            account.setLifetimePointsEarned(account.getLifetimePointsEarned() + 50);
            totalBonus += 50;
            accountRef.collection(SUBCOLLECTION_TRANSACTIONS).add(new LoyaltyTransaction(
                    account.getUserId(), LoyaltyTransaction.TYPE_MILESTONE_BONUS, 50, "🏆 Bronze Rider Milestone (5 rides completed!)", null
            ));
        }

        if (rides >= 15 && !account.isMilestoneSilverClaimed()) {
            account.setMilestoneSilverClaimed(true);
            account.setPointsBalance(account.getPointsBalance() + 150);
            account.setLifetimePointsEarned(account.getLifetimePointsEarned() + 150);
            totalBonus += 150;
            accountRef.collection(SUBCOLLECTION_TRANSACTIONS).add(new LoyaltyTransaction(
                    account.getUserId(), LoyaltyTransaction.TYPE_MILESTONE_BONUS, 150, "🥈 Silver Cruiser Milestone (15 rides completed!)", null
            ));
        }

        if (rides >= 30 && !account.isMilestoneGoldClaimed()) {
            account.setMilestoneGoldClaimed(true);
            account.setPointsBalance(account.getPointsBalance() + 350);
            account.setLifetimePointsEarned(account.getLifetimePointsEarned() + 350);
            totalBonus += 350;
            accountRef.collection(SUBCOLLECTION_TRANSACTIONS).add(new LoyaltyTransaction(
                    account.getUserId(), LoyaltyTransaction.TYPE_MILESTONE_BONUS, 350, "🥇 Gold Commuter Milestone (30 rides completed!)", null
            ));
        }

        if (rides >= 50 && !account.isMilestonePlatinumClaimed()) {
            account.setMilestonePlatinumClaimed(true);
            account.setPointsBalance(account.getPointsBalance() + 1000);
            account.setLifetimePointsEarned(account.getLifetimePointsEarned() + 1000);
            totalBonus += 1000;
            accountRef.collection(SUBCOLLECTION_TRANSACTIONS).add(new LoyaltyTransaction(
                    account.getUserId(), LoyaltyTransaction.TYPE_MILESTONE_BONUS, 1000, "💎 Platinum Champion Milestone (50 rides completed!)", null
            ));
        }

        return totalBonus;
    }

    /**
     * Redeem points for ride discount.
     * Minimum 100 points required to redeem (₹10.00).
     */
    public LiveData<Resource<Long>> redeemPoints(String userId, long pointsToRedeem, String bookingId) {
        MutableLiveData<Resource<Long>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (pointsToRedeem < 100) {
            result.setValue(Resource.error("Minimum 100 points required to redeem (₹10.00 value)", null));
            return result;
        }

        DocumentReference accountRef = db.collection(COLLECTION_LOYALTY).document(userId);
        accountRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                result.setValue(Resource.error("Loyalty account not found", null));
                return;
            }

            LoyaltyAccount account = snap.toObject(LoyaltyAccount.class);
            if (account == null || account.getPointsBalance() < pointsToRedeem) {
                result.setValue(Resource.error("Insufficient points balance. Current balance: " + (account != null ? account.getPointsBalance() : 0), null));
                return;
            }

            account.setPointsBalance(account.getPointsBalance() - pointsToRedeem);
            account.setLifetimePointsRedeemed(account.getLifetimePointsRedeemed() + pointsToRedeem);

            long discountPaise = pointsToRedeem * LoyaltyAccount.PAISE_PER_POINT;

            LoyaltyTransaction redeemTxn = new LoyaltyTransaction(
                    userId,
                    LoyaltyTransaction.TYPE_REDEEMED,
                    -pointsToRedeem,
                    String.format(Locale.ENGLISH, "Redeemed for ₹%.2f Ride Discount", discountPaise / 100.0),
                    bookingId
            );
            accountRef.collection(SUBCOLLECTION_TRANSACTIONS).add(redeemTxn);
            accountRef.set(account).addOnSuccessListener(v -> result.setValue(Resource.success(discountPaise)))
                    .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
        }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Credit referral bonus points to a referrer.
     */
    public void creditReferralBonus(String referrerUserId, int points, String refereeName) {
        DocumentReference accountRef = db.collection(COLLECTION_LOYALTY).document(referrerUserId);
        accountRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) return;
            LoyaltyAccount account = snap.toObject(LoyaltyAccount.class);
            if (account == null) return;

            account.setPointsBalance(account.getPointsBalance() + points);
            account.setLifetimePointsEarned(account.getLifetimePointsEarned() + points);
            account.evaluateTier();

            LoyaltyTransaction txn = new LoyaltyTransaction(
                    referrerUserId,
                    LoyaltyTransaction.TYPE_REFERRAL_BONUS,
                    points,
                    "🎉 Referral Bonus: " + refereeName + " completed their first ride!",
                    null
            );
            accountRef.collection(SUBCOLLECTION_TRANSACTIONS).add(txn);
            accountRef.set(account);

            notificationRepository.dispatchNotification(null, new AppNotification(
                    referrerUserId,
                    AppNotification.TYPE_LOYALTY_POINTS_UPDATE,
                    AppNotification.CHANNEL_ALERTS,
                    "🎉 Referral Reward Credited!",
                    String.format(Locale.ENGLISH, "+%d Points added to your wallet! %s just completed their first ride.", points, refereeName),
                    AppNotification.TARGET_LOYALTY,
                    referrerUserId
            ));
        });
    }
}
