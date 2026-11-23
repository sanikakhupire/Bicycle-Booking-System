package com.sanika.bicyclebookingapp.ui.payment;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemPaymentTransactionBinding;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for customer payment transaction history.
 */
public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
    }

    private final List<Payment> payments = new ArrayList<>();
    private final OnPaymentClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);

    public PaymentHistoryAdapter(OnPaymentClickListener listener) {
        this.listener = listener;
    }

    public void setPayments(List<Payment> newPayments) {
        this.payments.clear();
        if (newPayments != null) {
            this.payments.addAll(newPayments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPaymentTransactionBinding binding = ItemPaymentTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Payment payment = payments.get(position);
        holder.bind(payment);
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPaymentTransactionBinding binding;

        ViewHolder(ItemPaymentTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Payment payment) {
            // Amount in INR
            binding.tvPaymentAmount.setText(payment.getFormattedAmount());

            // Status Badge
            String status = payment.getStatus() != null ? payment.getStatus() : Payment.STATUS_PENDING;
            binding.tvPaymentStatus.setText(status);

            switch (status) {
                case Payment.STATUS_SUCCESS:
                    binding.tvPaymentStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                    binding.tvPaymentStatus.setTextColor(Color.parseColor("#2E7D32"));
                    break;
                case Payment.STATUS_REFUNDED:
                    binding.tvPaymentStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                    binding.tvPaymentStatus.setTextColor(Color.parseColor("#0D47A1"));
                    break;
                case Payment.STATUS_FAILED:
                    binding.tvPaymentStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                    binding.tvPaymentStatus.setTextColor(Color.parseColor("#C62828"));
                    break;
                default: // PENDING
                    binding.tvPaymentStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                    binding.tvPaymentStatus.setTextColor(Color.parseColor("#E65100"));
                    break;
            }

            // Method Badge
            String method = payment.getMethod() != null ? payment.getMethod() : "UPI";
            binding.tvPaymentMethod.setText(method.replace("_", " "));

            // Description
            String desc = payment.getDescription() != null ? payment.getDescription() : "Ride Fare";
            binding.tvPaymentDesc.setText(desc);

            // Transaction ID
            String txnId = payment.getTransactionId() != null
                    ? payment.getTransactionId()
                    : (payment.getGatewayPaymentId() != null ? payment.getGatewayPaymentId() : payment.getOrderId());
            binding.tvTransactionId.setText("Txn: " + (txnId != null ? txnId : "N/A"));

            // Date
            if (payment.getPaidAt() != null) {
                binding.tvPaymentDate.setText(dateFormat.format(payment.getPaidAt()));
            } else if (payment.getCreatedAt() != null) {
                binding.tvPaymentDate.setText(dateFormat.format(payment.getCreatedAt()));
            } else {
                binding.tvPaymentDate.setText("Recent");
            }

            // Refund Info
            if (Payment.STATUS_REFUNDED.equalsIgnoreCase(status)) {
                binding.tvRefundInfo.setVisibility(View.VISIBLE);
                double refAmount = payment.getRefundAmountInr() > 0 ? payment.getRefundAmountInr() : payment.getAmount();
                binding.tvRefundInfo.setText("✓ Refund of " + CurrencyUtil.formatInr(refAmount) + " processed to original source");
            } else {
                binding.tvRefundInfo.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPaymentClick(payment);
            });
        }
    }
}
