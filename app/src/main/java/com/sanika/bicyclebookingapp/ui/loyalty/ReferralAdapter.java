package com.sanika.bicyclebookingapp.ui.loyalty;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemReferralBinding;
import com.sanika.bicyclebookingapp.model.Referral;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying invited friends and their referral qualification status.
 */
public class ReferralAdapter extends RecyclerView.Adapter<ReferralAdapter.ViewHolder> {

    private final List<Referral> referrals = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    public void setReferrals(List<Referral> list) {
        this.referrals.clear();
        if (list != null) {
            this.referrals.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReferralBinding binding = ItemReferralBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(referrals.get(position));
    }

    @Override
    public int getItemCount() {
        return referrals.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReferralBinding binding;

        ViewHolder(ItemReferralBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Referral ref) {
            binding.tvRefereeName.setText(ref.getRefereeName() != null ? ref.getRefereeName() : "Invited Friend");

            if (ref.getCreatedAt() != null) {
                binding.tvReferralDate.setText("Invited on " + dateFormat.format(ref.getCreatedAt()));
            } else {
                binding.tvReferralDate.setText("");
            }

            if (Referral.STATUS_REWARDED.equalsIgnoreCase(ref.getStatus()) || Referral.STATUS_QUALIFIED.equalsIgnoreCase(ref.getStatus())) {
                binding.tvReferralStatus.setText("✓ REWARDED (+" + ref.getRewardPoints() + " pts)");
                binding.tvReferralStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                binding.tvReferralStatus.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                binding.tvReferralStatus.setText("PENDING (Awaiting 1st Ride)");
                binding.tvReferralStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                binding.tvReferralStatus.setTextColor(Color.parseColor("#E65100"));
            }
        }
    }
}
