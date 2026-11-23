package com.sanika.bicyclebookingapp.model;

import java.io.Serializable;

/**
 * Stores customer and administrator notification preferences and alert settings.
 */
public class NotificationPreferences implements Serializable {

    private boolean pushEnabled = true;
    private boolean bookingAlerts = true;
    private boolean rideTrackingAlerts = true;
    private boolean billingAlerts = true;
    private boolean promotionalAlerts = true;
    private boolean maintenanceAlerts = true;
    private boolean supportAlerts = true;
    private boolean emailNotifications = false;
    private boolean smsNotifications = false;

    public NotificationPreferences() {}

    /**
     * Checks whether an alert of a specific category should be dispatched based on preferences.
     */
    public boolean isCategoryEnabled(String channelId) {
        if (!pushEnabled) return false;
        if (channelId == null) return true;

        switch (channelId) {
            case AppNotification.CHANNEL_BOOKINGS:
                return bookingAlerts;
            case AppNotification.CHANNEL_RIDES:
                return rideTrackingAlerts;
            case AppNotification.CHANNEL_PAYMENTS:
                return billingAlerts;
            case AppNotification.CHANNEL_ALERTS:
                return maintenanceAlerts;
            case AppNotification.CHANNEL_ADMIN:
                return true; // Admin critical operational alerts
            default:
                return true;
        }
    }

    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }

    public boolean isBookingAlerts() { return bookingAlerts; }
    public void setBookingAlerts(boolean bookingAlerts) { this.bookingAlerts = bookingAlerts; }

    public boolean isRideTrackingAlerts() { return rideTrackingAlerts; }
    public void setRideTrackingAlerts(boolean rideTrackingAlerts) { this.rideTrackingAlerts = rideTrackingAlerts; }

    public boolean isBillingAlerts() { return billingAlerts; }
    public void setBillingAlerts(boolean billingAlerts) { this.billingAlerts = billingAlerts; }

    public boolean isPromotionalAlerts() { return promotionalAlerts; }
    public void setPromotionalAlerts(boolean promotionalAlerts) { this.promotionalAlerts = promotionalAlerts; }

    public boolean isMaintenanceAlerts() { return maintenanceAlerts; }
    public void setMaintenanceAlerts(boolean maintenanceAlerts) { this.maintenanceAlerts = maintenanceAlerts; }

    public boolean isSupportAlerts() { return supportAlerts; }
    public void setSupportAlerts(boolean supportAlerts) { this.supportAlerts = supportAlerts; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public boolean isSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(boolean smsNotifications) { this.smsNotifications = smsNotifications; }
}
