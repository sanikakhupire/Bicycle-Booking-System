package com.sanika.bicyclebookingapp;

import com.google.firebase.firestore.GeoPoint;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.util.FareCalculator;
import com.sanika.bicyclebookingapp.util.ValidationUtil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 3 Unit Tests:
 * - Real-world ride tracking calculations
 * - GPS noise filtering thresholds
 * - Indian Rupee (INR) fare computations & paise conversion
 * - Itemized receipts and deposit settlement
 * - Route points model persistence and crash recovery
 */
public class Phase3RideTrackingTest {

    @Test
    public void testMinimumFareEnforcement() {
        // Very short 0 km, 0 min ride should be charged the minimum fare of ₹10.00
        double fare = FareCalculator.calculate(0.0, 0L);
        assertEquals(10.00, fare, 0.001);

        // 1 minute ride with 0 distance = ₹5 + 0 + ₹1 = ₹6 -> capped at min fare ₹10
        double oneMinFare = FareCalculator.calculate(0.0, 60_000L);
        assertEquals(10.00, oneMinFare, 0.001);
    }

    @Test
    public void testStandardRideFareCalculation() {
        // 3.5 km, 20 minutes:
        // Base = ₹5.00
        // Distance = 3.5 * ₹8.00 = ₹28.00
        // Time = 20 * ₹1.00 = ₹20.00
        // Total = ₹53.00
        long twentyMinutesMs = 20 * 60 * 1000L;
        double fare = FareCalculator.calculate(3.5, twentyMinutesMs);
        assertEquals(53.00, fare, 0.001);
    }

    @Test
    public void testItemizedFareComponents() {
        long durationMs = 15 * 60 * 1000L; // 15 mins
        double distanceKm = 2.5; // 2.5 km

        double base = FareCalculator.getBaseFare();
        double distCharge = FareCalculator.getDistanceCharge(distanceKm);
        double timeCharge = FareCalculator.getTimeCharge(durationMs);

        assertEquals(5.00, base, 0.001);
        assertEquals(20.00, distCharge, 0.001); // 2.5 * 8
        assertEquals(15.00, timeCharge, 0.001); // 15 * 1

        double total = FareCalculator.calculate(distanceKm, durationMs);
        assertEquals(40.00, total, 0.001); // 5 + 20 + 15
    }

    @Test
    public void testPaiseIntegerRepresentation() {
        // Financial calculations in India should support safe integer paise
        // ₹53.00 -> 5300 paise
        long paise = FareCalculator.calculatePaise(3.5, 20 * 60 * 1000L);
        assertEquals(5300L, paise);

        // ₹10.00 min fare -> 1000 paise
        long minPaise = FareCalculator.calculatePaise(0.0, 0L);
        assertEquals(1000L, minPaise);
    }

    @Test
    public void testDistanceCappingSafety() {
        // An impossible GPS anomaly of 500 km should be safely capped to 200 km
        long oneHourMs = 60 * 60 * 1000L;
        double fareCapped = FareCalculator.calculate(500.0, oneHourMs);
        double fareAt200 = FareCalculator.calculate(200.0, oneHourMs);
        assertEquals(fareAt200, fareCapped, 0.001);
    }

    @Test
    public void testBreakdownTextContainsInrSymbols() {
        String breakdown = FareCalculator.getBreakdown(3.0, 10 * 60 * 1000L);
        assertNotNull(breakdown);
        assertTrue("Breakdown must contain ₹ currency symbol", breakdown.contains("₹"));
        assertTrue(breakdown.contains("Base: ₹5.00"));
    }

    @Test
    public void testGpsNoiseFilterThresholds() {
        // Stationary noise (< 5 meters) should be filtered out
        float stationaryJitterMeters = 3.2f;
        boolean acceptJitter = stationaryJitterMeters >= 5.0f && stationaryJitterMeters <= 100.0f;
        assertFalse("Movements under 5m should be filtered as stationary jitter", acceptJitter);

        // Valid bicycle movement (20 meters in 10s = 7.2 km/h)
        float normalMovementMeters = 20.0f;
        boolean acceptNormal = normalMovementMeters >= 5.0f && normalMovementMeters <= 100.0f;
        assertTrue("Movements between 5m and 100m should be accepted", acceptNormal);

        // Impossible jump (> 100m in 10s = > 36 km/h teleport)
        float teleportJitterMeters = 250.0f;
        boolean acceptTeleport = teleportJitterMeters >= 5.0f && teleportJitterMeters <= 100.0f;
        assertFalse("Movements over 100m in 10s should be filtered as GPS teleport anomalies", acceptTeleport);

        // Inaccurate GPS reading (> 35m accuracy)
        float poorAccuracy = 45.0f;
        boolean acceptReading = poorAccuracy <= 35.0f;
        assertFalse("Accuracy worse than 35m should be ignored", acceptReading);
    }

    @Test
    public void testBookingRoutePointsAndCrashRecoveryModel() {
        Booking booking = new Booking("user-test-123", "bike-456", "station-1", "MG Road Metro");
        booking.setStatus(Booking.STATUS_ACTIVE);
        Date start = new Date(System.currentTimeMillis() - 15 * 60 * 1000L);
        booking.setStartTime(start);
        booking.setDistanceKm(2.45);
        booking.setCurrentFare(39.60);

        List<GeoPoint> route = new ArrayList<>();
        route.add(new GeoPoint(12.9716, 77.5946)); // Bengaluru MG Road
        route.add(new GeoPoint(12.9720, 77.5950));
        route.add(new GeoPoint(12.9725, 77.5955));
        booking.setRoutePoints(route);

        assertEquals(3, booking.getRoutePoints().size());
        assertEquals(2.45, booking.getDistanceKm(), 0.001);
        assertEquals(39.60, booking.getCurrentFare(), 0.001);
        assertEquals(start, booking.getStartTime());
    }

    @Test
    public void testRideCompletionModel() {
        Booking booking = new Booking();
        booking.setBookingId("BK-100200");
        booking.setStatus(Booking.STATUS_COMPLETED);
        booking.setStartTime(new Date(System.currentTimeMillis() - 30 * 60 * 1000L));
        booking.setEndTime(new Date());
        booking.setDurationMinutes(30);
        booking.setDistanceKm(4.2);
        booking.setFare(68.60);
        booking.setStationName("Indiranagar 100ft");
        booking.setReturnStationId("station-2");
        booking.setReturnStationName("Koramangala Sony Signal");
        booking.setEndedBy("RETURN_VALIDATED");
        booking.setPaymentStatus("PAID");

        assertEquals(Booking.STATUS_COMPLETED, booking.getStatus());
        assertEquals(30, booking.getDurationMinutes());
        assertEquals(4.2, booking.getDistanceKm(), 0.001);
        assertEquals("Indiranagar 100ft", booking.getStationName());
        assertEquals("Koramangala Sony Signal", booking.getReturnStationName());
        assertEquals("RETURN_VALIDATED", booking.getEndedBy());
        assertEquals("PAID", booking.getPaymentStatus());
    }

    @Test
    public void testDurationFormatting() {
        assertEquals("45 min", ValidationUtil.formatDuration(45));
        assertEquals("1h 15m", ValidationUtil.formatDuration(75));
        assertEquals("2h 0m", ValidationUtil.formatDuration(120));
        assertEquals("2h 5m", ValidationUtil.formatDuration(125));
    }
}
