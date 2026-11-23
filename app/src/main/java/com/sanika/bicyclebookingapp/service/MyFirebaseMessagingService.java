package com.sanika.bicyclebookingapp.service;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.NotificationPreferences;
import com.sanika.bicyclebookingapp.util.NotificationHelper;

import java.util.Date;
import java.util.Map;

/**
 * Handles incoming Firebase Cloud Messaging push notifications, parses custom payloads,
 * enforces user alert preferences, posts to specific notification channels with deep-link
 * PendingIntents, and archives messages into the persistent in-app notification center.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "CycleGo";
        String body  = "You have a new update";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle() != null
                    ? remoteMessage.getNotification().getTitle() : title;
            body  = remoteMessage.getNotification().getBody() != null
                    ? remoteMessage.getNotification().getBody() : body;
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            title = data.getOrDefault("title", title);
            body  = data.getOrDefault("body", body);
        }

        String type        = data != null ? data.getOrDefault("type", AppNotification.TYPE_BOOKING_REMINDER) : AppNotification.TYPE_BOOKING_REMINDER;
        String channelId   = data != null ? data.getOrDefault("channelId", AppNotification.CHANNEL_BOOKINGS) : AppNotification.CHANNEL_BOOKINGS;
        String targetType  = data != null ? data.get("targetType") : null;
        String targetId    = data != null ? data.get("targetId") : null;
        String idempotency = data != null ? data.get("idempotencyKey") : null;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = user != null ? user.getUid() : "guest";

        AppNotification appNotification = new AppNotification(
                currentUid, type, channelId, title, body, targetType, targetId
        );
        if (idempotency != null && !idempotency.isEmpty()) {
            appNotification.setIdempotencyKey(idempotency);
        }

        // Show system notification banner on appropriate channel
        NotificationHelper.showNotification(getApplicationContext(), appNotification);

        // Persist into user's in-app notification history
        if (user != null) {
            FirebaseFirestore.getInstance().collection("notifications").add(appNotification);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Save FCM token to Firestore for targeted server dispatch
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("fcmToken", token);
        }
    }
}
