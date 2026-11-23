package com.sanika.bicyclebookingapp.ui.loyalty;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemLoyaltyTransactionBinding;
import com.sanika.bicyclebookingapp.model.LoyaltyTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Customer Loyalty Points Transaction history ledger.
 */
public class LoyaltyTransactionAdapter extends RecyclerView.Adapter<LoyaltyTransactionAdapter.ViewHolder> {

    private final List<LoyaltyTransaction> transactions = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);

    public void setTransactions(List<LoyaltyTransaction> list) {
        this.transactions.clear();
        if (list != null) {
            this.transactions.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoyaltyTransactionBinding binding = ItemLoyaltyTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(transactions.get(position));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLoyaltyTransactionBinding binding;

        ViewHolder(ItemLoyaltyTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(LoyaltyTransaction txn) {
            binding.tvTxnDescription.setText(txn.getDescription() != null ? txn.getDescription() : "Loyalty Transaction");

            if (txn.getTimestamp() != null) {
                binding.tvTxnTime.setText(dateFormat.format(txn.getTimestamp()));
            } else {
                binding.tvTxnTime.setText("");
            }

            long pts = txn.getPoints();
            if (pts > 0) {
                binding.tvTxnPoints.setText("+" + pts + " pts");
                binding.tvTxnPoints.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                binding.tvTxnPoints.setText(pts + " pts");
                binding.tvTxnPoints.setTextColor(Color.parseColor("#C62828"));
            }
        }
    }
}
