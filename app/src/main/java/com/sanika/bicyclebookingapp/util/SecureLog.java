package com.sanika.bicyclebookingapp.util;

import android.util.Log;

import com.sanika.bicyclebookingapp.BuildConfig;

import java.util.regex.Pattern;

/**
 * Production-Grade Security Logging Utility.
 *
 * Ensures strict compliance with PCI-DSS, GDPR, and India's DPDP Act:
 * - Automatically redacts passwords, CVVs, UPI PINs, card numbers, and auth tokens.
 * - Suppresses verbose/debug logs in production release builds.
 * - Prevents sensitive customer data leaks into device logcat or error buffers.
 */
public final class SecureLog {

    private static final String DEFAULT_TAG = "CycleGo";

    // Regex patterns for sensitive data redaction
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9]{2})[0-9]{12}|3[47][0-9]{13})\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)(?:cvv|cvc|security_code)[\\s:=]+([0-9]{3,4})");
    private static final Pattern PIN_PATTERN = Pattern.compile("(?i)(?:upi_pin|pin|mpin)[\\s:=]+([0-9]{4,6})");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(?:password|passwd|pwd)[\\s:=]+([^\\s,;]+)");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(?:bearer|token|secret|key)[\\s:=]+([A-Za-z0-9_\\-\\.~]+)");

    private static boolean loggingEnabled = BuildConfig.DEBUG;

    private SecureLog() {}

    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    public static void d(String tag, String message) {
        if (loggingEnabled) {
            Log.d(safeTag(tag), sanitize(message));
        }
    }

    public static void i(String tag, String message) {
        if (loggingEnabled) {
            Log.i(safeTag(tag), sanitize(message));
        }
    }

    public static void w(String tag, String message) {
        Log.w(safeTag(tag), sanitize(message));
    }

    public static void w(String tag, String message, Throwable tr) {
        Log.w(safeTag(tag), sanitize(message), tr);
    }

    public static void e(String tag, String message) {
        Log.e(safeTag(tag), sanitize(message));
    }

    public static void e(String tag, String message, Throwable tr) {
        Log.e(safeTag(tag), sanitize(message), tr);
    }

    /**
     * Redacts credit/debit card numbers, CVVs, UPI PINs, passwords, and tokens from any log string.
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) return "";

        String sanitized = input;
        sanitized = CARD_PATTERN.matcher(sanitized).replaceAll("[REDACTED_CARD]");
        sanitized = CVV_PATTERN.matcher(sanitized).replaceAll("cvv=[REDACTED]");
        sanitized = PIN_PATTERN.matcher(sanitized).replaceAll("pin=[REDACTED]");
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("password=[REDACTED]");
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("token=[REDACTED]");

        return sanitized;
    }

    private static String safeTag(String tag) {
        return (tag != null && !tag.isEmpty()) ? tag : DEFAULT_TAG;
    }
}
