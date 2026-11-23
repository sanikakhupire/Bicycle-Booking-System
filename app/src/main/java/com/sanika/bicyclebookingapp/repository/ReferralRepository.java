package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.model.LoyaltyAccount;
import com.sanika.bicyclebookingapp.model.Referral;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository orchestrating referral registrations, fraud prevention checks,
 * and qualifying ride completion reward triggers.
 */
public class ReferralRepository {

    public static final String COLLECTION_REFERRALS = "referrals";
    public static final String COLLECTION_LOYALTY   = "loyalty_accounts";

    private final FirebaseFirestore db;
    private final LoyaltyRepository loyaltyRepository;

    public ReferralRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.loyaltyRepository = new LoyaltyRepository();
    }

    public ReferralRepository(FirebaseFirestore db, LoyaltyRepository loyaltyRepository) {
        this.db = db;
        this.loyaltyRepository = loyaltyRepository;
    }

    /**
     * Referee enters an invitation referral code upon signup or first ride.
     * Validates:
     * 1. Referrer code exists.
     * 2. Self-referral is strictly forbidden (referee != referrer).
     * 3. Referee has not already been referred.
     */
    public LiveData<Resource<String>> registerReferral(String referralCode, String refereeUserId, String refereeName, String refereePhone) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (referralCode == null || referralCode.trim().isEmpty()) {
            result.setValue(Resource.error("Please enter a valid referral code", null));
            return result;
        }

        String cleanCode = referralCode.trim().toUpperCase();

        // 1. Verify code exists and find referrer
        db.collection(COLLECTION_LOYALTY)
                .whereEqualTo("referralCode", cleanCode)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        result.setValue(Resource.error("Invalid or unrecognized referral code", null));
                        return;
                    }

                    LoyaltyAccount referrerAccount = snaps.getDocuments().get(0).toObject(LoyaltyAccount.class);
                    if (referrerAccount == null) {
                        result.setValue(Resource.error("Referrer account not found", null));
                        return;
                    }

                    String referrerUserId = referrerAccount.getUserId();

                    // 2. Strict anti-fraud: prevent self-referral
                    if (refereeUserId.equals(referrerUserId)) {
                        result.setValue(Resource.error("You cannot refer yourself.", null));
                        return;
                    }

                    // 3. Prevent duplicate referral registration for referee
                    db.collection(COLLECTION_REFERRALS)
                            .whereEqualTo("refereeUserId", refereeUserId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(refereeSnaps -> {
                                if (!refereeSnaps.isEmpty()) {
                                    result.setValue(Resource.error("You have already registered an invitation referral code.", null));
                                    return;
                                }

                                // Create PENDING referral record
                                DocumentReference docRef = db.collection(COLLECTION_REFERRALS).document();
                                Referral ref = new Referral(
                                        referrerUserId,
                                        "Friend / Fellow Rider",
                                        cleanCode,
                                        refereeUserId,
                                        refereeName,
                                        refereePhone
                                );
                                ref.setReferralId(docRef.getId());
                                ref.setCreatedAt(new Date());

                                docRef.set(ref).addOnSuccessListener(v -> {
                                    result.setValue(Resource.success("Referral code applied! Complete your first ride to unlock rewards."));
                                }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
                            })
                            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error("Failed to check code: " + e.getMessage(), null)));

        return result;
    }

    /**
     * Qualifying Event Trigger:
     * When a referee completes their first paid ride, the referral transitions to QUALIFIED & REWARDED.
     * The referrer receives their bonus loyalty points (+200 pts) immediately.
     */
    public void onRefereeRideCompleted(String refereeUserId, String bookingId) {
        db.collection(COLLECTION_REFERRALS)
                .whereEqualTo("refereeUserId", refereeUserId)
                .whereEqualTo("status", Referral.STATUS_PENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) return;

                    Referral referral = snaps.getDocuments().get(0).toObject(Referral.class);
                    if (referral != null) {
                        referral.setReferralId(snaps.getDocuments().get(0).getId());
                        referral.setStatus(Referral.STATUS_REWARDED);
                        referral.setQualifyingBookingId(bookingId);
                        referral.setQualifiedAt(new Date());
                        referral.setRewardedAt(new Date());

                        // Update referral record
                        db.collection(COLLECTION_REFERRALS).document(referral.getReferralId())
                                .set(referral)
                                .addOnSuccessListener(v -> {
                                    // Credit bonus to referrer
                                    loyaltyRepository.creditReferralBonus(
                                            referral.getReferrerUserId(),
                                            referral.getRewardPoints(),
                                            referral.getRefereeName() != null ? referral.getRefereeName() : "Your friend"
                                    );
                                });
                    }
                });
    }

    /**
     * Get list of invited friends and their qualification status for this user.
     */
    public LiveData<Resource<List<Referral>>> getCustomerReferrals(String referrerUserId) {
        MutableLiveData<Resource<List<Referral>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_REFERRALS)
                .whereEqualTo("referrerUserId", referrerUserId)
                .addSnapshotListener((snaps, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<Referral> list = new ArrayList<>();
                    if (snaps != null) {
                        for (QueryDocumentSnapshot doc : snaps) {
                            Referral r = doc.toObject(Referral.class);
                            if (r != null) {
                                r.setReferralId(doc.getId());
                                list.add(r);
                            }
                        }
                    }
                    list.sort((a, b) -> {
                        Date da = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                        Date db = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                        return db.compareTo(da);
                    });
                    result.setValue(Resource.success(list));
                });

        return result;
    }
}
