package com.sanika.bicyclebookingapp;

import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.util.ValidationUtil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class Phase2CustomerBookingTest {

    @Test
    public void testUniqueBookingIdFormatting() {
        String docId = "8b39f10a7c4e";
        String formattedRef = "BK-" + docId.substring(0, 6).toUpperCase();
        assertEquals("BK-8B39F1", formattedRef);
        assertTrue(formattedRef.startsWith("BK-"));
    }

    @Test
    public void testBicycleCategoryFiltering() {
        List<Bicycle> fleet = new ArrayList<>();
        fleet.add(new Bicycle("b1", "st1", "Standard"));
        fleet.add(new Bicycle("b2", "st1", "Electric"));
        fleet.add(new Bicycle("b3", "st1", "Mountain"));
        fleet.add(new Bicycle("b4", "st1", "Electric"));

        // Filter: All
        assertEquals(4, fleet.size());

        // Filter: Electric
        List<Bicycle> electricOnly = new ArrayList<>();
        for (Bicycle b : fleet) {
            if ("Electric".equalsIgnoreCase(b.getType())) {
                electricOnly.add(b);
            }
        }
        assertEquals(2, electricOnly.size());

        // Filter: Mountain
        List<Bicycle> mountainOnly = new ArrayList<>();
        for (Bicycle b : fleet) {
            if ("Mountain".equalsIgnoreCase(b.getType())) {
                mountainOnly.add(b);
            }
        }
        assertEquals(1, mountainOnly.size());
    }

    @Test
    public void testPreventBookingDamagedOrUnavailableBicycles() {
        Bicycle availableBike = new Bicycle("b1", "st1", "Standard");
        availableBike.setStatus(Bicycle.STATUS_AVAILABLE);
        assertTrue(availableBike.isAvailable());

        Bicycle damagedBike = new Bicycle("b2", "st1", "Standard");
        damagedBike.setStatus(Bicycle.STATUS_DAMAGED);
        assertFalse(damagedBike.isAvailable());
        assertTrue(damagedBike.isUnderMaintenance());

        Bicycle maintenanceBike = new Bicycle("b3", "st1", "Standard");
        maintenanceBike.setStatus(Bicycle.STATUS_MAINTENANCE);
        assertFalse(maintenanceBike.isAvailable());
        assertTrue(maintenanceBike.isUnderMaintenance());

        Bicycle reservedBike = new Bicycle("b4", "st1", "Standard");
        reservedBike.setStatus(Bicycle.STATUS_RESERVED);
        assertFalse(reservedBike.isAvailable());

        Bicycle inUseBike = new Bicycle("b5", "st1", "Standard");
        inUseBike.setStatus(Bicycle.STATUS_IN_USE);
        assertFalse(inUseBike.isAvailable());
    }

    @Test
    public void testReservationHoldAndCountdownMath() {
        long now = System.currentTimeMillis();
        long tenMinutesMs = 10 * 60 * 1000;
        Date expiresAt = new Date(now + tenMinutesMs);

        Booking booking = new Booking("user1", "b1", "st1", "Main Station");
        booking.setExpiresAt(expiresAt);

        // Before expiry
        assertFalse(booking.isExpired());

        // Remaining time calculation
        long remaining = booking.getExpiresAt().getTime() - now;
        assertEquals(tenMinutesMs, remaining);
        long minutes = remaining / 60000;
        long seconds = (remaining % 60000) / 1000;
        assertEquals(10, minutes);
        assertEquals(0, seconds);
    }

    @Test
    public void testInrCurrencyFormatting() {
        String fare5 = ValidationUtil.formatFare(5.0);
        assertEquals("₹5.00", fare5);

        String fare4950 = ValidationUtil.formatFare(49.50);
        assertEquals("₹49.50", fare4950);

        String fare1250 = ValidationUtil.formatFare(1250.0);
        assertTrue("₹1,250.00".equals(fare1250) || "₹1250.00".equals(fare1250));
    }
}
