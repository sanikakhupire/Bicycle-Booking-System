package com.sanika.bicyclebookingapp.ui.booking;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentBookingConfirmBinding;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.PricingConfig;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.util.UpiPaymentManager;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;
import com.sanika.bicyclebookingapp.viewmodel.PricingViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Booking confirmation screen with dynamic India pricing,
 * promotional coupons, security deposit options, and 10-minute hold reservation.
 */
public class BookingConfirmFragment extends Fragment {

    private FragmentBookingConfirmBinding binding;
    private BookingViewModel bookingViewModel;
    private PricingViewModel pricingViewModel;

    private String bicycleId;
    private String stationId;
    private String stationName;
    private String bicycleType = "Standard";

    private PricingConfig activePricingConfig;
    private Coupon appliedCoupon;
    private boolean depositPaid = false;
    private double requiredDepositInr = 0.0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBookingConfirmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BookingConfirmFragmentArgs args = BookingConfirmFragmentArgs.fromBundle(getArguments());
        bicycleId   = args.getBicycleId();
        stationId   = args.getStationId();
        stationName = args.getStationName();

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        pricingViewModel = new ViewModelProvider(this).get(PricingViewModel.class);

        String previewId = "BK-" + (bicycleId.length() >= 6 ? bicycleId.substring(0, 6).toUpperCase() : bicycleId.toUpperCase());
        binding.tvBookingRef.setText("Reference: " + previewId);
        binding.tvBicycleId.setText("Bicycle: #" + (bicycleId.length() >= 8 ? bicycleId.substring(0, 8).toUpperCase() : bicycleId.toUpperCase()));
        binding.tvStation.setText("Pickup Station: " + stationName);

        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());

        setupCouponSection();
        setupPaymentCallback();
        loadPricingConfig();
    }

    private void loadPricingConfig() {
        setLoading(true);
        pricingViewModel.getPricingConfig().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            setLoading(false);
            if (resource.data != null) {
                activePricingConfig = resource.data;
                displayPricing(activePricingConfig);
            } else {
                activePricingConfig = PricingConfig.getDefaultSampleConfig();
                displayPricing(activePricingConfig);
            }
        });
    }

    private void displayPricing(PricingConfig config) {
        double baseFee = config.getBaseFeeInr();
        double ratePerMin = config.getRatePerMinuteInr();
        double ratePerHour = config.getRatePerHourInr();
        double minFare = config.getMinimumFareInr();

        StringBuilder rateText = new StringBuilder();
        rateText.append("• Base Unlock Fee: ").append(CurrencyUtil.formatInr(baseFee)).append("\n");
        rateText.append("• Per-Minute Rate: ").append(CurrencyUtil.formatInr(ratePerMin)).append(" / min\n");
        rateText.append("• Per-Hour Rate: ").append(CurrencyUtil.formatInr(ratePerHour)).append(" / hr\n");
        rateText.append("• Minimum Ride Charge: ").append(CurrencyUtil.formatInr(minFare));

        if (config.isTaxEnabled() && config.getTaxPercentage() > 0) {
            rateText.append("\n• Taxes: +").append(String.format("%.1f%% GST (CGST + SGST)", config.getTaxPercentage()));
        }

        binding.tvRate.setText(rateText.toString());

        if (config.isSecurityDepositEnabled() && config.getSecurityDepositInr() > 0) {
            requiredDepositInr = config.getSecurityDepositInr();
            binding.tvDeposit.setText("Security Deposit: " + CurrencyUtil.formatInr(requiredDepositInr)
                    + " (Refundable / Adjusted on return)");
            binding.btnConfirmBooking.setText("Pay " + CurrencyUtil.formatInr(requiredDepositInr) + " Deposit & Reserve (10m Hold)");
            setupDepositPaymentButton();
        } else {
            requiredDepositInr = 0.0;
            depositPaid = true;
            binding.tvDeposit.setText("Security Deposit: ₹0.00 (No deposit required)");
            binding.btnConfirmBooking.setText("Confirm & Reserve Bicycle (10m Hold)");
            binding.btnConfirmBooking.setOnClickListener(v -> proceedWithBooking("NO_DEPOSIT_REQUIRED"));
        }
    }

    private void setupCouponSection() {
        binding.btnApplyCoupon.setOnClickListener(v -> {
            String code = binding.etCouponCode.getText() != null
                    ? binding.etCouponCode.getText().toString().trim() : "";

            if (TextUtils.isEmpty(code)) {
                binding.tvCouponStatus.setVisibility(View.VISIBLE);
                binding.tvCouponStatus.setTextColor(requireContext().getColor(R.color.colorUnavailable));
                binding.tvCouponStatus.setText("Enter a coupon code");
                return;
            }

            setLoading(true);
            long estimatedSubtotalPaise = activePricingConfig != null ? activePricingConfig.getBaseFeePaise() + 2000L : 3000L;
            pricingViewModel.validateCoupon(code, estimatedSubtotalPaise).observe(getViewLifecycleOwner(), resource -> {
                if (binding == null) return;
                setLoading(false);
                switch (resource.status) {
                    case SUCCESS:
                        appliedCoupon = resource.data;
                        binding.tvCouponStatus.setVisibility(View.VISIBLE);
                        binding.tvCouponStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
                        binding.tvCouponStatus.setText("✓ " + (appliedCoupon != null ? appliedCoupon.getDescription() : "Coupon applied!"));
                        Toast.makeText(requireContext(), "Coupon applied successfully!", Toast.LENGTH_SHORT).show();
                        break;
                    case ERROR:
                        appliedCoupon = null;
                        binding.tvCouponStatus.setVisibility(View.VISIBLE);
                        binding.tvCouponStatus.setTextColor(requireContext().getColor(R.color.colorUnavailable));
                        binding.tvCouponStatus.setText(resource.message != null ? resource.message : "Invalid coupon code");
                        break;
                }
            });
        });
    }

    private void setupPaymentCallback() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setPaymentCallback(
                new MainActivity.PaymentCallback() {
                    @Override
                    public void onSuccess(String txnId) {
                        depositPaid = true;
                        proceedWithBooking(txnId);
                    }
                    @Override
                    public void onFailure(String reason) {
                        setLoading(false);
                        binding.tvError.setVisibility(View.VISIBLE);
                        binding.tvError.setText("Deposit payment failed: " + reason);
                    }
                }
            );
        }
    }

    private void setupDepositPaymentButton() {
        binding.btnConfirmBooking.setOnClickListener(v -> {
            if (!depositPaid) {
                collectDeposit();
            } else {
                proceedWithBooking("ALREADY_PAID");
            }
        });
    }

    private void collectDeposit() {
        setLoading(true);
        binding.tvError.setVisibility(View.GONE);

        android.content.Intent upiIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("upi://pay"));
        java.util.List<android.content.pm.ResolveInfo> apps =
                requireActivity().getPackageManager().queryIntentActivities(upiIntent, 0);

        if (apps != null && !apps.isEmpty()) {
            UpiPaymentManager.startUpiPayment(
                requireActivity(),
                (float) requiredDepositInr,
                "DEP_" + (bicycleId.length() >= 8 ? bicycleId.substring(0, 8) : bicycleId),
                "CycleGo security deposit"
            );
        } else {
            setLoading(false);
            new AlertDialog.Builder(requireContext())
                .setTitle("UPI App Not Detected")
                .setMessage("No Indian UPI app (GPay/PhonePe/Paytm) was found on this device.\n\nWould you like to simulate "
                        + CurrencyUtil.formatInr(requiredDepositInr) + " test deposit to verify booking?")
                .setPositiveButton("Simulate Deposit", (dialog, which) -> {
                    depositPaid = true;
                    proceedWithBooking("SIM_DEP_" + System.currentTimeMillis());
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void proceedWithBooking(String depositTxnId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        setLoading(true);

        String couponCode = appliedCoupon != null ? appliedCoupon.getCode() : null;
        Map<String, Object> snapshot = activePricingConfig != null ? activePricingConfig.toMap() : new HashMap<>();

        bookingViewModel.createBooking(
                uid,
                bicycleId,
                stationId,
                stationName,
                bicycleType,
                couponCode,
                requiredDepositInr,
                snapshot
        ).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            switch (resource.status) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    if (resource.data != null) {
                        if (requiredDepositInr > 0) {
                            saveDepositRecord(resource.data.getBookingId(), uid, depositTxnId);
                        }

                        BookingConfirmFragmentDirections.ActionBookingConfirmToQrScan action =
                                BookingConfirmFragmentDirections.actionBookingConfirmToQrScan(
                                        resource.data.getBookingId(), bicycleId);
                        Navigation.findNavController(requireView()).navigate(action);
                    }
                    break;
                case ERROR:
                    setLoading(false);
                    binding.tvError.setVisibility(View.VISIBLE);
                    binding.tvError.setText(resource.message);
                    break;
            }
        });
    }

    private void saveDepositRecord(String bookingId, String userId, String txnId) {
        Map<String, Object> deposit = new HashMap<>();
        deposit.put("bookingId", bookingId);
        deposit.put("userId", userId);
        deposit.put("amount", requiredDepositInr);
        deposit.put("type", "SECURITY_DEPOSIT");
        deposit.put("transactionId", txnId);
        deposit.put("status", "PAID");
        deposit.put("paidAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("payments").add(deposit);

        FirebaseFirestore.getInstance().collection("bookings").document(bookingId)
                .update("depositPaid", requiredDepositInr,
                        "depositTxnId", txnId,
                        "autoDeducted", 0.0,
                        "remainingDeposit", requiredDepositInr);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnConfirmBooking.setEnabled(!loading);
        binding.btnApplyCoupon.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setPaymentCallback(null);
        }
        super.onDestroyView();
        binding = null;
    }
}
