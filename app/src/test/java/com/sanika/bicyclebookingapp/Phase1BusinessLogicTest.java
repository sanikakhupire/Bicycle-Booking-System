package com.sanika.bicyclebookingapp;

import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.util.ValidationUtil;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class Phase1BusinessLogicTest {

    @Test
    public void testIndianPhoneValidation() {
        // Valid 10-digit Indian numbers starting with 6, 7, 8, 9
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
        assertTrue(ValidationUtil.isValidPhone("8123456789"));
        assertTrue(ValidationUtil.isValidPhone("7012345678"));
        assertTrue(ValidationUtil.isValidPhone("6987654321"));

        // Valid with +91 or 0 prefix
        assertTrue(ValidationUtil.isValidPhone("+919876543210"));
        assertTrue(ValidationUtil.isValidPhone("+91 9876543210"));
        assertTrue(ValidationUtil.isValidPhone("+91-9876543210"));
        assertTrue(ValidationUtil.isValidPhone("09876543210"));

        // Invalid numbers
        assertFalse(ValidationUtil.isValidPhone("1234567890")); // starts with 1
        assertFalse(ValidationUtil.isValidPhone("5123456789")); // starts with 5
        assertFalse(ValidationUtil.isValidPhone("987654321"));  // only 9 digits
        assertFalse(ValidationUtil.isValidPhone("98765432100")); // 11 digits without 0
        assertFalse(ValidationUtil.isValidPhone(""));
        assertFalse(ValidationUtil.isValidPhone(null));
        assertFalse(ValidationUtil.isValidPhone("abcdefghij"));
    }

    @Test
    public void testIndianPhoneNormalization() {
        assertEquals("9876543210", ValidationUtil.normalizeIndianPhone("+919876543210"));
        assertEquals("9876543210", ValidationUtil.normalizeIndianPhone("+91 9876543210"));
        assertEquals("9876543210", ValidationUtil.normalizeIndianPhone("09876543210"));
        assertEquals("9876543210", ValidationUtil.normalizeIndianPhone("9876543210"));
    }

    @Test
    public void testBicycleStatusTransitionsAndCompatibility() {
        Bicycle bike = new Bicycle("B001", "ST01", "Standard");

        // Initial default state
        assertEquals(Bicycle.STATUS_AVAILABLE, bike.getStatus());
        assertTrue(bike.isAvailable());
        assertFalse(bike.isUnderMaintenance());

        // Status: RESERVED
        bike.setStatus(Bicycle.STATUS_RESERVED);
        assertEquals(Bicycle.STATUS_RESERVED, bike.getStatus());
        assertFalse(bike.isAvailable());
        assertFalse(bike.isUnderMaintenance());

        // Status: IN_USE
        bike.setStatus(Bicycle.STATUS_IN_USE);
        assertEquals(Bicycle.STATUS_IN_USE, bike.getStatus());
        assertFalse(bike.isAvailable());
        assertFalse(bike.isUnderMaintenance());

        // Status: MAINTENANCE
        bike.setStatus(Bicycle.STATUS_MAINTENANCE);
        assertEquals(Bicycle.STATUS_MAINTENANCE, bike.getStatus());
        assertFalse(bike.isAvailable());
        assertTrue(bike.isUnderMaintenance());

        // Status: DAMAGED
        bike.setStatus(Bicycle.STATUS_DAMAGED);
        assertEquals(Bicycle.STATUS_DAMAGED, bike.getStatus());
        assertFalse(bike.isAvailable());
        assertTrue(bike.isUnderMaintenance());

        // Status: INACTIVE
        bike.setStatus(Bicycle.STATUS_INACTIVE);
        assertEquals(Bicycle.STATUS_INACTIVE, bike.getStatus());
        assertFalse(bike.isAvailable());
        assertFalse(bike.isUnderMaintenance());

        // Back to AVAILABLE
        bike.setStatus(Bicycle.STATUS_AVAILABLE);
        assertTrue(bike.isAvailable());
        assertFalse(bike.isUnderMaintenance());

        // Legacy setAvailable(false) should transition
        bike.setAvailable(false);
        assertFalse(bike.isAvailable());
        assertEquals(Bicycle.STATUS_IN_USE, bike.getStatus());

        // Legacy setAvailable(true)
        bike.setAvailable(true);
        assertTrue(bike.isAvailable());
        assertEquals(Bicycle.STATUS_AVAILABLE, bike.getStatus());

        // Legacy setUnderMaintenance(true)
        bike.setUnderMaintenance(true);
        assertTrue(bike.isUnderMaintenance());
        assertFalse(bike.isAvailable());
        assertEquals(Bicycle.STATUS_MAINTENANCE, bike.getStatus());

        // Legacy setUnderMaintenance(false)
        bike.setUnderMaintenance(false);
        assertFalse(bike.isUnderMaintenance());
        assertTrue(bike.isAvailable());
        assertEquals(Bicycle.STATUS_AVAILABLE, bike.getStatus());
    }

    @Test
    public void testBookingStatusAndExpiration() {
        Booking booking = new Booking("user1", "bike1", "station1", "Main Station");

        // Initial state
        assertEquals(Booking.STATUS_PENDING, booking.getStatus());
        assertFalse(booking.isExpired());

        // Future expiration date
        booking.setExpiresAt(new Date(System.currentTimeMillis() + 600000)); // +10 mins
        assertFalse(booking.isExpired());

        // Past expiration date
        booking.setExpiresAt(new Date(System.currentTimeMillis() - 5000)); // 5 seconds ago
        assertTrue(booking.isExpired());

        // Status explicitly set to EXPIRED
        booking.setStatus(Booking.STATUS_EXPIRED);
        assertTrue(booking.isExpired());
    }

    @Test
    public void testValidationUtilHelpers() {
        assertTrue(ValidationUtil.isValidBicycleStatus("AVAILABLE"));
        assertTrue(ValidationUtil.isValidBicycleStatus("RESERVED"));
        assertTrue(ValidationUtil.isValidBicycleStatus("IN_USE"));
        assertTrue(ValidationUtil.isValidBicycleStatus("MAINTENANCE"));
        assertTrue(ValidationUtil.isValidBicycleStatus("DAMAGED"));
        assertTrue(ValidationUtil.isValidBicycleStatus("INACTIVE"));
        assertFalse(ValidationUtil.isValidBicycleStatus("UNKNOWN"));

        assertTrue(ValidationUtil.isValidBookingStatus("PENDING"));
        assertTrue(ValidationUtil.isValidBookingStatus("ACTIVE"));
        assertTrue(ValidationUtil.isValidBookingStatus("COMPLETED"));
        assertTrue(ValidationUtil.isValidBookingStatus("CANCELLED"));
        assertTrue(ValidationUtil.isValidBookingStatus("EXPIRED"));
        assertFalse(ValidationUtil.isValidBookingStatus("UNKNOWN"));

        assertTrue(ValidationUtil.isValidUserRole(User.ROLE_USER));
        assertTrue(ValidationUtil.isValidUserRole(User.ROLE_ADMIN));
        assertTrue(ValidationUtil.isValidUserRole(User.ROLE_HANDLER));
        assertFalse(ValidationUtil.isValidUserRole("SUPERADMIN"));
    }
}
