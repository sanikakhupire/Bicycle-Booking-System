package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminCouponsBinding;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminCouponAdapter;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.AdminViewModel;

import java.util.List;

/**
 * Coupon & Promotion Management screen for Administrators.
 */
public class AdminCouponsFragment extends Fragment {

    private FragmentAdminCouponsBinding binding;
    private AdminViewModel adminViewModel;
    private AdminCouponAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminCouponsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupRecyclerView();
        loadCoupons();

        binding.fabAddCoupon.setOnClickListener(v -> showCreateCouponDialog());
    }

    private void setupRecyclerView() {
        adapter = new AdminCouponAdapter((coupon, isChecked) -> {
            coupon.setActive(isChecked);
            adminViewModel.saveCoupon(coupon,
                    () -> Toast.makeText(requireContext(), "Coupon status updated!", Toast.LENGTH_SHORT).show(),
                    err -> Toast.makeText(requireContext(), "Update failed: " + err, Toast.LENGTH_SHORT).show());
        });
        binding.rvCoupons.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCoupons.setAdapter(adapter);
    }

    private void loadCoupons() {
        binding.progressBarCoupons.setVisibility(View.VISIBLE);

        adminViewModel.getAllCoupons().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarCoupons.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                List<Coupon> list = resource.data;
                adapter.setCoupons(list);
                binding.tvEmptyCoupons.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateCouponDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_coupon, null);
        EditText etCode = dialogView.findViewById(R.id.etCouponCode);
        EditText etDiscount = dialogView.findViewById(R.id.etDiscountValue);
        EditText etMinFare = dialogView.findViewById(R.id.etMinRideFare);
        EditText etDesc = dialogView.findViewById(R.id.etCouponDesc);
        RadioGroup rgType = dialogView.findViewById(R.id.rgDiscountType);

        new AlertDialog.Builder(requireContext())
                .setTitle("Create Promotional Coupon")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String code = etCode.getText().toString().trim().toUpperCase();
                    String discountStr = etDiscount.getText().toString().trim();
                    String minFareStr = etMinFare.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (TextUtils.isEmpty(code) || TextUtils.isEmpty(discountStr)) {
                        Toast.makeText(requireContext(), "Coupon code and discount are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean isPercent = rgType.getCheckedRadioButtonId() == R.id.rbPercentage;
                    long discountVal = 1000L;
                    try {
                        discountVal = isPercent ? Long.parseLong(discountStr) : CurrencyUtil.toPaise(Double.parseDouble(discountStr));
                    } catch (Exception ignored) {}

                    long minFareVal = 2000L;
                    try {
                        minFareVal = CurrencyUtil.toPaise(Double.parseDouble(minFareStr));
                    } catch (Exception ignored) {}

                    Coupon coupon = new Coupon();
                    coupon.setCode(code);
                    coupon.setDiscountType(isPercent ? Coupon.TYPE_PERCENTAGE : Coupon.TYPE_FLAT);
                    if (isPercent) {
                        coupon.setDiscountPercentage(discountVal);
                        coupon.setMaxDiscountPaise(5000L); // Capped at ₹50
                    } else {
                        coupon.setDiscountValuePaise(discountVal);
                    }
                    coupon.setMinRideFarePaise(minFareVal);
                    coupon.setDescription(!desc.isEmpty() ? desc : ("Get " + (isPercent ? discountVal + "%" : CurrencyUtil.formatInrFromPaise(discountVal)) + " off"));
                    coupon.setActive(true);

                    adminViewModel.saveCoupon(coupon,
                            () -> {
                                Toast.makeText(requireContext(), "Coupon " + code + " created!", Toast.LENGTH_SHORT).show();
                                loadCoupons();
                            },
                            err -> Toast.makeText(requireContext(), "Failed to create: " + err, Toast.LENGTH_LONG).show());
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
