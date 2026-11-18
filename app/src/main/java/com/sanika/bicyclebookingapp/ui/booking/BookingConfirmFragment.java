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

        // Populate UI
        binding.tvBicycleId.setText("Bicycle: " + bicycleId.substring(0, 8).toUpperCase());
        binding.tvStation.setText("Station: " + stationName);
        binding.tvRate.setText("Fare: ₹5 base + ₹8/km + ₹1/min");
        binding.tvDeposit.setText("Security Deposit: ₹"
            + (int) AutoBillingManager.SECURITY_DEPOSIT
            + " (adjusted against final fare on return)");

        binding.btnConfirmBooking.setText("Pay ₹"
            + (int) AutoBillingManager.SECURITY_DEPOSIT + " Deposit & Book");

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
                // Step 1: Collect deposit via UPI
                collectDeposit();
            } else {
                // Already paid — proceed directly
                proceedWithBooking("ALREADY_PAID");
            }
        });
    }

    /**
     * Opens UPI to collect ₹100 security deposit.
     */
    private void collectDeposit() {
        setLoading(true);
        binding.tvError.setVisibility(View.GONE);

        UpiPaymentManager.startUpiPayment(
            requireActivity(),
            AutoBillingManager.SECURITY_DEPOSIT,
            "DEP_" + bicycleId.substring(0, 8),
            "CycleGo security deposit"
        );
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
