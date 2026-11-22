package com.sanika.bicyclebookingapp.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.databinding.FragmentBookingConfirmBinding;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.AutoBillingManager;
import com.sanika.bicyclebookingapp.util.UpiPaymentManager;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;

/**
 * Booking confirmation screen.
 *
 * Flow:
 * 1. User sees fare info + ₹100 security deposit requirement
 * 2. User taps "Pay Deposit & Book"
 * 3. UPI opens → user pays ₹100 deposit
 * 4. On deposit success → booking is created → QR scan screen
 *
 * The deposit is used for:
 * - Hourly auto-deduction if ride > 1 hour
 * - Final fare settlement on return
 * - Refund if deposit > fare
 */
public class BookingConfirmFragment extends Fragment {

    private FragmentBookingConfirmBinding binding;
    private BookingViewModel bookingViewModel;

    private String bicycleId;
    private String stationId;
    private String stationName;
    private boolean depositPaid = false;

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

        String previewId = "BK-" + (bicycleId.length() >= 6 ? bicycleId.substring(0, 6).toUpperCase() : bicycleId.toUpperCase());
        binding.tvBookingRef.setText("Reference: " + previewId);
        binding.tvBicycleId.setText("Bicycle: #" + (bicycleId.length() >= 8 ? bicycleId.substring(0, 8).toUpperCase() : bicycleId.toUpperCase()));
        binding.tvStation.setText("Pickup Station: " + stationName);
        binding.tvRate.setText("• Base Unlock Fee: ₹5.00\n• Per-Minute Charge: ₹1.00 / min\n• Per-KM Charge: ₹8.00 / km\n• Minimum Fare: ₹10.00");
        binding.tvDeposit.setText("Security Deposit: ₹"
            + (int) AutoBillingManager.SECURITY_DEPOSIT
            + " (Refundable / Adjusted on return)");

        binding.btnConfirmBooking.setText("Pay ₹"
            + (int) AutoBillingManager.SECURITY_DEPOSIT + " Deposit & Reserve (10m Hold)");

        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Register payment callback
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setPaymentCallback(
                new MainActivity.PaymentCallback() {
                    @Override
                    public void onSuccess(String txnId) {
                        depositPaid = true;
                        // Deposit paid — now create the booking
                        proceedWithBooking(txnId);
                    }
                    @Override
                    public void onFailure(String reason) {
                        setLoading(false);
                        binding.tvError.setVisibility(View.VISIBLE);
                        binding.tvError.setText(
                            "Deposit payment failed. Please try again.\n" + reason);
                    }
                }
            );
        }

        binding.btnConfirmBooking.setOnClickListener(v -> {
            if (!depositPaid) {
                // Step 1: Collect deposit via UPI or test fallback
                collectDeposit();
            } else {
                // Already paid — proceed directly
                proceedWithBooking("ALREADY_PAID");
            }
        });
    }

    /**
     * Opens UPI to collect ₹100 security deposit.
     * Provides test mode fallback when running on emulator or non-UPI device.
     */
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
                AutoBillingManager.SECURITY_DEPOSIT,
                "DEP_" + (bicycleId.length() >= 8 ? bicycleId.substring(0, 8) : bicycleId),
                "CycleGo security deposit"
            );
        } else {
            // No UPI app found (e.g. Emulator / development device)
            setLoading(false);
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("UPI App Not Detected")
                .setMessage("No Indian UPI app (GPay/PhonePe/Paytm) was found on this device.\n\nWould you like to simulate ₹100 test deposit to verify the booking flow?")
                .setPositiveButton("Simulate ₹100 Deposit", (dialog, which) -> {
                    depositPaid = true;
                    proceedWithBooking("SIM_TEST_" + System.currentTimeMillis());
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    /**
     * Creates the booking after deposit is confirmed.
     */
    private void proceedWithBooking(String depositTxnId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        setLoading(true);

        bookingViewModel.createBooking(uid, bicycleId, stationId, stationName)
            .observe(getViewLifecycleOwner(), resource -> {
                switch (resource.status) {
                    case LOADING:
                        setLoading(true);
                        break;
                    case SUCCESS:
                        setLoading(false);
                        if (resource.data != null) {
                            // Save deposit payment record
                            saveDepositRecord(resource.data.getBookingId(),
                                    uid, depositTxnId);

                            // Navigate to QR scan
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
        java.util.Map<String, Object> deposit = new java.util.HashMap<>();
        deposit.put("bookingId", bookingId);
        deposit.put("userId", userId);
        deposit.put("amount", AutoBillingManager.SECURITY_DEPOSIT);
        deposit.put("type", "SECURITY_DEPOSIT");
        deposit.put("transactionId", txnId);
        deposit.put("status", "PAID");
        deposit.put("paidAt",
            com.google.firebase.firestore.FieldValue.serverTimestamp());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("payments").add(deposit);

        // Update booking with deposit info
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId)
            .update("depositPaid", AutoBillingManager.SECURITY_DEPOSIT,
                    "depositTxnId", txnId,
                    "autoDeducted", 0.0,
                    "remainingDeposit", AutoBillingManager.SECURITY_DEPOSIT);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnConfirmBooking.setEnabled(!loading);
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
