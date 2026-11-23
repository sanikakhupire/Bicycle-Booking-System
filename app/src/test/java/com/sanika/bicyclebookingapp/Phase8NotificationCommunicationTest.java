package com.sanika.bicyclebookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.NotificationPreferences;

import org.junit.Test;

/**
 * Unit tests for Phase 8: Notifications and Communication.
 * Tests real event alert models, channel mappings, deep link target definitions,
 * user alert preferences logic, idempotency keys, and read/unread status mutations.
 */
public class Phase8NotificationCommunicationTest {

    @Test
    public void testCustomerNotificationCreationAndDefaults() {
        AppNotification notif = new AppNotification(
                "USER_123",
                AppNotification.TYPE_BOOKING_CONFIRMATION,
                AppNotification.CHANNEL_BOOKINGS,
                "Booking Confirmed!",
                "Bicycle at MG Road Station is reserved for 10 minutes.",
                AppNotification.TARGET_BOOKING,
                "BK_999"
        );

        assertEquals("USER_123", notif.getUserId());
        assertEquals(AppNotification.TYPE_BOOKING_CONFIRMATION, notif.getType());
        assertEquals(AppNotification.CHANNEL_BOOKINGS, notif.getChannelId());
        assertEquals(AppNotification.TARGET_BOOKING, notif.getTargetType());
        assertEquals("BK_999", notif.getTargetId());
        assertFalse("New notifications must default to unread", notif.isRead());
        assertNotNull("Creation timestamp must be populated", notif.getCreatedAt());
        assertNotNull("Idempotency key must be generated", notif.getIdempotencyKey());
        assertTrue("Idempotency key must contain targetId", notif.getIdempotencyKey().contains("BK_999"));
    }

    @Test
    public void testAdminOperationalAlertTypes() {
        AppNotification adminAlert = new AppNotification(
                "ADMIN",
                AppNotification.TYPE_ADMIN_DAMAGE_REPORT,
                AppNotification.CHANNEL_ADMIN,
                "⚠️ Bicycle Damage Grounding",
                "Bicycle BK-01 reported for Brakes Malfunction (CRITICAL)",
                AppNotification.TARGET_MAINTENANCE,
                "BK-01"
        );

        assertEquals("ADMIN", adminAlert.getUserId());
        assertEquals(AppNotification.TYPE_ADMIN_DAMAGE_REPORT, adminAlert.getType());
        assertEquals(AppNotification.CHANNEL_ADMIN, adminAlert.getChannelId());
        assertEquals(AppNotification.TARGET_MAINTENANCE, adminAlert.getTargetType());
        assertEquals("BK-01", adminAlert.getTargetId());
    }

    @Test
    public void testNotificationPreferencesCategoryFiltering() {
        NotificationPreferences prefs = new NotificationPreferences();
        assertTrue("Push should be enabled by default", prefs.isPushEnabled());
        assertTrue("Booking alerts should be enabled by default", prefs.isBookingAlerts());
        assertTrue("Ride tracking alerts should be enabled by default", prefs.isRideTrackingAlerts());
        assertTrue("Billing alerts should be enabled by default", prefs.isBillingAlerts());

        // Test category mapping
        assertTrue(prefs.isCategoryEnabled(AppNotification.CHANNEL_BOOKINGS));
        assertTrue(prefs.isCategoryEnabled(AppNotification.CHANNEL_RIDES));
        assertTrue(prefs.isCategoryEnabled(AppNotification.CHANNEL_PAYMENTS));
        assertTrue(prefs.isCategoryEnabled(AppNotification.CHANNEL_ALERTS));
        assertTrue(prefs.isCategoryEnabled(AppNotification.CHANNEL_ADMIN));

        // Disable billing alerts specifically
        prefs.setBillingAlerts(false);
        assertFalse("Disabled billing category must return false",
                prefs.isCategoryEnabled(AppNotification.CHANNEL_PAYMENTS));
        assertTrue("Other categories must remain enabled",
                prefs.isCategoryEnabled(AppNotification.CHANNEL_BOOKINGS));

        // Disable master push notifications
        prefs.setPushEnabled(false);
        assertFalse("Master toggle off must suppress all categories",
                prefs.isCategoryEnabled(AppNotification.CHANNEL_BOOKINGS));
        assertFalse("Master toggle off must suppress all categories",
                prefs.isCategoryEnabled(AppNotification.CHANNEL_RIDES));
    }

    @Test
    public void testReadUnreadStateMutation() {
        AppNotification notif = new AppNotification();
        notif.setNotificationId("NOTIF_777");
        notif.setRead(false);

        assertFalse(notif.isRead());

        // Mark as read
        notif.setRead(true);
        assertTrue(notif.isRead());
    }

    @Test
    public void testIdempotencyKeyUniquenessAndDeduplication() {
        String key1 = new AppNotification("USR_1", AppNotification.TYPE_PAYMENT_SUCCESS,
                AppNotification.CHANNEL_PAYMENTS, "Paid", "Paid", AppNotification.TARGET_PAYMENT, "TXN_123")
                .getIdempotencyKey();

        String key2 = new AppNotification("USR_1", AppNotification.TYPE_PAYMENT_SUCCESS,
                AppNotification.CHANNEL_PAYMENTS, "Paid", "Paid", AppNotification.TARGET_PAYMENT, "TXN_123")
                .getIdempotencyKey();

        String key3 = new AppNotification("USR_2", AppNotification.TYPE_PAYMENT_SUCCESS,
                AppNotification.CHANNEL_PAYMENTS, "Paid", "Paid", AppNotification.TARGET_PAYMENT, "TXN_123")
                .getIdempotencyKey();

        assertEquals("Same user, event, and entity must generate identical idempotency key to prevent duplication",
                key1, key2);
        assertFalse("Different user must generate different idempotency key", key1.equals(key3));
    }

    @Test
    public void testAllRequiredNotificationEventTypesDefined() {
        // Customer required types
        assertNotNull(AppNotification.TYPE_BOOKING_CONFIRMATION);
        assertNotNull(AppNotification.TYPE_BOOKING_REMINDER);
        assertNotNull(AppNotification.TYPE_BOOKING_EXPIRATION_WARNING);
        assertNotNull(AppNotification.TYPE_RIDE_STARTED);
        assertNotNull(AppNotification.TYPE_RIDE_DURATION_REMINDER);
        assertNotNull(AppNotification.TYPE_RIDE_OVERDUE);
        assertNotNull(AppNotification.TYPE_RIDE_COMPLETED);
        assertNotNull(AppNotification.TYPE_PAYMENT_SUCCESS);
        assertNotNull(AppNotification.TYPE_PAYMENT_FAILED);
        assertNotNull(AppNotification.TYPE_REFUND_STATUS);
        assertNotNull(AppNotification.TYPE_COUPON_PROMO);
        assertNotNull(AppNotification.TYPE_STATION_UPDATE);
        assertNotNull(AppNotification.TYPE_MAINTENANCE_ALERT);
        assertNotNull(AppNotification.TYPE_SUPPORT_TICKET_UPDATE);

        // Admin required types
        assertNotNull(AppNotification.TYPE_ADMIN_NEW_BOOKING);
        assertNotNull(AppNotification.TYPE_ADMIN_ACTIVE_RIDE);
        assertNotNull(AppNotification.TYPE_ADMIN_OVERDUE_RIDE);
        assertNotNull(AppNotification.TYPE_ADMIN_DAMAGE_REPORT);
        assertNotNull(AppNotification.TYPE_ADMIN_MAINTENANCE_REQUEST);
        assertNotNull(AppNotification.TYPE_ADMIN_PAYMENT_FAILURE);
        assertNotNull(AppNotification.TYPE_ADMIN_CUSTOMER_COMPLAINT);
        assertNotNull(AppNotification.TYPE_ADMIN_LOW_INVENTORY);
        assertNotNull(AppNotification.TYPE_ADMIN_SUSPICIOUS_ACTIVITY);
    }
}
