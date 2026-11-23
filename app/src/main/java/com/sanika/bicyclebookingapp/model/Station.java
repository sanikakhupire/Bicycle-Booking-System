package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;

/**
 * Represents a bicycle station in Firestore collection "stations".
 */
public class Station {

    @DocumentId
    private String stationId;
    private String name;
    private String address;
    private GeoPoint location; // Firestore GeoPoint (latitude, longitude)
    private int totalBicycles;
    private int availableBicycles;
    private double averageRating = 5.0;
    private int totalRatings = 0;

    // Required empty constructor for Firestore
    public Station() {}

    public Station(String stationId, String name, String address, double latitude, double longitude, int totalBicycles) {
        this.stationId = stationId;
        this.name = name;
        this.address = address;
        this.location = new GeoPoint(latitude, longitude);
        this.totalBicycles = totalBicycles;
        this.availableBicycles = totalBicycles;
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    public int getTotalBicycles() { return totalBicycles; }
    public void setTotalBicycles(int totalBicycles) { this.totalBicycles = totalBicycles; }

    public int getTotalCapacity() { return totalBicycles; }
    public void setTotalCapacity(int totalCapacity) { this.totalBicycles = totalCapacity; }

    public int getAvailableBicycles() { return availableBicycles; }
    public void setAvailableBicycles(int availableBicycles) { this.availableBicycles = availableBicycles; }

    // Helper methods
    public double getLatitude() {
        return location != null ? location.getLatitude() : 0.0;
    }

    public double getLongitude() {
        return location != null ? location.getLongitude() : 0.0;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }
}
