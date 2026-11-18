package com.sanika.bicyclebookingapp.util;

/**
 * Fare calculation engine.
 *
 * Formula:
 *   fare = BASE_FARE + (distanceKm * RATE_PER_KM) + (minutes * RATE_PER_MIN)
 *
 * Rates:
 *   Base fare  : ₹5   (charged on ride start)
 *   Per km     : ₹8   per kilometre
 *   Per minute : ₹1   per minute
 *   Minimum    : ₹10  (minimum charge for any ride)
 *
 * Example:
 *   3 km ride, 20 minutes = ₹5 + (3 × ₹8) + (20 × ₹1) = ₹49
 *   1 min test ride, 0 km = max(₹10, ₹5 + 0 + ₹1) = ₹10
 */
public class FareCalculator {

    public static final double BASE_FARE     = 5.0;
    public static final double RATE_PER_KM   = 8.0;
    public static final double RATE_PER_MIN  = 1.0;
    public static final double MINIMUM_FARE  = 10.0;

    // Maximum distance per update interval (sanity cap — 0.5 km per 10 seconds = 180 km/h)
    public static final double MAX_DISTANCE_PER_UPDATE_KM = 0.5;

    private FareCalculator() {}

    /**
     * Calculate fare based on distance and elapsed time.
     *
     * @param distanceKm  Total distance travelled in kilometres (GPS-tracked)
     * @param elapsedMs   Total elapsed time in milliseconds
     * @return            Fare in rupees, minimum ₹10
     */
    public static double calculate(double distanceKm, long elapsedMs) {
        // Cap distance to prevent GPS noise from inflating fare
        double safeDistance = Math.min(distanceKm, 200.0); // max 200 km per ride
        double minutes = elapsedMs / 60000.0;
        double fare = BASE_FARE
                + (safeDistance * RATE_PER_KM)
                + (minutes * RATE_PER_MIN);
        // Apply minimum fare
        fare = Math.max(fare, MINIMUM_FARE);
        // Round to 2 decimal places
        return Math.round(fare * 100.0) / 100.0;
    }

    public static String getBreakdown(double distanceKm, long elapsedMs) {
        double minutes = elapsedMs / 60000.0;
        double distCharge = distanceKm * RATE_PER_KM;
        double timeCharge = minutes * RATE_PER_MIN;
        double total = Math.max(MINIMUM_FARE, BASE_FARE + distCharge + timeCharge);
        return String.format(
            "Base: ₹%.2f  |  %.2f km × ₹%.0f = ₹%.2f  |  %.0f min × ₹%.0f = ₹%.2f  |  Total: ₹%.2f",
            BASE_FARE, distanceKm, RATE_PER_KM, distCharge,
            minutes, RATE_PER_MIN, timeCharge, total);
    }
}
