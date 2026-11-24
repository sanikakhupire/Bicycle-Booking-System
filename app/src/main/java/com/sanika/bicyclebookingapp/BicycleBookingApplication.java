package com.sanika.bicyclebookingapp;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.sanika.bicyclebookingapp.util.AppCheckManager;
import com.sanika.bicyclebookingapp.util.AppCrashHandler;
import com.sanika.bicyclebookingapp.util.NetworkMonitor;
import com.sanika.bicyclebookingapp.util.NotificationHelper;
import com.sanika.bicyclebookingapp.util.SecureLog;

/**
 * Enterprise Application Entry Point for CycleGo India.
 *
 * Bootstraps:
 * 1. Global Uncaught Crash Interceptor (AppCrashHandler)
 * 2. Sensitive Data Redaction & Production Logging Policy (SecureLog)
 * 3. Firebase App Check Attestation (Play Integrity / Debug)
 * 4. High-Performance Firestore Offline Persistence & Caching
 * 5. Real-Time Network Quality & Offline State Monitoring
 * 6. System Notification Channels & Badging
 */
public class BicycleBookingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Install Crash Interceptor first before any subsystem throws
        AppCrashHandler.install(this);

        // 2. Initialize Secure Logging Policy
        SecureLog.setLoggingEnabled(BuildConfig.ENABLE_SECURE_LOGS);
        SecureLog.i("Application", "Initializing CycleGo [Env=" + BuildConfig.ENVIRONMENT + "]");

        // 3. Initialize Firebase & App Check
        FirebaseApp.initializeApp(this);
        AppCheckManager.init(this);

        // 4. Configure Firestore Offline Persistence
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            SecureLog.d("Application", "Firestore unlimited offline cache enabled.");
        } catch (Exception e) {
            SecureLog.w("Application", "Firestore settings notice: " + e.getMessage());
        }

        // 5. Initialize Network Connectivity Engine
        NetworkMonitor.getInstance(this);

        // 6. Setup Notification Channels
        NotificationHelper.createNotificationChannels(this);

        SecureLog.i("Application", "CycleGo production infrastructure ready.");
    }
}
