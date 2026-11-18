package com.sanika.bicyclebookingapp.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for date/time formatting.
 */
public class DateUtil {

    private DateUtil() {}

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public static String formatDate(Date date) {
        if (date == null) return "—";
        return DATE_FORMAT.format(date);
    }

    public static String formatDateTime(Date date) {
        if (date == null) return "—";
        return DATE_TIME_FORMAT.format(date);
    }

    public static String formatTime(Date date) {
        if (date == null) return "—";
        return TIME_FORMAT.format(date);
    }

    /** Returns elapsed time string like "5 min ago" or "2 hrs ago". */
    public static String timeAgo(Date date) {
        if (date == null) return "—";
        long diffMs = System.currentTimeMillis() - date.getTime();
        long seconds = diffMs / 1000;
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hr ago";
        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }
}
