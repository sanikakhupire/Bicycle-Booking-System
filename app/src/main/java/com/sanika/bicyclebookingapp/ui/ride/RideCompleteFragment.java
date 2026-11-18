package com.sanika.bicyclebookingapp.ui.ride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentRideCompleteBinding;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.AutoBillingManager;
import com.sanika.bicyclebookingapp.util.UpiPaymentManager;
import com.sanika.bicyclebookingapp.util.ValidationUtil;

/**
 * Ride summary screen.
 * Tapping "Pay" opens GPay / PhonePe / Paytm directly via UPI Intent.
 * No SDK or registration needed.
 */
public class RideCompleteFragment extends Fragment {

    private FragmentRideCompleteBinding binding;
    private String bookingId;
    private float fare;
    private long durationMinutes;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRideCompleteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RideCompleteFragmentArgs args = RideCompleteFragmentArgs.fromBundle(getArguments());
        bookingId       = args.getBookingId();
        fare            = args.getFare();
        durationMinutes = args.getDurationMinutes();

        // Show ride summary
        binding.tvFare.setText(ValidationUtil.formatFare(fare));
        binding.tvDuration.setText(ValidationUtil.formatDuration(durationMinutes));

        // Check deposit vs fare settlement
        double deposit = AutoBillingManager.SECURITY_DEPOSIT;
        double settlement = AutoBillingManager.calculateFinalSettlement(fare, deposit, 0);

        if (settlement <= 0) {
            // Deposit covers fare — show refund
            double refund = Math.abs(settlement);
            binding.tvPaymentStatus.setText(
                "Deposit paid: ₹" + (int) deposit + "\n" +
                "Fare: " + ValidationUtil.formatFare(fare) + "\n" +
                (refund > 0
                    ? "✓ Refund due: ₹" + String.format("%.2f", refund)
                    : "✓ Fully covered by deposit"));
            binding.tvPaymentStatus.setTextColor(
                requireContext().getColor(R.color.colorAvailable));
            binding.btnPayNow.setVisibility(View.GONE);
            binding.btnPayCash.setVisibility(View.GONE);
            binding.btnGoHome.setVisibility(View.VISIBLE);
            // Auto-mark as paid since deposit covers it
            handlePaymentSuccess("DEPOSIT_COVERED");
        } else {
            // User owes more than deposit
            binding.tvPaymentStatus.setText(
                "Deposit paid: ₹" + (int) deposit + "\n" +
                "Fare: " + ValidationUtil.formatFare(fare) + "\n" +
                "Balance due: ₹" + String.format("%.2f", settlement));
            binding.btnPayNow.setText("Pay Balance ₹" + String.format("%.2f", settlement) + " via UPI");
            binding.btnPayNow.setVisibility(View.VISIBLE);
            binding.btnPayCash.setVisibility(View.GONE);
            binding.btnGoHome.setVisibility(View.GONE);
            // Override fare to only charge the balance
            fare = (float) settlement;
        }

        // Register payment callback with MainActivity
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setPaymentCallback(
                new MainActivity.PaymentCallback() {
                    @Override
                    public void onSuccess(String txnId) {
                        if (binding != null) handlePaymentSuccess(txnId);
                    }
                    @Override
                    public void onFailure(String reason) {
                        if (binding != null) handlePaymentFailure(reason);
                    }
                }
            );
        }

        // Pay button → open UPI apps
        binding.btnPayNow.setOnClickListener(v ->
            UpiPaymentManager.startUpiPayment(
                requireActivity(), fare, bookingId, "CycleGo ride fare")
        );

        // Cash payment option
        binding.btnPayCash.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Pay by Cash")
                .setMessage("Please pay ₹" + String.format("%.2f", fare)
                    + " in cash to the station attendant.\n\nMark as paid?")
                .setPositiveButton("Mark as Paid", (d, w) -> handlePaymentSuccess("CASH_" + bookingId))
                .setNegativeButton("Cancel", null)
                .show();
        });

        binding.btnGoHome.setOnClickListener(v -> {
            NavDirections action = RideCompleteFragmentDirections.actionRideCompleteToHome();
            Navigation.findNavController(v).navigate(action);
        });
    }

    private void handlePaymentSuccess(String txnId) {
        // Save payment to Firestore
        java.util.Map<String, Object> payment = new java.util.HashMap<>();
        payment.put("bookingId", bookingId);
        payment.put("amount", (double) fare);
        payment.put("status", "SUCCESS");
        payment.put("method", "UPI");
        payment.put("transactionId", txnId);
        payment.put("paidAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("payments").add(payment);

        FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId)
            .update("paymentStatus", "PAID", "paymentTxnId", txnId);

        // Update UI
        binding.tvPaymentStatus.setText("✓ Payment Successful!");
        binding.tvPaymentStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
        binding.tvTransactionId.setText("Txn ID: " + txnId);
        binding.tvTransactionId.setVisibility(View.VISIBLE);
        binding.btnPayNow.setVisibility(View.GONE);
        binding.btnGoHome.setVisibility(View.VISIBLE);
    }

    private void handlePaymentFailure(String reason) {
        binding.tvPaymentStatus.setText("Payment failed or cancelled.");
        binding.tvPaymentStatus.setTextColor(requireContext().getColor(R.color.colorUnavailable));
        binding.btnPayNow.setVisibility(View.VISIBLE);
        // Show cash option
        binding.btnPayCash.setVisibility(View.VISIBLE);
        binding.btnGoHome.setVisibility(View.VISIBLE);
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
