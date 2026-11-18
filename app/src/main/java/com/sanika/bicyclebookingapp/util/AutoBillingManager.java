package com.sanika.bicyclebookingapp.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-Billing System for long rides.
 *
 * How it works:
 * 1. User pays a SECURITY DEPOSIT (₹100) when booking
 * 2. Every hour, the system checks if ride is still active
 * 3. If ride > 1 hour: sends notification "You've been riding for X hours, fare is ₹Y"
 * 4. Every hour after that: deducts ₹60 (hourly rate) from the deposit automatically
 * 5. If deposit runs out: sends urgent notification to return bicycle
 * 6. On return: final fare is calculated, remaining deposit is refunded
 *
 * This is fully legal — user pre-authorizes the deposit payment.
 */
public class AutoBillingManager {

    private static final String CHANNEL_ID = "billing_channel";

    // Security deposit amount
    public static final double SECURITY_DEPOSIT = 100.0;

    // Hourly rate for auto-deduction
    public static final double HOURLY_RATE = 60.0;

    // Threshold after which auto-billing starts (1 hour in ms)
    public static final long AUTO_BILLING_THRESHOLD_MS = 60 * 60 * 1000;

    private final Context context;
    private final FirebaseFirestore db;

    public AutoBillingManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        createNotificationChannel();
    }

    /**
     * Called every hour by RideTrackingService.
     * Checks ride duration and sends billing notifications.
     */
    public void checkAndBill(String bookingId, String userId,
                              long startTimeMs, double currentFare,
                              double depositPaid) {

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        long elapsedHours = elapsedMs / (60 * 60 * 1000);
        long elapsedMinutes = (elapsedMs / 60000) % 60;

        if (elapsedMs < AUTO_BILLING_THRESHOLD_MS) return; // Under 1 hour, no auto-billing

        // Calculate how much has been auto-deducted so far
        double autoDeducted = (elapsedHours - 1) * HOURLY_RATE; // First hour is normal
        double remainingDeposit = depositPaid - autoDeducted;

        if (remainingDeposit <= 0) {
            // Deposit exhausted — send urgent notification
            sendUrgentReturnNotification(bookingId, currentFare, elapsedHours);
        } else {
            // Deduct hourly rate from deposit
            deductFromDeposit(bookingId, userId, HOURLY_RATE, elapsedHours,
                    remainingDeposit, currentFare);
        }
    }

    /**
     * Deducts hourly rate from the user's deposit in Firestore.
     * Records each deduction as a partial payment.
     */
    private void deductFromDeposit(String bookingId, String userId,
                                    double amount, long elapsedHours,
                                    double remainingDeposit, double currentFare) {

        // Record auto-deduction in Firestore
        Map<String, Object> deduction = new HashMap<>();
        deduction.put("bookingId", bookingId);
        deduction.put("userId", userId);
        deduction.put("amount", amount);
        deduction.put("type", "AUTO_HOURLY_DEDUCTION");
        deduction.put("hour", elapsedHours);
        deduction.put("remainingDeposit", remainingDeposit - amount);
        deduction.put("timestamp", FieldValue.serverTimestamp());

        db.collection("payments").add(deduction);

        // Update booking with auto-deduction info
        db.collection("bookings").document(bookingId)
            .update("autoDeducted", FieldValue.increment(amount),
                    "remainingDeposit", remainingDeposit - amount);

        // Notify user
        sendHourlyBillingNotification(elapsedHours, currentFare,
                remainingDeposit - amount);
    }

    /**
     * Sends hourly billing notification to user.
     */
    private void sendHourlyBillingNotification(long hours, double fare,
                                                double remainingDeposit) {
        String title = "CycleGo — Hourly Billing";
        String body  = String.format(
            "You've been riding for %d hour%s.\n" +
            "Current fare: ₹%.2f\n" +
            "Deposit remaining: ₹%.2f\n" +
            "Return bicycle to stop billing.",
            hours, hours > 1 ? "s" : "", fare, remainingDeposit);

        sendNotification(title, body, 3001);
    }

    /**
     * Sends urgent notification when deposit is exhausted.
     */
    private void sendUrgentReturnNotification(String bookingId,
                                               double fare, long hours) {
        String title = "⚠️ CycleGo — Return Bicycle Now!";
        String body  = String.format(
            "Your deposit has been used up after %d hours.\n" +
            "Current fare: ₹%.2f\n" +
            "Please return the bicycle immediately to avoid additional charges.",
            hours, fare);

        sendNotification(title, body, 3002);

        // Flag booking as deposit exhausted
        db.collection("bookings").document(bookingId)
            .update("depositExhausted", true);
    }

    /**
     * Called when ride ends. Calculates final settlement.
     * Returns: amount user still owes (or refund if deposit > fare)
     */
    public static double calculateFinalSettlement(double totalFare,
                                                   double depositPaid,
                                                   double autoDeducted) {
        double totalPaid = depositPaid; // Deposit was paid upfront
        double balance = totalFare - totalPaid;
        // Positive = user owes more
        // Negative = user gets refund
        return balance;
    }

    private void sendNotification(String title, String body, int notifId) {
        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        manager.notify(notifId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Billing Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Hourly billing notifications during active ride");
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
        }
    }
}
