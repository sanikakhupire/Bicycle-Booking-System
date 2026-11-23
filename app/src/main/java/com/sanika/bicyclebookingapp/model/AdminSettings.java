package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * System and Business Settings for CycleGo Operations.
 */
public class AdminSettings {

    public static final String SETTINGS_ID_DEFAULT = "default";

    @DocumentId
    private String settingsId = SETTINGS_ID_DEFAULT;

    private String businessName = "CycleGo India";
    private String operatingHours = "06:00 AM - 11:00 PM";
    private String emergencyPhone = "+91 98765 43210";
    private String supportEmail = "support@cyclego.in";

    private int geofenceRadiusMeters = 100;
    private int autoExpireHoldMinutes = 10;
    private String theftSensitivity = "MEDIUM";
    private boolean maintenanceAlertsEnabled = true;
    private boolean automatedRefundsEnabled = true;

    @ServerTimestamp
    private Date updatedAt;
    private String updatedBy;

    public AdminSettings() {}

    public static AdminSettings createDefaultSettings() {
        AdminSettings settings = new AdminSettings();
        settings.setSettingsId(SETTINGS_ID_DEFAULT);
        settings.setBusinessName("CycleGo India");
        settings.setOperatingHours("06:00 AM - 11:00 PM");
        settings.setEmergencyPhone("+91 98765 43210");
        settings.setSupportEmail("support@cyclego.in");
        settings.setGeofenceRadiusMeters(100);
        settings.setAutoExpireHoldMinutes(10);
        settings.setTheftSensitivity("MEDIUM");
        settings.setMaintenanceAlertsEnabled(true);
        settings.setAutomatedRefundsEnabled(true);
        return settings;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("settingsId", settingsId);
        map.put("businessName", businessName);
        map.put("operatingHours", operatingHours);
        map.put("emergencyPhone", emergencyPhone);
        map.put("supportEmail", supportEmail);
        map.put("geofenceRadiusMeters", geofenceRadiusMeters);
        map.put("autoExpireHoldMinutes", autoExpireHoldMinutes);
        map.put("theftSensitivity", theftSensitivity);
        map.put("maintenanceAlertsEnabled", maintenanceAlertsEnabled);
        map.put("automatedRefundsEnabled", automatedRefundsEnabled);
        map.put("updatedAt", updatedAt);
        map.put("updatedBy", updatedBy);
        return map;
    }

    public static AdminSettings fromMap(Map<String, Object> map) {
        if (map == null) return createDefaultSettings();
        AdminSettings settings = new AdminSettings();
        if (map.containsKey("settingsId")) settings.setSettingsId((String) map.get("settingsId"));
        if (map.containsKey("businessName")) settings.setBusinessName((String) map.get("businessName"));
        if (map.containsKey("operatingHours")) settings.setOperatingHours((String) map.get("operatingHours"));
        if (map.containsKey("emergencyPhone")) settings.setEmergencyPhone((String) map.get("emergencyPhone"));
        if (map.containsKey("supportEmail")) settings.setSupportEmail((String) map.get("supportEmail"));
        if (map.containsKey("geofenceRadiusMeters")) settings.setGeofenceRadiusMeters(((Number) map.get("geofenceRadiusMeters")).intValue());
        if (map.containsKey("autoExpireHoldMinutes")) settings.setAutoExpireHoldMinutes(((Number) map.get("autoExpireHoldMinutes")).intValue());
        if (map.containsKey("theftSensitivity")) settings.setTheftSensitivity((String) map.get("theftSensitivity"));
        if (map.containsKey("maintenanceAlertsEnabled")) settings.setMaintenanceAlertsEnabled((Boolean) map.get("maintenanceAlertsEnabled"));
        if (map.containsKey("automatedRefundsEnabled")) settings.setAutomatedRefundsEnabled((Boolean) map.get("automatedRefundsEnabled"));
        return settings;
    }

    public String getSettingsId() { return settingsId; }
    public void setSettingsId(String settingsId) { this.settingsId = settingsId; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public int getGeofenceRadiusMeters() { return geofenceRadiusMeters; }
    public void setGeofenceRadiusMeters(int geofenceRadiusMeters) { this.geofenceRadiusMeters = geofenceRadiusMeters; }

    public int getAutoExpireHoldMinutes() { return autoExpireHoldMinutes; }
    public void setAutoExpireHoldMinutes(int autoExpireHoldMinutes) { this.autoExpireHoldMinutes = autoExpireHoldMinutes; }

    public String getTheftSensitivity() { return theftSensitivity; }
    public void setTheftSensitivity(String theftSensitivity) { this.theftSensitivity = theftSensitivity; }

    public boolean isMaintenanceAlertsEnabled() { return maintenanceAlertsEnabled; }
    public void setMaintenanceAlertsEnabled(boolean maintenanceAlertsEnabled) { this.maintenanceAlertsEnabled = maintenanceAlertsEnabled; }

    public boolean isAutomatedRefundsEnabled() { return automatedRefundsEnabled; }
    public void setAutomatedRefundsEnabled(boolean automatedRefundsEnabled) { this.automatedRefundsEnabled = automatedRefundsEnabled; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
