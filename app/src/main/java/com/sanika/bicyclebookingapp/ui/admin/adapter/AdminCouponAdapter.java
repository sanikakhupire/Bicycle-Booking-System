package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAdminCouponBinding;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Admin Coupon Management.
 */
public class AdminCouponAdapter extends RecyclerView.Adapter<AdminCouponAdapter.ViewHolder> {

    public interface OnCouponToggleListener {
        void onToggle(Coupon coupon, boolean isChecked);
    }

    private final List<Coupon> coupons = new ArrayList<>();
    private final OnCouponToggleListener listener;

    public AdminCouponAdapter(OnCouponToggleListener listener) {
        this.listener = listener;
    }

    public void setCoupons(List<Coupon> newCoupons) {
        this.coupons.clear();
        if (newCoupons != null) {
            this.coupons.addAll(newCoupons);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminCouponBinding binding = ItemAdminCouponBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(coupons.get(position));
    }

    @Override
    public int getItemCount() {
        return coupons.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminCouponBinding binding;

        ViewHolder(ItemAdminCouponBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Coupon coupon) {
            binding.tvCouponCode.setText(coupon.getCode() != null ? coupon.getCode() : "N/A");
            binding.tvCouponDescription.setText(coupon.getDescription() != null ? coupon.getDescription() : "");

            String type = coupon.getDiscountType() != null ? coupon.getDiscountType() : Coupon.TYPE_FLAT;
            String valStr;
            if (Coupon.TYPE_PERCENTAGE.equalsIgnoreCase(type)) {
                valStr = coupon.getDiscountPercentage() + "% Off";
            } else {
                valStr = CurrencyUtil.formatInrFromPaise(coupon.getDiscountValuePaise());
            }

            String rules = "Type: " + type + " • Discount: " + valStr
                    + " • Min Fare: " + CurrencyUtil.formatInrFromPaise(coupon.getMinRideFarePaise());
            binding.tvCouponRules.setText(rules);

            binding.switchCouponActive.setOnCheckedChangeListener(null);
            binding.switchCouponActive.setChecked(coupon.isActive());
            binding.switchCouponActive.setOnCheckedChangeListener((btn, isChecked) -> {
                if (listener != null) listener.onToggle(coupon, isChecked);
            });
        }
    }
}
