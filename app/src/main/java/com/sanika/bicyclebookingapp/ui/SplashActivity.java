package com.sanika.bicyclebookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.ui.admin.AdminActivity;
import com.sanika.bicyclebookingapp.ui.auth.AuthActivity;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;

/**
 * Splash screen shown briefly at startup.
 * Checks authentication status and routes:
 * - Unauthenticated -> AuthActivity
 * - Admin User -> AdminActivity
 * - Regular User -> MainActivity
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
                finish();
                return;
            }

            // Check if logged-in user is admin
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        Intent intent;
                        if (Boolean.TRUE.equals(isAdmin)) {
                            intent = new Intent(SplashActivity.this, AdminActivity.class);
                        } else {
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // On network or read failure, fallback safely to MainActivity
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    });
        }, SPLASH_DELAY_MS);
    }
}
