package com.sanika.bicyclebookingapp.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.NotificationPreferences;
import com.sanika.bicyclebookingapp.util.NotificationHelper;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository orchestrating persistent in-app notification center, delivery logging,
 * idempotency checks, and notification preference synchronization.
 */
public class NotificationRepository {

    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_USERS         = "users";

    private final FirebaseFirestore db;

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Dispatches a notification: checks idempotency to avoid duplicates, records in Firestore,
     * and shows local Android notification tray alert if targeting current user or admin.
     */
    public void dispatchNotification(Context context, AppNotification notification) {
        if (notification == null) return;

        String key = notification.getIdempotencyKey();
        if (key != null && !key.isEmpty()) {
            db.collection(COLLECTION_NOTIFICATIONS)
                    .whereEqualTo("idempotencyKey", key)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            // Already delivered, skip duplicate
                            return;
                        }
                        persistAndShowNotification(context, notification);
                    })
                    .addOnFailureListener(e -> persistAndShowNotification(context, notification));
        } else {
            persistAndShowNotification(context, notification);
        }
    }

    private void persistAndShowNotification(Context context, AppNotification notification) {
        DocumentReference ref = db.collection(COLLECTION_NOTIFICATIONS).document();
        notification.setNotificationId(ref.getId());
        notification.setCreatedAt(new Date());

        ref.set(notification).addOnSuccessListener(unused -> {
            String currentUid = FirebaseAuth.getInstance().getUid();
            boolean isTarget = notification.getUserId() != null
                    && (notification.getUserId().equals(currentUid) || "ADMIN".equalsIgnoreCase(notification.getUserId()));

            if (context != null && isTarget) {
                NotificationHelper.showNotification(context, notification);
            }
        });
    }

    /**
     * Live stream of notifications for a specific user (customer).
     */
    public LiveData<Resource<List<AppNotification>>> getCustomerNotifications(String userId) {
        MutableLiveData<Resource<List<AppNotification>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<AppNotification> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            AppNotification notif = doc.toObject(AppNotification.class);
                            if (notif != null) {
                                notif.setNotificationId(doc.getId());
                                list.add(notif);
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

    /**
     * Live stream of admin operational notifications.
     */
    public LiveData<Resource<List<AppNotification>>> getAdminNotifications() {
        MutableLiveData<Resource<List<AppNotification>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", "ADMIN")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<AppNotification> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            AppNotification notif = doc.toObject(AppNotification.class);
                            if (notif != null) {
                                notif.setNotificationId(doc.getId());
                                list.add(notif);
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

    /**
     * Marks a single notification as read.
     */
    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return;
        db.collection(COLLECTION_NOTIFICATIONS).document(notificationId)
                .update("read", true, "readAt", FieldValue.serverTimestamp());
    }

    /**
     * Marks all notifications for a given user as read.
     */
    public void markAllAsRead(String userId) {
        if (userId == null || userId.isEmpty()) return;
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null || snapshots.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        batch.update(doc.getReference(), "read", true, "readAt", new Date());
                    }
                    batch.commit();
                });
    }

    /**
     * Live stream of unread notification count.
     */
    public LiveData<Integer> getUnreadCount(String userId) {
        MutableLiveData<Integer> countData = new MutableLiveData<>(0);
        if (userId == null || userId.isEmpty()) return countData;

        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null) {
                        countData.setValue(snapshots.size());
                    }
                });

        return countData;
    }

    /**
     * Fetches user's notification preferences.
     */
    public LiveData<NotificationPreferences> getPreferences(String userId) {
        MutableLiveData<NotificationPreferences> prefsData = new MutableLiveData<>(new NotificationPreferences());
        if (userId == null || userId.isEmpty()) return prefsData;

        db.collection(COLLECTION_USERS).document(userId)
                .collection("settings").document("notification_preferences")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists()) {
                        NotificationPreferences p = snap.toObject(NotificationPreferences.class);
                        if (p != null) prefsData.setValue(p);
                    }
                });

        return prefsData;
    }

    /**
     * Saves user's notification preferences.
     */
    public void savePreferences(String userId, NotificationPreferences prefs) {
        if (userId == null || userId.isEmpty() || prefs == null) return;
        db.collection(COLLECTION_USERS).document(userId)
                .collection("settings").document("notification_preferences")
                .set(prefs);
    }
}
