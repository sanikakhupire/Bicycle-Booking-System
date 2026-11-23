package com.sanika.bicyclebookingapp.ui.ride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentRideCompleteBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.model.PricingConfig;
import com.sanika.bicyclebookingapp.service.BillingService;
import com.sanika.bicyclebookingapp.service.PaymentGatewayService;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.util.UpiPaymentManager;
import com.sanika.bicyclebookingapp.util.ValidationUtil;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Ride summary and receipt screen.
 * Displays itemized fare breakdown in INR, GPS distance, duration,
 * pickup/return stations, promotional discount, GST breakdown,
 * deposit reconciliation, and UPI/Cash settlement.
 */
public class RideCompleteFragment extends Fragment {

    private FragmentRideCompleteBinding binding;
    private BookingViewModel bookingViewModel;
    private com.sanika.bicyclebookingapp.viewmodel.LoyaltyViewModel loyaltyViewModel;
    private com.sanika.bicyclebookingapp.viewmodel.ReferralViewModel referralViewModel;

    private String bookingId;
    private String bicycleId;
    private String pickupStationId;
    private String pickupStationName;
    private String returnStationId;
    private String returnStationName;
    private float fare;
    private long durationMinutes;
    private double distanceKm = 0.0;
    private double outstandingBalanceInr = 0.0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRideCompleteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        loyaltyViewModel = new ViewModelProvider(this).get(com.sanika.bicyclebookingapp.viewmodel.LoyaltyViewModel.class);
        referralViewModel = new ViewModelProvider(this).get(com.sanika.bicyclebookingapp.viewmodel.ReferralViewModel.class);

        RideCompleteFragmentArgs args = RideCompleteFragmentArgs.fromBundle(getArguments());
        bookingId       = args.getBookingId();
        fare            = args.getFare();
        durationMinutes = args.getDurationMinutes();

        String refStr = "Ref: " + (bookingId != null && bookingId.length() >= 8
                ? "BK-" + bookingId.substring(0, 8).toUpperCase() : "BK-" + bookingId);
        binding.tvBookingRef.setText(refStr);

        binding.tvFare.setText(CurrencyUtil.formatInr(fare));
        binding.tvDuration.setText(ValidationUtil.formatDuration(durationMinutes));

        loadInvoiceAndBookingDetails();
        setupPaymentCallbacks();
    }

    private void loadInvoiceAndBookingDetails() {
        if (bookingId == null || bookingId.isEmpty()) return;

        // 1. First try loading official generated Invoice
        bookingViewModel.getBookingInvoice(bookingId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource.isSuccess() && resource.data != null) {
                displayInvoice(resource.data);
            } else {
                // Fallback: load booking directly and compute breakdown
                loadBookingFallback();
            }
        });
    }

    private void displayInvoice(Invoice invoice) {
        if (binding == null || invoice == null) return;

        if (invoice.getBicycleId() != null) {
            bicycleId = invoice.getBicycleId();
        }
        pickupStationId = invoice.getStationId();
        pickupStationName = invoice.getStationName();
        returnStationId = invoice.getReturnStationId();
        returnStationName = invoice.getReturnStationName();
        fare = (float) invoice.getTotalAmountInr();
        durationMinutes = invoice.getDurationMinutes();
        distanceKm = invoice.getDistanceKm();

        String pickup = invoice.getStationName() != null ? invoice.getStationName() : "Station";
        String returnSt = invoice.getReturnStationName() != null ? invoice.getReturnStationName() : "Station";
        binding.tvStations.setText(pickup + " → " + returnSt);

        binding.tvDuration.setText(ValidationUtil.formatDuration(durationMinutes));
        binding.tvDistance.setText(String.format("%.2f km", distanceKm));
        binding.tvBicycleType.setText(invoice.getBicycleType() != null ? invoice.getBicycleType() : "Standard Bicycle");

        // Itemized Breakdown
        binding.tvBaseFare.setText(CurrencyUtil.formatInr(invoice.getBaseFeeInr()));
        binding.tvDurationFare.setText(CurrencyUtil.formatInr(invoice.getUsageChargeInr()));
        binding.tvDurationLabel.setText("Usage Charge (" + durationMinutes + " min)");

        // Late fee
        if (invoice.getLateFeeInr() > 0) {
            binding.rowLateFee.setVisibility(View.VISIBLE);
            binding.tvLateFee.setText(CurrencyUtil.formatInr(invoice.getLateFeeInr()));
        } else {
            binding.rowLateFee.setVisibility(View.GONE);
        }

        // Subtotal
        binding.tvSubtotal.setText(CurrencyUtil.formatInr(invoice.getSubtotalInr()));

        // Discount
        if (invoice.getDiscountInr() > 0) {
            binding.rowDiscount.setVisibility(View.VISIBLE);
            String couponLabel = invoice.getCouponCode() != null ? " (" + invoice.getCouponCode() + ")" : "";
            binding.tvDiscountLabel.setText("Promotional Discount" + couponLabel);
            binding.tvDiscount.setText("-" + CurrencyUtil.formatInr(invoice.getDiscountInr()));
        } else {
            binding.rowDiscount.setVisibility(View.GONE);
        }

        // Tax
        if (invoice.isTaxApplied() && invoice.getTotalTaxInr() > 0) {
            binding.rowTax.setVisibility(View.VISIBLE);
            binding.tvTaxLabel.setText(String.format("India GST (%.1f%%)", invoice.getTaxRatePercent()));
            binding.tvTax.setText(CurrencyUtil.formatInr(invoice.getTotalTaxInr()));
        } else {
            binding.rowTax.setVisibility(View.GONE);
        }

        // Total
        binding.tvFare.setText(CurrencyUtil.formatInr(fare));

        // Settlements
        double depositInr = invoice.getSecurityDepositInr();
        double refundInr = invoice.getRefundAmountInr();
        double outstandingInr = invoice.getAmountOutstandingInr();
        outstandingBalanceInr = outstandingInr;

        if (outstandingInr <= 0) {
            // Paid or covered by deposit
            binding.tvPaymentStatus.setText(
                    depositInr > 0
                            ? (refundInr > 0
                            ? "✓ Fully covered by Security Deposit\nRefund Due: " + CurrencyUtil.formatInr(refundInr) + " to original payment method"
                            : "✓ Fully covered by Security Deposit")
                            : "✓ Total Fare: " + CurrencyUtil.formatInr(fare) + " (Zero Balance Due)");
            binding.tvPaymentStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
            binding.btnPayNow.setVisibility(View.GONE);
            binding.btnPayCash.setVisibility(View.GONE);
            binding.btnGoHome.setVisibility(View.VISIBLE);
            handlePaymentSuccess("DEPOSIT_COVERED");
        } else {
            // Outstanding amount
            binding.tvPaymentStatus.setText(
                    depositInr > 0
                            ? "Security Deposit: " + CurrencyUtil.formatInr(depositInr) + "\nTotal Fare: " + CurrencyUtil.formatInr(fare) + "\nBalance Due: " + CurrencyUtil.formatInr(outstandingInr)
                            : "Total Amount Due: " + CurrencyUtil.formatInr(outstandingInr));
            binding.btnPayNow.setText("Pay Balance " + CurrencyUtil.formatInr(outstandingInr) + " via UPI");
            binding.btnPayNow.setVisibility(View.VISIBLE);
            binding.btnPayCash.setVisibility(View.VISIBLE);
            binding.btnGoHome.setVisibility(View.GONE);
        }
    }

    private void loadBookingFallback() {
        FirebaseFirestore.getInstance().collection("bookings").document(bookingId).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists() || binding == null) return;
                    Booking booking = snap.toObject(Booking.class);
                    if (booking != null) {
                        bicycleId = booking.getBicycleId();
                        Invoice fallbackInvoice = BillingService.getInstance().generateInvoice(
                                PricingConfig.getDefaultSampleConfig(),
                                booking,
                                booking.getDurationMinutes() > 0 ? booking.getDurationMinutes() : durationMinutes,
                                booking.getDistanceKm() > 0 ? booking.getDistanceKm() : distanceKm,
                                null,
                                booking.getDepositAmount()
                        );
                        displayInvoice(fallbackInvoice);
                    }
                });
    }

    private void setupPaymentCallbacks() {
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

        // Pay button → launches Indian gateway (Razorpay / UPI / Cards / Netbanking / Sandbox)
        binding.btnPayNow.setOnClickListener(v -> {
            binding.btnPayNow.setEnabled(false);
            binding.btnPayNow.setText("Connecting Gateway...");

            PaymentGatewayService.getInstance().startPayment(
                    requireActivity(),
                    bookingId,
                    null,
                    "FARE",
                    "Customer",
                    null,
                    null,
                    new PaymentGatewayService.GatewayCallback() {
                        @Override
                        public void onInitiating() {
                            if (binding != null) {
                                binding.btnPayNow.setEnabled(false);
                                binding.btnPayNow.setText("Creating Order...");
                            }
                        }

                        @Override
                        public void onPaymentSuccess(String paymentId, String transactionId) {
                            if (binding != null) {
                                handlePaymentSuccess(transactionId);
                            }
                        }

                        @Override
                        public void onPaymentFailed(String reason) {
                            if (binding != null) {
                                binding.btnPayNow.setEnabled(true);
                                binding.btnPayNow.setText("Retry Payment (" + CurrencyUtil.formatInr(outstandingBalanceInr > 0 ? outstandingBalanceInr : fare) + ")");
                                handlePaymentFailure(reason);
                            }
                        }
                    }
            );
        });

        // Cash payment option
        binding.btnPayCash.setOnClickListener(v -> {
            double amountToPay = outstandingBalanceInr > 0 ? outstandingBalanceInr : fare;
            new AlertDialog.Builder(requireContext())
                .setTitle("Pay by Cash")
                .setMessage("Please pay " + CurrencyUtil.formatInr(amountToPay)
                    + " in cash to the station attendant.\n\nMark as paid?")
                .setPositiveButton("Mark as Paid", (d, w) -> handlePaymentSuccess("CASH_" + bookingId))
                .setNegativeButton("Cancel", null)
                .show();
        });

        binding.btnRateRide.setOnClickListener(v -> {
            RateRideDialogFragment dialog = RateRideDialogFragment.newInstance(
                    bookingId,
                    bicycleId,
                    pickupStationId,
                    pickupStationName,
                    returnStationId,
                    returnStationName
            );
            dialog.setOnReviewSubmittedListener(review -> {
                binding.btnRateRide.setText("✓ Ride & Bicycle Rated");
                binding.btnRateRide.setEnabled(false);
            });
            dialog.show(getChildFragmentManager(), "RateRideDialog");
        });

        binding.btnGoHome.setOnClickListener(v -> {
            NavDirections action = RideCompleteFragmentDirections.actionRideCompleteToHome();
            Navigation.findNavController(v).navigate(action);
        });

        binding.btnReportIssue.setOnClickListener(v -> {
            Bundle reportBundle = new Bundle();
            reportBundle.putString("bicycleId", bicycleId != null ? bicycleId : "");
            reportBundle.putString("bookingId", bookingId != null ? bookingId : "");
            Navigation.findNavController(v).navigate(R.id.action_rideComplete_to_reportDamage, reportBundle);
        });
    }

    private void handlePaymentSuccess(String txnId) {
        if (binding == null) return;

        double amountRecorded = outstandingBalanceInr > 0 ? outstandingBalanceInr : fare;

        Map<String, Object> payment = new HashMap<>();
        payment.put("bookingId", bookingId);
        payment.put("amount", amountRecorded);
        payment.put("status", "SUCCESS");
        payment.put("method", txnId.startsWith("CASH") ? "CASH" : (txnId.equals("DEPOSIT_COVERED") ? "DEPOSIT" : "UPI"));
        payment.put("transactionId", txnId);
        payment.put("paidAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("payments").add(payment);

        FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId)
            .update("paymentStatus", "PAID", "paymentTxnId", txnId);

        // Credit customer loyalty points and evaluate milestone bonuses
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            loyaltyViewModel.creditRidePoints(currentUid, bookingId, amountRecorded);
            referralViewModel.onRefereeRideCompleted(currentUid, bookingId);
        }

        // Update UI
        binding.tvPaymentStatus.setText("✓ Ride & Settlement Completed");
        binding.tvPaymentStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
        binding.tvTransactionId.setText("Txn Ref: " + txnId);
        binding.tvTransactionId.setVisibility(View.VISIBLE);
        binding.btnPayNow.setVisibility(View.GONE);
        binding.btnPayCash.setVisibility(View.GONE);
        binding.btnRateRide.setVisibility(View.VISIBLE);
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
