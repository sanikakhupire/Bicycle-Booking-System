package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ActivityAdminBinding;
import com.sanika.bicyclebookingapp.util.TheftAlertManager;

/**
 * Admin panel activity — manages stations, bicycles, and views active rides.
 * Also starts real-time theft monitoring while admin is active.
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

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.admin_nav_host_fragment);
        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(binding.adminBottomNavigation, navController);

        // Start real-time theft monitoring
        theftAlertManager = new TheftAlertManager(this);
        theftAlertManager.startMonitoring();
    }

    @Override
    protected void onDestroy() {
        // Stop monitoring when admin closes the panel
        if (theftAlertManager != null) theftAlertManager.stopMonitoring();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
