package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sanika.bicyclebookingapp.databinding.FragmentAdminPaymentsBinding;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.PaymentConfig;
import com.sanika.bicyclebookingapp.model.Refund;
import com.sanika.bicyclebookingapp.repository.PaymentRepository;
import com.sanika.bicyclebookingapp.ui.payment.PaymentHistoryAdapter;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.viewmodel.PaymentViewModel;

import java.util.List;
import java.util.Map;

/**
 * Admin screen providing real-time revenue analytics, transaction audit,
 * and refund processing.
 */
public class AdminPaymentsFragment extends Fragment {

    private FragmentAdminPaymentsBinding binding;
    private PaymentViewModel paymentViewModel;
    private PaymentHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminPaymentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paymentViewModel = new ViewModelProvider(this).get(PaymentViewModel.class);

        setupRecyclerView();
        observeMetrics();
        observeTransactions();
        observeGatewayConfig();
    }

    private void setupRecyclerView() {
        adapter = new PaymentHistoryAdapter(this::onTransactionSelected);
        binding.rvAdminTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdminTransactions.setAdapter(adapter);
    }

    private void observeMetrics() {
        paymentViewModel.getAdminPaymentOverview().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource.status == com.sanika.bicyclebookingapp.util.Resource.Status.SUCCESS && resource.data != null) {
                Map<String, Object> data = resource.data;
                binding.tvAdminTotalRevenue.setText((String) data.get("formattedRevenue"));
                binding.tvAdminSuccessCount.setText(data.get("successCount") + " successful");

                binding.tvAdminRefundedAmount.setText((String) data.get("formattedRefunded"));
                binding.tvAdminRefundCount.setText(data.get("refundedCount") + " refunded");

                binding.tvAdminPendingCount.setText(data.get("pendingCount") + " Pending");
                binding.tvAdminTotalTransactions.setText(data.get("totalTransactions") + " Total");
            }
        });
    }

    private void observeTransactions() {
        binding.progressBarAdmin.setVisibility(View.VISIBLE);

        paymentViewModel.getAllPayments().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarAdmin.setVisibility(View.GONE);

            switch (resource.status) {
                case SUCCESS:
                    List<Payment> list = resource.data;
                    adapter.setPayments(list);
                    binding.tvAdminEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), "Failed to load transactions: " + resource.message, Toast.LENGTH_SHORT).show();
                    break;
                case LOADING:
                    binding.progressBarAdmin.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void observeGatewayConfig() {
        paymentViewModel.getPaymentConfig().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource.status == com.sanika.bicyclebookingapp.util.Resource.Status.SUCCESS && resource.data != null) {
                PaymentConfig config = resource.data;
                String mode = config.isTestMode() ? "TEST / SANDBOX MODE" : "PRODUCTION LIVE";
                binding.tvGatewayEnv.setText("GATEWAY: " + config.getGatewayProvider() + " (" + mode + ")");
            }
        });
    }

    private void onTransactionSelected(Payment payment) {
        String[] actions;
        boolean canRefund = Payment.STATUS_SUCCESS.equalsIgnoreCase(payment.getStatus());

        if (canRefund) {
            actions = new String[]{"View Full Receipt", "Issue Refund to Customer"};
        } else {
            actions = new String[]{"View Full Receipt"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Transaction #" + (payment.getPaymentId() != null ? payment.getPaymentId() : ""))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showTransactionDetails(payment);
                    } else if (which == 1) {
                        confirmIssueRefund(payment);
                    }
                })
                .show();
    }

    private void showTransactionDetails(Payment payment) {
        String info = "• Amount: " + payment.getFormattedAmount() + "\n"
                + "• Status: " + payment.getStatus() + "\n"
                + "• Method: " + payment.getMethod() + "\n"
                + "• Gateway Txn: " + (payment.getTransactionId() != null ? payment.getTransactionId() : "N/A") + "\n"
                + "• Order ID: " + (payment.getOrderId() != null ? payment.getOrderId() : "N/A") + "\n"
                + "• Customer ID: " + (payment.getUserId() != null ? payment.getUserId() : "N/A") + "\n"
                + "• Booking ID: " + (payment.getBookingId() != null ? payment.getBookingId() : "N/A") + "\n"
                + "• Mode: " + (payment.isTestMode() ? "Test / Sandbox" : "Live");

        new AlertDialog.Builder(requireContext())
                .setTitle("Transaction Record")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirmIssueRefund(Payment payment) {
        long amountPaise = payment.getAmountPaise() > 0 ? payment.getAmountPaise() : CurrencyUtil.toPaise(payment.getAmount());

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Refund")
                .setMessage("Are you sure you want to process a full refund of "
                        + CurrencyUtil.formatInrFromPaise(amountPaise)
                        + " for Booking #" + payment.getBookingId() + "?")
                .setPositiveButton("Process Refund", (dialog, which) -> {
                    paymentViewModel.requestRefund(
                            payment.getPaymentId(),
                            payment.getBookingId(),
                            Refund.REASON_DEPOSIT_RETURN,
                            amountPaise,
                            new PaymentRepository.RefundCallback() {
                                @Override
                                public void onSuccess(String refundId, double amountInr) {
                                    Toast.makeText(requireContext(), "Refund of " + CurrencyUtil.formatInr(amountInr) + " processed successfully!", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(requireContext(), "Refund failed: " + message, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
