package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sanika.bicyclebookingapp.databinding.FragmentAdminSettingsBinding;
import com.sanika.bicyclebookingapp.model.AdminSettings;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.AdminViewModel;

/**
 * Admin screen for configuring business identity, operational hours,
 * geofence boundaries, and telemetry alert sensitivity.
 */
public class AdminSettingsFragment extends Fragment {

    private FragmentAdminSettingsBinding binding;
    private AdminViewModel adminViewModel;
    private AdminSettings currentSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        loadSettings();
        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        adminViewModel.getAdminSettings().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                currentSettings = resource.data;
                binding.etBusinessName.setText(currentSettings.getBusinessName());
                binding.etOperatingHours.setText(currentSettings.getOperatingHours());
                binding.etEmergencyPhone.setText(currentSettings.getEmergencyPhone());
                binding.etSupportEmail.setText(currentSettings.getSupportEmail());
                binding.etGeofenceRadius.setText(String.valueOf(currentSettings.getGeofenceRadiusMeters()));
                binding.etHoldMinutes.setText(String.valueOf(currentSettings.getAutoExpireHoldMinutes()));
                binding.switchMaintenanceAlerts.setChecked(currentSettings.isMaintenanceAlertsEnabled());
                binding.switchAutoRefunds.setChecked(currentSettings.isAutomatedRefundsEnabled());
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Failed to load settings: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSettings() {
        if (currentSettings == null) {
            currentSettings = AdminSettings.createDefaultSettings();
        }

        String name = binding.etBusinessName.getText() != null ? binding.etBusinessName.getText().toString().trim() : "";
        String hours = binding.etOperatingHours.getText() != null ? binding.etOperatingHours.getText().toString().trim() : "";
        String phone = binding.etEmergencyPhone.getText() != null ? binding.etEmergencyPhone.getText().toString().trim() : "";
        String email = binding.etSupportEmail.getText() != null ? binding.etSupportEmail.getText().toString().trim() : "";
        String radiusStr = binding.etGeofenceRadius.getText() != null ? binding.etGeofenceRadius.getText().toString().trim() : "100";
        String holdStr = binding.etHoldMinutes.getText() != null ? binding.etHoldMinutes.getText().toString().trim() : "10";

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), "Business name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        int radius = 100;
        try {
            radius = Integer.parseInt(radiusStr);
        } catch (Exception ignored) {}

        int holdMins = 10;
        try {
            holdMins = Integer.parseInt(holdStr);
        } catch (Exception ignored) {}

        currentSettings.setBusinessName(name);
        currentSettings.setOperatingHours(hours);
        currentSettings.setEmergencyPhone(phone);
        currentSettings.setSupportEmail(email);
        currentSettings.setGeofenceRadiusMeters(radius);
        currentSettings.setAutoExpireHoldMinutes(holdMins);
        currentSettings.setMaintenanceAlertsEnabled(binding.switchMaintenanceAlerts.isChecked());
        currentSettings.setAutomatedRefundsEnabled(binding.switchAutoRefunds.isChecked());

        binding.btnSaveSettings.setEnabled(false);
        binding.btnSaveSettings.setText("Saving...");

        adminViewModel.saveAdminSettings(currentSettings,
                () -> {
                    if (binding != null) {
                        binding.btnSaveSettings.setEnabled(true);
                        binding.btnSaveSettings.setText("Save Operations Settings");
                    }
                    Toast.makeText(requireContext(), "Operations settings updated successfully!", Toast.LENGTH_SHORT).show();
                },
                err -> {
                    if (binding != null) {
                        binding.btnSaveSettings.setEnabled(true);
                        binding.btnSaveSettings.setText("Save Operations Settings");
                    }
                    Toast.makeText(requireContext(), "Save failed: " + err, Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
