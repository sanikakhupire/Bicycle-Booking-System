package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a booking in Firestore collection "bookings".
 *
 * Status flow: PENDING → ACTIVE → COMPLETED / CANCELLED / EXPIRED
 */
public class Booking {

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_ACTIVE    = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED   = "EXPIRED";

    @DocumentId
    private String bookingId;
    private String userId;
    private String bicycleId;
    private String stationId;
    private String stationName;
    private String status;

    @ServerTimestamp
    private Date createdAt;
    private Date startTime;
    private Date endTime;
    private Date expiresAt;

    private long durationMinutes;
    private double fare;
    private double distanceKm;

    // Required empty constructor for Firestore
    public Booking() {}

    public Booking(String userId, String bicycleId, String stationId, String stationName) {
        this.userId = userId;
        this.bicycleId = bicycleId;
        this.stationId = stationId;
        this.stationName = stationName;
        this.status = STATUS_PENDING;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() {
        if (STATUS_EXPIRED.equalsIgnoreCase(status)) return true;
        return STATUS_PENDING.equalsIgnoreCase(status) && expiresAt != null && new Date().after(expiresAt);
    }

    public long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
}
