package com.sanika.bicyclebookingapp.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Currency utility for Indian Rupee (INR / ₹) calculations and formatting.
 *
 * Enforces:
 * - Integer paise arithmetic to eliminate floating-point rounding errors.
 * - Indian number system comma grouping (e.g. ₹1,250.00, ₹10,00,000.00).
 * - Standard currency symbol: ₹
 * - Standard currency code: INR
 */
public final class CurrencyUtil {

    public static final String CURRENCY_CODE = "INR";
    public static final String CURRENCY_SYMBOL = "₹";

    private CurrencyUtil() {}

    /**
     * Converts a rupee decimal amount to integer paise safely.
     * ₹10.50 -> 1050 paise
     * ₹1,250.00 -> 125000 paise
     */
    public static long toPaise(double rupees) {
        return Math.round(rupees * 100.0);
    }

    public static long rupeesToPaise(double rupees) {
        return toPaise(rupees);
    }

    /**
     * Converts integer paise to rupee decimal.
     * 1050 paise -> 10.50
     */
    public static double toRupees(long paise) {
        return paise / 100.0;
    }

    public static double paiseToRupees(long paise) {
        return toRupees(paise);
    }

    /**
     * Formats integer paise into Indian currency string: ₹1,250.00
     */
    public static String formatPaise(long paise) {
        return formatInr(toRupees(paise));
    }

    public static String formatInrFromPaise(long paise) {
        return formatPaise(paise);
    }

    /**
     * Formats rupee decimal amount with Indian comma grouping and ₹ symbol.
     * Examples:
     *   10.0 -> "₹10.00"
     *   1250.0 -> "₹1,250.00"
     *   100000.0 -> "₹1,00,000.00"
     */
    public static String formatInr(double rupees) {
        boolean isNegative = rupees < 0;
        double absVal = Math.abs(rupees);

        long paiseTotal = Math.round(absVal * 100.0);
        long wholeRupees = paiseTotal / 100;
        long paisePart = paiseTotal % 100;

        String formattedWhole = formatIndianNumberGrouping(wholeRupees);
        String result = String.format(Locale.US, "%s%s.%02d", CURRENCY_SYMBOL, formattedWhole, paisePart);

        return isNegative ? "-" + result : result;
    }

    /**
     * Formats a whole number using Indian numbering system comma grouping:
     * The last 3 digits are grouped, then every 2 digits preceding.
     * Example:
     *   500 -> "500"
     *   1250 -> "1,250"
     *   125000 -> "1,25,000"
     *   1000000 -> "10,00,000"
     */
    public static String formatIndianNumberGrouping(long amount) {
        if (amount == 0) return "0";

        String numStr = String.valueOf(amount);
        int len = numStr.length();
        if (len <= 3) {
            return numStr;
        }

        StringBuilder sb = new StringBuilder();
        // Last 3 digits
        String lastThree = numStr.substring(len - 3);
        String remaining = numStr.substring(0, len - 3);

        sb.append(lastThree);

        // Group preceding digits in twos
        while (remaining.length() > 2) {
            sb.insert(0, ",");
            sb.insert(0, remaining.substring(remaining.length() - 2));
            remaining = remaining.substring(0, remaining.length() - 2);
        }

        if (remaining.length() > 0) {
            sb.insert(0, ",");
            sb.insert(0, remaining);
        }

        return sb.toString();
    }
}
