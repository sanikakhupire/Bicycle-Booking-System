package com.sanika.bicyclebookingapp.util;

import android.util.Patterns;

/**
 * Utility class for input validation.
 */
public class ValidationUtil {

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

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.trim().length() >= 10;
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
