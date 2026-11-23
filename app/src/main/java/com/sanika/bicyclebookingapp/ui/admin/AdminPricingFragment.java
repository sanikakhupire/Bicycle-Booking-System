package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sanika.bicyclebookingapp.databinding.FragmentAdminPricingBinding;
import com.sanika.bicyclebookingapp.model.PricingConfig;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.viewmodel.PricingViewModel;

/**
 * Admin screen for configuring rental pricing rules, category multipliers,
 * penalties, security deposits, and GST tax settings.
 */
public class AdminPricingFragment extends Fragment {

    private FragmentAdminPricingBinding binding;
    private PricingViewModel pricingViewModel;
    private PricingConfig currentConfig;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminPricingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pricingViewModel = new ViewModelProvider(this).get(PricingViewModel.class);

        setupSwitches();
        setupSaveButton();
        loadPricingConfiguration();
    }

    private void setupSwitches() {
        binding.switchSecurityDeposit.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.layoutSecurityDeposit.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        binding.switchTax.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.layoutTaxPercentage.setVisibility(isChecked ? View.VISIBLE : View.GONE));
    }

    private void loadPricingConfiguration() {
        binding.progressBar.setVisibility(View.VISIBLE);

        pricingViewModel.getPricingConfig().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        currentConfig = resource.data;
                        populateFields(currentConfig);
                    }
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Could not load pricing: " + resource.message, Toast.LENGTH_SHORT).show();
                    // Load fallback sample config
                    currentConfig = PricingConfig.getDefaultSampleConfig();
                    populateFields(currentConfig);
                    break;
            }
        });
    }

    private void populateFields(PricingConfig config) {
        if (config == null || binding == null) return;

        binding.cardSampleNotice.setVisibility(config.isSampleConfig() ? View.VISIBLE : View.GONE);

        binding.etBaseFee.setText(String.format("%.2f", config.getBaseFeeInr()));
        binding.etRatePerMin.setText(String.format("%.2f", config.getRatePerMinuteInr()));
        binding.etRatePerHour.setText(String.format("%.2f", config.getRatePerHourInr()));
        binding.etMinCharge.setText(String.format("%.2f", config.getMinimumFareInr()));
        binding.etMaxDurationHours.setText(String.valueOf(Math.max(1, config.getMaximumDurationMinutes() / 60)));

        binding.etLateFeePerHour.setText(String.format("%.2f", config.getLateFeePerHourInr()));
        binding.etCancellationFee.setText(String.format("%.2f", config.getCancellationFeeInr()));

        binding.switchSecurityDeposit.setChecked(config.isSecurityDepositEnabled());
        binding.layoutSecurityDeposit.setVisibility(config.isSecurityDepositEnabled() ? View.VISIBLE : View.GONE);
        binding.etSecurityDeposit.setText(String.format("%.2f", config.getSecurityDepositInr()));

        binding.switchTax.setChecked(config.isTaxEnabled());
        binding.layoutTaxPercentage.setVisibility(config.isTaxEnabled() ? View.VISIBLE : View.GONE);
        binding.etTaxPercentage.setText(String.format("%.1f", config.getTaxPercentage()));

        binding.etStandardMultiplier.setText(String.format("%.2f", config.getStandardCategoryMultiplier()));
        binding.etElectricMultiplier.setText(String.format("%.2f", config.getElectricCategoryMultiplier()));
        binding.etMountainMultiplier.setText(String.format("%.2f", config.getMountainCategoryMultiplier()));
    }

    private void setupSaveButton() {
        binding.btnSavePricing.setOnClickListener(v -> validateAndConfirmSave());
    }

    private void validateAndConfirmSave() {
        String baseFeeStr = getText(binding.etBaseFee);
        String ratePerMinStr = getText(binding.etRatePerMin);
        String ratePerHourStr = getText(binding.etRatePerHour);
        String minChargeStr = getText(binding.etMinCharge);
        String maxDurStr = getText(binding.etMaxDurationHours);
        String lateFeeStr = getText(binding.etLateFeePerHour);
        String cancelFeeStr = getText(binding.etCancellationFee);

        if (TextUtils.isEmpty(baseFeeStr) || TextUtils.isEmpty(ratePerMinStr)
                || TextUtils.isEmpty(ratePerHourStr) || TextUtils.isEmpty(minChargeStr)) {
            Toast.makeText(requireContext(), "Please fill in all rental rate fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double baseFee, ratePerMin, ratePerHour, minCharge, lateFee, cancelFee;
        int maxDurationHours;

        try {
            baseFee = Double.parseDouble(baseFeeStr);
            ratePerMin = Double.parseDouble(ratePerMinStr);
            ratePerHour = Double.parseDouble(ratePerHourStr);
            minCharge = Double.parseDouble(minChargeStr);
            maxDurationHours = TextUtils.isEmpty(maxDurStr) ? 12 : Integer.parseInt(maxDurStr);
            lateFee = TextUtils.isEmpty(lateFeeStr) ? 50.0 : Double.parseDouble(lateFeeStr);
            cancelFee = TextUtils.isEmpty(cancelFeeStr) ? 10.0 : Double.parseDouble(cancelFeeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid number format in pricing fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (baseFee < 0 || ratePerMin < 0 || ratePerHour < 0 || minCharge < 0) {
            Toast.makeText(requireContext(), "Pricing values cannot be negative", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean depositEnabled = binding.switchSecurityDeposit.isChecked();
        double depositAmount = 0.0;
        if (depositEnabled) {
            try {
                depositAmount = Double.parseDouble(getText(binding.etSecurityDeposit));
            } catch (Exception e) {
                depositAmount = 100.0;
            }
        }

        boolean taxEnabled = binding.switchTax.isChecked();
        double taxPercent = 5.0;
        if (taxEnabled) {
            try {
                taxPercent = Double.parseDouble(getText(binding.etTaxPercentage));
                if (taxPercent < 0 || taxPercent > 50) {
                    Toast.makeText(requireContext(), "GST percentage must be between 0% and 50%", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                taxPercent = 5.0;
            }
        }

        double standardMul = 1.0, electricMul = 1.5, mountainMul = 1.25;
        try {
            standardMul = Double.parseDouble(getText(binding.etStandardMultiplier));
            electricMul = Double.parseDouble(getText(binding.etElectricMultiplier));
            mountainMul = Double.parseDouble(getText(binding.etMountainMultiplier));
        } catch (Exception ignored) {}

        // Build updated config
        PricingConfig updatedConfig = new PricingConfig();
        updatedConfig.setConfigId(PricingConfig.CONFIG_ID_DEFAULT);
        updatedConfig.setBaseFeeInr(baseFee);
        updatedConfig.setRatePerMinuteInr(ratePerMin);
        updatedConfig.setRatePerHourInr(ratePerHour);
        updatedConfig.setMinimumFareInr(minCharge);
        updatedConfig.setMaximumDurationMinutes(maxDurationHours * 60);
        updatedConfig.setLateFeePerHourInr(lateFee);
        updatedConfig.setCancellationFeeInr(cancelFee);
        updatedConfig.setSecurityDepositEnabled(depositEnabled);
        updatedConfig.setSecurityDepositInr(depositAmount);
        updatedConfig.setTaxEnabled(taxEnabled);
        updatedConfig.setTaxPercentage(taxPercent);
        updatedConfig.setStandardCategoryMultiplier(standardMul);
        updatedConfig.setElectricCategoryMultiplier(electricMul);
        updatedConfig.setMountainCategoryMultiplier(mountainMul);
        updatedConfig.setSampleConfig(false); // Officially configured by admin

        String confirmationMsg = String.format(
                "Confirm new India pricing configuration:\n\n" +
                "• Base Unlock Fee: %s\n" +
                "• Per-Minute Rate: %s/min\n" +
                "• Per-Hour Rate: %s/hr\n" +
                "• Minimum Ride Charge: %s\n" +
                "• Security Deposit: %s\n" +
                "• India GST Tax: %s\n\n" +
                "These rates will apply to all new bicycle rentals.",
                CurrencyUtil.formatInr(baseFee),
                CurrencyUtil.formatInr(ratePerMin),
                CurrencyUtil.formatInr(ratePerHour),
                CurrencyUtil.formatInr(minCharge),
                depositEnabled ? CurrencyUtil.formatInr(depositAmount) : "Disabled",
                taxEnabled ? String.format("%.1f%%", taxPercent) : "Disabled"
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Save Pricing Configuration")
                .setMessage(confirmationMsg)
                .setPositiveButton("Confirm & Save", (dialog, which) -> executeSave(updatedConfig))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeSave(PricingConfig config) {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSavePricing.setEnabled(false);

        pricingViewModel.savePricingConfig(config).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSavePricing.setEnabled(true);
                    currentConfig = resource.data;
                    populateFields(currentConfig);
                    Toast.makeText(requireContext(), "Pricing configuration saved successfully!", Toast.LENGTH_LONG).show();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSavePricing.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to save: " + resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private String getText(android.widget.EditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
