package com.sanika.bicyclebookingapp.ui.payment;

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

import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentPaymentHistoryBinding;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.viewmodel.PaymentViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen displaying the customer's payment history, INR transactions, and receipts.
 */
public class PaymentHistoryFragment extends Fragment {

    private FragmentPaymentHistoryBinding binding;
    private PaymentViewModel paymentViewModel;
    private PaymentHistoryAdapter adapter;

    private List<Payment> allPayments = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPaymentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paymentViewModel = new ViewModelProvider(this).get(PaymentViewModel.class);

        setupRecyclerView();
        setupFilters();
        loadPayments();
    }

    private void setupRecyclerView() {
        adapter = new PaymentHistoryAdapter(this::showReceiptDialog);
        binding.recyclerViewPayments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPayments.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipSuccess) {
                currentFilter = Payment.STATUS_SUCCESS;
            } else if (checkedId == R.id.chipRefunded) {
                currentFilter = Payment.STATUS_REFUNDED;
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void loadPayments() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        paymentViewModel.getUserPayments(uid).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);

            switch (resource.status) {
                case SUCCESS:
                    allPayments = resource.data != null ? resource.data : new ArrayList<>();
                    updateSummaryMetrics();
                    applyFilter();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
                    break;
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void updateSummaryMetrics() {
        long totalSpentPaise = 0L;
        int completedCount = 0;

        for (Payment p : allPayments) {
            if (Payment.STATUS_SUCCESS.equalsIgnoreCase(p.getStatus())) {
                totalSpentPaise += (p.getAmountPaise() > 0 ? p.getAmountPaise() : CurrencyUtil.toPaise(p.getAmount()));
                completedCount++;
            }
        }

        binding.tvTotalSpent.setText(CurrencyUtil.formatInrFromPaise(totalSpentPaise));
        binding.tvTotalTransactionsCount.setText("Across " + completedCount + " successful transaction" + (completedCount == 1 ? "" : "s"));
    }

    private void applyFilter() {
        List<Payment> filtered = new ArrayList<>();
        for (Payment p : allPayments) {
            if ("ALL".equals(currentFilter) || currentFilter.equalsIgnoreCase(p.getStatus())) {
                filtered.add(p);
            }
        }

        adapter.setPayments(filtered);
        binding.layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showReceiptDialog(Payment payment) {
        String txnId = payment.getTransactionId() != null
                ? payment.getTransactionId()
                : (payment.getGatewayPaymentId() != null ? payment.getGatewayPaymentId() : payment.getOrderId());

        String receiptMsg = "Transaction Details:\n\n"
                + "• Amount Paid: " + payment.getFormattedAmount() + "\n"
                + "• Status: " + payment.getStatus() + "\n"
                + "• Payment Method: " + payment.getMethod() + "\n"
                + "• Transaction Reference: " + (txnId != null ? txnId : "N/A") + "\n"
                + "• Order ID: " + (payment.getOrderId() != null ? payment.getOrderId() : "N/A") + "\n"
                + "• Booking ID: " + (payment.getBookingId() != null ? payment.getBookingId() : "N/A") + "\n"
                + "• Currency: " + payment.getCurrency() + "\n"
                + (payment.isTestMode() ? "• Mode: Test / Sandbox\n" : "")
                + "\nGST Invoice Receipt generated by CycleGo.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Transaction Receipt")
                .setMessage(receiptMsg)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
