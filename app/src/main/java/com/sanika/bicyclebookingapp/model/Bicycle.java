package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Represents a bicycle in Firestore collection "bicycles".
 *
 * Supported statuses:
 * - AVAILABLE   : Docked at station, ready to be booked
 * - RESERVED    : Booking pending QR unlock / ride start
 * - IN_USE      : Actively being ridden in an ongoing trip
 * - MAINTENANCE : Under routine servicing or inspection
 * - DAMAGED     : Reported damaged / non-functional
 * - INACTIVE    : Decommissioned or disabled by admin
 */
public class Bicycle {

    public static final String STATUS_AVAILABLE   = "AVAILABLE";
    public static final String STATUS_RESERVED    = "RESERVED";
    public static final String STATUS_IN_USE      = "IN_USE";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String STATUS_DAMAGED     = "DAMAGED";
    public static final String STATUS_INACTIVE    = "INACTIVE";

    @DocumentId
    private String bicycleId;
    private String stationId;
    private String stationName;
    private boolean isAvailable;
    private boolean underMaintenance;
    private String status = STATUS_AVAILABLE;
    private String qrCode;       // QR code string used to unlock
    private String type;         // e.g., "Standard", "Electric", "Mountain"
    private String imageUrl;

    // Required empty constructor for Firestore
    public Bicycle() {}

    public Bicycle(String bicycleId, String stationId, String type) {
        this.bicycleId = bicycleId;
        this.stationId = stationId;
        this.type = type;
        this.status = STATUS_AVAILABLE;
        this.isAvailable = true;
        this.underMaintenance = false;
        this.qrCode = bicycleId; // QR code = bicycleId for simplicity
    }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getStatus() {
        if (status == null || status.trim().isEmpty()) {
            if (underMaintenance) return STATUS_MAINTENANCE;
            return isAvailable ? STATUS_AVAILABLE : STATUS_IN_USE;
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        // Keep boolean fields backward-compatible with legacy Firestore documents
        if (STATUS_AVAILABLE.equalsIgnoreCase(status)) {
            this.isAvailable = true;
            this.underMaintenance = false;
        } else if (STATUS_MAINTENANCE.equalsIgnoreCase(status) || STATUS_DAMAGED.equalsIgnoreCase(status)) {
            this.isAvailable = false;
            this.underMaintenance = true;
        } else {
            this.isAvailable = false;
            this.underMaintenance = false;
        }
    }

    public boolean isAvailable() {
        if (status != null && !status.isEmpty()) {
            return STATUS_AVAILABLE.equalsIgnoreCase(status);
        }
        return isAvailable && !underMaintenance;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
        if (available) {
            this.status = STATUS_AVAILABLE;
            this.underMaintenance = false;
        } else if (STATUS_AVAILABLE.equalsIgnoreCase(this.status)) {
            this.status = STATUS_IN_USE;
        }
    }

    public boolean isUnderMaintenance() {
        if (status != null && !status.isEmpty()) {
            return STATUS_MAINTENANCE.equalsIgnoreCase(status) || STATUS_DAMAGED.equalsIgnoreCase(status);
        }
        return underMaintenance;
    }

    public void setUnderMaintenance(boolean underMaintenance) {
        this.underMaintenance = underMaintenance;
        if (underMaintenance) {
            this.status = STATUS_MAINTENANCE;
            this.isAvailable = false;
        } else if (STATUS_MAINTENANCE.equalsIgnoreCase(this.status) || STATUS_DAMAGED.equalsIgnoreCase(this.status)) {
            this.status = STATUS_AVAILABLE;
            this.isAvailable = true;
        }
    }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
