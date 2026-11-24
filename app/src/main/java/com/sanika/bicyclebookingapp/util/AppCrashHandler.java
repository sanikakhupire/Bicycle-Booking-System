package com.sanika.bicyclebookingapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Uncaught Exception & Crash Recovery Handler.
 * Traps unexpected application crashes, persists sanitized diagnostics,
 * records crash telemetry for production monitoring, and prevents crash loops.
 */
public class AppCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String PREF_NAME = "cyclego_crash_diagnostics";
    private static final String KEY_LAST_CRASH_TIME = "last_crash_time";
    private static final String KEY_LAST_CRASH_INFO = "last_crash_info";

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private AppCrashHandler(Context context, Thread.UncaughtExceptionHandler defaultHandler) {
        this.context = context.getApplicationContext();
        this.defaultHandler = defaultHandler;
    }

    public static void install(Context context) {
        Thread.UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(currentHandler instanceof AppCrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new AppCrashHandler(context, currentHandler));
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            recordCrash(thread, throwable);
        } catch (Exception e) {
            SecureLog.e("AppCrashHandler", "Failed to record crash: " + e.getMessage());
        } finally {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(1);
            }
        }
    }

    private void recordCrash(Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        String sanitizedStackTrace = SecureLog.sanitize(stackTrace);
        String threadName = thread != null ? thread.getName() : "Unknown";
        String exceptionName = throwable.getClass().getName();
        String message = SecureLog.sanitize(throwable.getMessage());

        String currentUserId = null;
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        } catch (Exception ignored) {}

        // 1. Save locally in SharedPreferences for immediate restart inspection
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putLong(KEY_LAST_CRASH_TIME, System.currentTimeMillis())
                    .putString(KEY_LAST_CRASH_INFO, exceptionName + ": " + message)
                    .apply();
        } catch (Exception ignored) {}

        // 2. Transmit sanitized crash diagnostic to Firestore `crash_reports`
        try {
            Map<String, Object> crashDoc = new HashMap<>();
            crashDoc.put("timestamp", new Date());
            crashDoc.put("thread", threadName);
            crashDoc.put("exception", exceptionName);
            crashDoc.put("message", message);
            crashDoc.put("stackTrace", sanitizedStackTrace);
            crashDoc.put("userId", currentUserId != null ? currentUserId : "ANONYMOUS");
            crashDoc.put("deviceManufacturer", Build.MANUFACTURER);
            crashDoc.put("deviceModel", Build.MODEL);
            crashDoc.put("androidVersion", Build.VERSION.RELEASE);
            crashDoc.put("sdkInt", Build.VERSION.SDK_INT);

            FirebaseFirestore.getInstance().collection("crash_reports").add(crashDoc);
        } catch (Exception ignored) {}

        SecureLog.e("AppCrashHandler", "FATAL CRASH recorded on [" + threadName + "]: " + exceptionName);
    }

    public static boolean hadRecentCrash(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastCrash = prefs.getLong(KEY_LAST_CRASH_TIME, 0L);
        // Returns true if crash occurred within the last 60 seconds (potential crash loop)
        return (System.currentTimeMillis() - lastCrash) < 60000L;
    }
}
