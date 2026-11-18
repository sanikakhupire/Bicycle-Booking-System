package com.sanika.bicyclebookingapp.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.razorpay.PaymentResultListener;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ActivityMainBinding;
import com.sanika.bicyclebookingapp.util.UpiPaymentManager;

/**
 * Main activity — handles both Razorpay and UPI payment callbacks.
 */
public class MainActivity extends AppCompatActivity implements PaymentResultListener {

    private ActivityMainBinding binding;
    private NavController navController;

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
                    || id == R.id.stationDetailFragment) {
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
        });
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
