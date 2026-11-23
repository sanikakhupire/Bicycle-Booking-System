package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a persistent notification in Firestore collection "notifications".
 * Powers both in-app notification center and push notification history across
 * customer and admin experiences.
 */
public class AppNotification implements Serializable {

    // Customer Notification Types
    public static final String TYPE_BOOKING_CONFIRMATION        = "BOOKING_CONFIRMATION";
    public static final String TYPE_BOOKING_REMINDER            = "BOOKING_REMINDER";
    public static final String TYPE_BOOKING_EXPIRATION_WARNING  = "BOOKING_EXPIRATION_WARNING";
    public static final String TYPE_RIDE_STARTED                = "RIDE_STARTED";
    public static final String TYPE_RIDE_DURATION_REMINDER      = "RIDE_DURATION_REMINDER";
    public static final String TYPE_RIDE_OVERDUE                = "RIDE_OVERDUE";
    public static final String TYPE_RIDE_COMPLETED              = "RIDE_COMPLETED";
    public static final String TYPE_PAYMENT_SUCCESS             = "PAYMENT_SUCCESS";
    public static final String TYPE_PAYMENT_FAILED              = "PAYMENT_FAILED";
    public static final String TYPE_REFUND_STATUS               = "REFUND_STATUS";
    public static final String TYPE_COUPON_PROMO                = "COUPON_PROMO";
    public static final String TYPE_STATION_UPDATE              = "STATION_UPDATE";
    public static final String TYPE_MAINTENANCE_ALERT           = "MAINTENANCE_ALERT";
    public static final String TYPE_SUPPORT_TICKET_UPDATE       = "SUPPORT_TICKET_UPDATE";

    // Admin Notification Types
    public static final String TYPE_ADMIN_NEW_BOOKING           = "ADMIN_NEW_BOOKING";
    public static final String TYPE_ADMIN_ACTIVE_RIDE           = "ADMIN_ACTIVE_RIDE";
    public static final String TYPE_ADMIN_OVERDUE_RIDE          = "ADMIN_OVERDUE_RIDE";
    public static final String TYPE_ADMIN_DAMAGE_REPORT         = "ADMIN_DAMAGE_REPORT";
    public static final String TYPE_ADMIN_MAINTENANCE_REQUEST   = "ADMIN_MAINTENANCE_REQUEST";
    public static final String TYPE_ADMIN_PAYMENT_FAILURE       = "ADMIN_PAYMENT_FAILURE";
    public static final String TYPE_ADMIN_CUSTOMER_COMPLAINT    = "ADMIN_CUSTOMER_COMPLAINT";
    public static final String TYPE_ADMIN_LOW_INVENTORY         = "ADMIN_LOW_INVENTORY";
    public static final String TYPE_ADMIN_SUSPICIOUS_ACTIVITY   = "ADMIN_SUSPICIOUS_ACTIVITY";

    // Notification Channel IDs
    public static final String CHANNEL_BOOKINGS = "channel_bookings";
    public static final String CHANNEL_RIDES    = "channel_rides";
    public static final String CHANNEL_PAYMENTS = "channel_payments";
    public static final String CHANNEL_ALERTS   = "channel_alerts";
    public static final String CHANNEL_ADMIN    = "channel_admin";

    // Target Entities for Deep Linking
    public static final String TARGET_BOOKING     = "BOOKING";
    public static final String TARGET_RIDE        = "RIDE";
    public static final String TARGET_PAYMENT     = "PAYMENT";
    public static final String TARGET_REFUND      = "REFUND";
    public static final String TARGET_MAINTENANCE = "MAINTENANCE";
    public static final String TARGET_SUPPORT     = "SUPPORT";
    public static final String TARGET_STATION     = "STATION";

    @DocumentId
    private String notificationId;
    private String userId; // Specific user UID, or "ADMIN" for broadcast to all managers
    private String type;
    private String channelId = CHANNEL_BOOKINGS;
    private String title;
    private String message;
    private String targetType;
    private String targetId;
    private String idempotencyKey; // Prevents duplicate delivery of same event
    private boolean read = false;
    private Date readAt;

    @ServerTimestamp
    private Date createdAt;

    public AppNotification() {}

    public AppNotification(String userId, String type, String channelId,
                           String title, String message, String targetType, String targetId) {
        this.userId = userId;
        this.type = type;
        this.channelId = channelId;
        this.title = title;
        this.message = message;
        this.targetType = targetType;
        this.targetId = targetId;
        this.read = false;
        this.createdAt = new Date();
        this.idempotencyKey = (userId != null ? userId : "admin") + "_" + type + "_"
                + (targetId != null ? targetId : System.currentTimeMillis());
    }

    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Date getReadAt() { return readAt; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
