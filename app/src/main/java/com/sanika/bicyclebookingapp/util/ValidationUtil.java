package com.sanika.bicyclebookingapp.util;

import android.util.Patterns;

import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.User;

import java.util.regex.Pattern;

/**
 * Utility class for input validation, phone formatting, and business entity validation.
 */
public class ValidationUtil {

    // Indian mobile number pattern: optional +91 or 0, followed by 10 digits starting with 6-9
    private static final Pattern INDIAN_PHONE_PATTERN =
            Pattern.compile("^(?:\\+91[\\-\\s]?|0)?[6-9]\\d{9}$");

    private ValidationUtil() {}

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Validates whether the given string is a valid Indian mobile number.
     * Accepts:
     * - 10 digits starting with 6-9 (e.g. 9876543210)
     * - Prefixed with +91 (e.g. +919876543210 or +91-9876543210)
     * - Prefixed with 0 (e.g. 09876543210)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String trimmed = phone.trim().replaceAll("\\s+", "");
        return INDIAN_PHONE_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Normalizes an Indian phone number to 10 standard digits.
     */
    public static String normalizeIndianPhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");
        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }

    public static boolean isValidUserRole(String role) {
        if (role == null) return false;
        return User.ROLE_USER.equalsIgnoreCase(role)
                || User.ROLE_ADMIN.equalsIgnoreCase(role)
                || User.ROLE_HANDLER.equalsIgnoreCase(role);
    }

    public static boolean isValidBicycleStatus(String status) {
        if (status == null) return false;
        return Bicycle.STATUS_AVAILABLE.equalsIgnoreCase(status)
                || Bicycle.STATUS_RESERVED.equalsIgnoreCase(status)
                || Bicycle.STATUS_IN_USE.equalsIgnoreCase(status)
                || Bicycle.STATUS_MAINTENANCE.equalsIgnoreCase(status)
                || Bicycle.STATUS_DAMAGED.equalsIgnoreCase(status)
                || Bicycle.STATUS_INACTIVE.equalsIgnoreCase(status);
    }

    public static boolean isValidBookingStatus(String status) {
        if (status == null) return false;
        return Booking.STATUS_PENDING.equalsIgnoreCase(status)
                || Booking.STATUS_ACTIVE.equalsIgnoreCase(status)
                || Booking.STATUS_COMPLETED.equalsIgnoreCase(status)
                || Booking.STATUS_CANCELLED.equalsIgnoreCase(status)
                || Booking.STATUS_EXPIRED.equalsIgnoreCase(status);
    }

    public static String formatFare(double fare) {
        return String.format("₹%.2f", fare);
    }

    public static String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long mins  = minutes % 60;
        return hours + "h " + mins + "m";
    }
}
