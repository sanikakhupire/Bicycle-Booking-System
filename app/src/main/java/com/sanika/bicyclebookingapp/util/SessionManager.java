package com.sanika.bicyclebookingapp.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.ui.auth.AuthActivity;

/**
 * Enterprise Session & Authentication Lifecycle Manager.
 * Handles user credentials, session state verification, cached roles,
 * and secure sign-out with background process cleanup.
 */
public class SessionManager {

    private static final String PREF_NAME = "cyclego_user_session";
    private static final String KEY_CACHED_ROLE = "cached_role";
    private static final String KEY_LAST_LOGIN = "last_login_time";

    private static volatile SessionManager instance;
    private final Context context;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    public boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public void cacheUserRole(String role) {
        prefs.edit()
                .putString(KEY_CACHED_ROLE, role != null ? role.toUpperCase() : "USER")
                .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                .apply();
    }

    public String getCachedUserRole() {
        return prefs.getString(KEY_CACHED_ROLE, "USER");
    }

    public boolean isCachedAdmin() {
        return "ADMIN".equalsIgnoreCase(getCachedUserRole());
    }

    /**
     * Completely terminates user session, signs out from Firebase,
     * clears local session cache, and redirects to login screen.
     */
    public void logout(Context activityContext) {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            SecureLog.e("SessionManager", "Error signing out: " + e.getMessage());
        }

        prefs.edit().clear().apply();
        SecureLog.i("SessionManager", "User session successfully terminated.");

        Intent intent = new Intent(activityContext, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activityContext.startActivity(intent);
    }
}
