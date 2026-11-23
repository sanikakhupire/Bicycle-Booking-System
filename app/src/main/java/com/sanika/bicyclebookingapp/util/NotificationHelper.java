package com.sanika.bicyclebookingapp.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;

/**
 * Helper class that configures Android 8.0+ Notification Channels, builds deep-link
 * PendingIntents, and posts local system push notifications.
 */
public class NotificationHelper {

    /**
     * Initializes all standard notification channels. Call during application/activity startup.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        // 1. Bookings & Reservations Channel
        NotificationChannel bookingChannel = new NotificationChannel(
                AppNotification.CHANNEL_BOOKINGS,
                "Bookings & Reservations",
                NotificationManager.IMPORTANCE_HIGH
        );
        bookingChannel.setDescription("Confirmation, hold timers, and reservation reminders");
        bookingChannel.enableLights(true);
        bookingChannel.setLightColor(Color.BLUE);
        bookingChannel.enableVibration(true);
        manager.createNotificationChannel(bookingChannel);

        // 2. Rides & Trip Tracking Channel
        NotificationChannel rideChannel = new NotificationChannel(
                AppNotification.CHANNEL_RIDES,
                "Ride & Trip Tracking",
                NotificationManager.IMPORTANCE_HIGH
        );
        rideChannel.setDescription("Live trip alerts, ongoing ride reminders, and return confirmations");
        rideChannel.enableLights(true);
        rideChannel.setLightColor(Color.GREEN);
        rideChannel.enableVibration(true);
        manager.createNotificationChannel(rideChannel);

        // 3. Billing & Payments Channel
        NotificationChannel paymentChannel = new NotificationChannel(
                AppNotification.CHANNEL_PAYMENTS,
                "Billing, Payments & Invoices",
                NotificationManager.IMPORTANCE_HIGH
        );
        paymentChannel.setDescription("Invoices, UPI payment receipts, deposit refunds, and transaction alerts");
        paymentChannel.enableLights(true);
        paymentChannel.setLightColor(Color.YELLOW);
        manager.createNotificationChannel(paymentChannel);

        // 4. Station & Safety Alerts Channel
        NotificationChannel alertChannel = new NotificationChannel(
                AppNotification.CHANNEL_ALERTS,
                "Station & Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.setDescription("Bicycle grounding, mechanical defect reports, and station operational alerts");
        alertChannel.enableLights(true);
        alertChannel.setLightColor(Color.RED);
        alertChannel.enableVibration(true);
        manager.createNotificationChannel(alertChannel);

        // 5. Admin & Operations Channel
        NotificationChannel adminChannel = new NotificationChannel(
                AppNotification.CHANNEL_ADMIN,
                "Admin Fleet & Operations",
                NotificationManager.IMPORTANCE_HIGH
        );
        adminChannel.setDescription("Real-time manager alerts for new bookings, active rides, repairs, and complaints");
        adminChannel.enableLights(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);
        manager.createNotificationChannel(adminChannel);
    }

    /**
     * Posts a notification to the system notification tray with deep linking into MainActivity.
     */
    public static void showNotification(Context context, AppNotification appNotification) {
        if (context == null || appNotification == null) return;

        createNotificationChannels(context);

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        // Build Deep Linking Intent
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("targetType", appNotification.getTargetType());
        intent.putExtra("targetId", appNotification.getTargetId());
        intent.putExtra("notificationType", appNotification.getType());
        intent.putExtra("notificationId", appNotification.getNotificationId());

        int reqCode = (appNotification.getIdempotencyKey() != null
                ? appNotification.getIdempotencyKey().hashCode() : (int) System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                Math.abs(reqCode),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = appNotification.getChannelId() != null
                ? appNotification.getChannelId() : AppNotification.CHANNEL_BOOKINGS;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(appNotification.getTitle())
                .setContentText(appNotification.getMessage())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(appNotification.getMessage()))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        int notifId = Math.abs(reqCode % 100000) + 1000;
        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }
}
