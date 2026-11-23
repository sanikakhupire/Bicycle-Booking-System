package com.sanika.bicyclebookingapp.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.razorpay.PaymentResultListener;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ActivityMainBinding;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.util.NotificationHelper;
import com.sanika.bicyclebookingapp.util.UpiPaymentManager;

/**
 * Main activity — handles deep linking, notification permission, Razorpay and UPI callbacks.
 */
public class MainActivity extends AppCompatActivity implements PaymentResultListener {

    private ActivityMainBinding binding;
    private NavController navController;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handled permission grant
            });

    public interface PaymentCallback {
        void onSuccess(String paymentId);
        void onFailure(String reason);
    }
    private PaymentCallback paymentCallback;

    public void setPaymentCallback(PaymentCallback callback) {
        this.paymentCallback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Android 8.0+ notification channels
        NotificationHelper.createNotificationChannels(this);
        checkNotificationPermission();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.qrScanFragment
                    || id == R.id.activeRideFragment
                    || id == R.id.rideCompleteFragment
                    || id == R.id.bookingConfirmFragment
                    || id == R.id.stationDetailFragment
                    || id == R.id.reportDamageFragment
                    || id == R.id.notificationPreferencesFragment) {
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
        });

        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || navController == null) return;

        String targetType = intent.getStringExtra("targetType");
        String targetId   = intent.getStringExtra("targetId");

        if (targetType == null) return;

        try {
            if (AppNotification.TARGET_PAYMENT.equalsIgnoreCase(targetType)
                    || AppNotification.TARGET_REFUND.equalsIgnoreCase(targetType)) {
                navController.navigate(R.id.paymentHistoryFragment);
            } else if (AppNotification.TARGET_MAINTENANCE.equalsIgnoreCase(targetType)) {
                Bundle args = new Bundle();
                args.putString("bicycleId", targetId != null ? targetId : "");
                navController.navigate(R.id.reportDamageFragment, args);
            } else if (AppNotification.TARGET_RIDE.equalsIgnoreCase(targetType)) {
                if (targetId != null && !targetId.isEmpty()) {
                    Bundle args = new Bundle();
                    args.putString("bookingId", targetId);
                    navController.navigate(R.id.activeRideFragment, args);
                } else {
                    navController.navigate(R.id.historyFragment);
                }
            } else if (AppNotification.TARGET_BOOKING.equalsIgnoreCase(targetType)) {
                navController.navigate(R.id.historyFragment);
            }
        } catch (Exception ignored) {}
    }

    // UPI payment result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UpiPaymentManager.UPI_REQUEST_CODE) {
            String status = UpiPaymentManager.getPaymentStatus(resultCode, data);
            String txnId  = UpiPaymentManager.getTransactionId(data);
            if ("SUCCESS".equals(status) || "PENDING".equals(status)) {
                if (paymentCallback != null) paymentCallback.onSuccess(
                    "PENDING".equals(status) ? "PENDING_" + txnId : txnId);
            } else {
                if (paymentCallback != null) paymentCallback.onFailure("Payment failed or cancelled");
            }
        }
    }

    // Razorpay callbacks (kept as fallback)
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        if (paymentCallback != null) paymentCallback.onSuccess(razorpayPaymentId);
    }

    @Override
    public void onPaymentError(int code, String response) {
        if (paymentCallback != null) paymentCallback.onFailure(response);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
