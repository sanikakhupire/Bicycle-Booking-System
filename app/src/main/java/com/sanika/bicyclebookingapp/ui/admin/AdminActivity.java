package com.sanika.bicyclebookingapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ActivityAdminBinding;
import com.sanika.bicyclebookingapp.ui.auth.AuthActivity;
import com.sanika.bicyclebookingapp.util.TheftAlertManager;

/**
 * Admin panel activity — manages stations, bicycles, and views active rides.
 * Also starts real-time theft monitoring while admin is active.
 * Enforces role-based security in onCreate.
 */
public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private NavController navController;
    private TheftAlertManager theftAlertManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.sanika.bicyclebookingapp.util.NotificationHelper.createNotificationChannels(this);

        // Hide UI until admin verification succeeds
        binding.getRoot().setVisibility(View.INVISIBLE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        // Verify admin privilege in Firestore
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                    if (Boolean.TRUE.equals(isAdmin)) {
                        // Authorized: Reveal UI and setup navigation
                        binding.getRoot().setVisibility(View.VISIBLE);
                        setupAdminNavigation();
                        // Start real-time theft monitoring
                        theftAlertManager = new TheftAlertManager(this);
                        theftAlertManager.startMonitoring();
                    } else {
                        Toast.makeText(AdminActivity.this, "Access denied: Administrator privileges required.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminActivity.this, "Failed to verify admin status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupAdminNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.admin_nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.adminBottomNavigation, navController);
        }
    }

    @Override
    protected void onDestroy() {
        // Stop monitoring when admin closes the panel
        if (theftAlertManager != null) theftAlertManager.stopMonitoring();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, 1011, 0, "Analytics & Reports")
            .setIcon(android.R.drawable.ic_menu_sort_by_size)
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 1002, 1, "Revenue")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1001, 2, "Pricing")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1003, 3, "Customers")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1004, 4, "Bookings")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1005, 5, "Coupons")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1006, 6, "Support Grievances")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1007, 7, "Admin Settings")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1008, 8, "Maintenance & Repairs")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1009, 9, "Operational Alerts")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 1010, 10, "Reviews & Ratings")
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (navController == null) return super.onOptionsItemSelected(item);

        int id = item.getItemId();
        if (id == 1011) {
            navController.navigate(R.id.adminAnalyticsFragment);
            return true;
        } else if (id == 1001) {
            navController.navigate(R.id.adminPricingFragment);
            return true;
        } else if (id == 1002) {
            navController.navigate(R.id.adminPaymentsFragment);
            return true;
        } else if (id == 1003) {
            navController.navigate(R.id.adminCustomersFragment);
            return true;
        } else if (id == 1004) {
            navController.navigate(R.id.adminBookingsFragment);
            return true;
        } else if (id == 1005) {
            navController.navigate(R.id.adminCouponsFragment);
            return true;
        } else if (id == 1006) {
            navController.navigate(R.id.adminSupportFragment);
            return true;
        } else if (id == 1007) {
            navController.navigate(R.id.adminSettingsFragment);
            return true;
        } else if (id == 1008) {
            navController.navigate(R.id.adminMaintenanceFragment);
            return true;
        } else if (id == 1009) {
            navController.navigate(R.id.adminNotificationsFragment);
            return true;
        } else if (id == 1010) {
            navController.navigate(R.id.adminReviewsFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }
}
