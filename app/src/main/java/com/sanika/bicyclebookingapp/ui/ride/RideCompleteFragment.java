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
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.AutoBillingManager;
import com.sanika.bicyclebookingapp.util.FareCalculator;
import com.sanika.bicyclebookingapp.util.UpiPaymentManager;
import com.sanika.bicyclebookingapp.util.ValidationUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Ride summary and receipt screen.
 * Displays itemized fare breakdown in INR, GPS distance, duration,
 * pickup/return stations, deposit reconciliation, and UPI/Cash settlement.
 */
public class RideCompleteFragment extends Fragment {

    private FragmentRideCompleteBinding binding;
    private String bookingId;
    private float fare;
    private long durationMinutes;
    private double distanceKm = 0.0;

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

        // Initial summary display from Safe Args
        binding.tvFare.setText(ValidationUtil.formatFare(fare));
        binding.tvDuration.setText(ValidationUtil.formatDuration(durationMinutes));
        displayItemizedBreakdown(distanceKm, durationMinutes, fare);

        // Fetch complete booking details from Firestore
        loadBookingDetails();

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

        // Pay button → open UPI apps (GPay, PhonePe, Paytm, etc.)
        binding.btnPayNow.setOnClickListener(v ->
            UpiPaymentManager.startUpiPayment(
                requireActivity(), fare, bookingId, "CycleGo ride fare")
        );

        // Cash payment option
        binding.btnPayCash.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Pay by Cash")
                .setMessage("Please pay " + ValidationUtil.formatFare(fare)
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

    private void loadBookingDetails() {
        if (bookingId == null || bookingId.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("bookings").document(bookingId).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists() || binding == null) return;

                    Booking booking = snap.toObject(Booking.class);
                    if (booking != null) {
                        if (booking.getFare() > 0) {
                            fare = (float) booking.getFare();
                        }
                        if (booking.getDurationMinutes() > 0) {
                            durationMinutes = booking.getDurationMinutes();
                        }
                        if (booking.getDistanceKm() > 0) {
                            distanceKm = booking.getDistanceKm();
                        }

                        // Display stations
                        String pickup = booking.getStationName() != null ? booking.getStationName() : "Station";
                        String returnSt = booking.getReturnStationName() != null ? booking.getReturnStationName() : "Station";
                        binding.tvStations.setText(pickup + " → " + returnSt);

                        // Update metrics
                        binding.tvDuration.setText(ValidationUtil.formatDuration(durationMinutes));
                        binding.tvDistance.setText(String.format("%.2f km", distanceKm));
                        binding.tvFare.setText(ValidationUtil.formatFare(fare));

                        displayItemizedBreakdown(distanceKm, durationMinutes, fare);
                        evaluateSettlement(fare);
                    }
                })
                .addOnFailureListener(e -> evaluateSettlement(fare));
    }

    private void displayItemizedBreakdown(double distance, long minutes, float totalFare) {
        if (binding == null) return;

        double base = FareCalculator.getBaseFare();
        double distCharge = FareCalculator.getDistanceCharge(distance);
        double timeCharge = FareCalculator.getTimeCharge(minutes * 60_000L);

        binding.tvBaseFare.setText(String.format("₹%.2f", base));
        binding.tvDistanceFare.setText(String.format("₹%.2f", distCharge));
        binding.tvDistanceLabel.setText(String.format("Distance Charge (%.2f km @ ₹8/km)", distance));
        binding.tvDurationFare.setText(String.format("₹%.2f", timeCharge));
        binding.tvDurationLabel.setText(String.format("Time Charge (%d min @ ₹1/min)", minutes));
    }

    private void evaluateSettlement(float totalFare) {
        if (binding == null) return;

        double deposit = AutoBillingManager.SECURITY_DEPOSIT;
        double settlement = AutoBillingManager.calculateFinalSettlement(totalFare, deposit, 0);

        if (settlement <= 0) {
            // Deposit covers fare — show refund
            double refund = Math.abs(settlement);
            binding.tvPaymentStatus.setText(
                "Security Deposit: ₹" + (int) deposit + "\n" +
                "Total Fare: " + ValidationUtil.formatFare(totalFare) + "\n" +
                (refund > 0
                    ? "✓ Refund Due: ₹" + String.format("%.2f", refund) + " (to original payment method)"
                    : "✓ Fully Covered by Security Deposit"));
            binding.tvPaymentStatus.setTextColor(
                requireContext().getColor(R.color.colorAvailable));
            binding.btnPayNow.setVisibility(View.GONE);
            binding.btnPayCash.setVisibility(View.GONE);
            binding.btnGoHome.setVisibility(View.VISIBLE);
            handlePaymentSuccess("DEPOSIT_COVERED");
        } else {
            // User owes more than deposit
            binding.tvPaymentStatus.setText(
                "Security Deposit: ₹" + (int) deposit + "\n" +
                "Total Fare: " + ValidationUtil.formatFare(totalFare) + "\n" +
                "Balance Due: ₹" + String.format("%.2f", settlement));
            binding.btnPayNow.setText("Pay Balance ₹" + String.format("%.2f", settlement) + " via UPI");
            binding.btnPayNow.setVisibility(View.VISIBLE);
            binding.btnPayCash.setVisibility(View.GONE);
            binding.btnGoHome.setVisibility(View.GONE);
            fare = (float) settlement;
        }
    }

    private void handlePaymentSuccess(String txnId) {
        if (binding == null) return;

        // Save payment to Firestore
        Map<String, Object> payment = new HashMap<>();
        payment.put("bookingId", bookingId);
        payment.put("amount", (double) fare);
        payment.put("status", "SUCCESS");
        payment.put("method", txnId.startsWith("CASH") ? "CASH" : (txnId.equals("DEPOSIT_COVERED") ? "DEPOSIT" : "UPI"));
        payment.put("transactionId", txnId);
        payment.put("paidAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("payments").add(payment);

        FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId)
            .update("paymentStatus", "PAID", "paymentTxnId", txnId);

        // Update UI
        binding.tvPaymentStatus.setText("✓ Ride & Settlement Completed");
        binding.tvPaymentStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
        binding.tvTransactionId.setText("Txn Ref: " + txnId);
        binding.tvTransactionId.setVisibility(View.VISIBLE);
        binding.btnPayNow.setVisibility(View.GONE);
        binding.btnPayCash.setVisibility(View.GONE);
        binding.btnGoHome.setVisibility(View.VISIBLE);
    }

    private void handlePaymentFailure(String reason) {
        if (binding == null) return;
        binding.tvPaymentStatus.setText("Payment was not completed. Please try again or pay cash.");
        binding.tvPaymentStatus.setTextColor(requireContext().getColor(R.color.colorUnavailable));
        binding.btnPayNow.setVisibility(View.VISIBLE);
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
