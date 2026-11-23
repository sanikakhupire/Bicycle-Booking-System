package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String returnStationId;
    private String returnStationName;
    private String status;

    @ServerTimestamp
    private Date createdAt;
    private Date startTime;
    private Date endTime;
    private Date expiresAt;

    private long durationMinutes;
    private double fare;
    private double distanceKm;
    private double currentFare;
    private GeoPoint currentLocation;
    private GeoPoint dropOffLocation;
    private List<GeoPoint> routePoints = new ArrayList<>();
    private String endedBy;
    private String paymentStatus;
    private String paymentTxnId;

    // Phase 4 Pricing & Invoicing fields
    private String invoiceId;
    private String couponCode;
    private String bicycleType = "Standard";
    private double subtotal;
    private double discount;
    private double tax;
    private double depositAmount;
    private Map<String, Object> pricingSnapshot = new HashMap<>();

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

    public String getReturnStationId() { return returnStationId; }
    public void setReturnStationId(String returnStationId) { this.returnStationId = returnStationId; }

    public String getReturnStationName() { return returnStationName; }
    public void setReturnStationName(String returnStationName) { this.returnStationName = returnStationName; }

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

    public double getCurrentFare() { return currentFare; }
    public void setCurrentFare(double currentFare) { this.currentFare = currentFare; }

    public GeoPoint getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(GeoPoint currentLocation) { this.currentLocation = currentLocation; }

    public GeoPoint getDropOffLocation() { return dropOffLocation; }
    public void setDropOffLocation(GeoPoint dropOffLocation) { this.dropOffLocation = dropOffLocation; }

    public List<GeoPoint> getRoutePoints() { return routePoints; }
    public void setRoutePoints(List<GeoPoint> routePoints) {
        this.routePoints = routePoints != null ? routePoints : new ArrayList<>();
    }

    public String getEndedBy() { return endedBy; }
    public void setEndedBy(String endedBy) { this.endedBy = endedBy; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentTxnId() { return paymentTxnId; }
    public void setPaymentTxnId(String paymentTxnId) { this.paymentTxnId = paymentTxnId; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getBicycleType() { return bicycleType; }
    public void setBicycleType(String bicycleType) { this.bicycleType = bicycleType; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(double depositAmount) { this.depositAmount = depositAmount; }

    public Map<String, Object> getPricingSnapshot() { return pricingSnapshot; }
    public void setPricingSnapshot(Map<String, Object> pricingSnapshot) {
        this.pricingSnapshot = pricingSnapshot != null ? pricingSnapshot : new HashMap<>();
    }
}
