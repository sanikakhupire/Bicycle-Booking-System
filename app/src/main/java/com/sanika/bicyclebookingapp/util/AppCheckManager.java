package com.sanika.bicyclebookingapp.util;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.sanika.bicyclebookingapp.BuildConfig;

/**
 * Firebase App Check Initializer.
 *
 * Protects backend Firestore, Cloud Functions, and Firebase Storage endpoints
 * from abuse, unauthorized scraping, bot attacks, and client spoofing:
 * - Uses Google Play Integrity API for certified production release builds.
 * - Uses DebugAppCheckProviderFactory for local debug and CI environments.
 */
public final class AppCheckManager {

    private static boolean initialized = false;

    private AppCheckManager() {}

    public static synchronized void init(Context context) {
        if (initialized) return;

        try {
            FirebaseApp.initializeApp(context);
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();

            if (BuildConfig.DEBUG) {
                // Debug environment: Use debug provider for testing and developer devices
                firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                );
                SecureLog.d("AppCheckManager", "Firebase App Check initialized with DebugAppCheckProviderFactory");
            } else {
                // Production environment: Use Play Integrity attestation
                firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                );
                SecureLog.i("AppCheckManager", "Firebase App Check initialized with PlayIntegrityAppCheckProviderFactory");
            }
            initialized = true;
        } catch (Exception e) {
            // Log warning but never crash the app if Play Integrity is unavailable (e.g. emulators without Play Store)
            SecureLog.w("AppCheckManager", "Firebase App Check initialization deferred: " + e.getMessage());
        }
    }
}
