package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Represents a bicycle in Firestore collection "bicycles".
 */
public class Bicycle {

    @DocumentId
    private String bicycleId;
    private String stationId;
    private String stationName;
    private boolean isAvailable;
    private String qrCode;       // QR code string used to unlock
    private String type;         // e.g., "Standard", "Electric", "Mountain"
    private String imageUrl;

    // Required empty constructor for Firestore
    public Bicycle() {}

    public Bicycle(String bicycleId, String stationId, String type) {
        this.bicycleId = bicycleId;
        this.stationId = stationId;
        this.type = type;
        this.isAvailable = true;
        this.qrCode = bicycleId; // QR code = bicycleId for simplicity
    }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
