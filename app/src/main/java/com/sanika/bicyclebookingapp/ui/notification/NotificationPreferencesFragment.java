package com.sanika.bicyclebookingapp.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.databinding.FragmentNotificationPreferencesBinding;
import com.sanika.bicyclebookingapp.model.NotificationPreferences;
import com.sanika.bicyclebookingapp.viewmodel.NotificationViewModel;

/**
 * Screen allowing users to configure push alert preferences and communication channels.
 */
public class NotificationPreferencesFragment extends Fragment {

    private FragmentNotificationPreferencesBinding binding;
    private NotificationViewModel notificationViewModel;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationPreferencesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        loadPreferences();
        setupActions();
    }

    private void loadPreferences() {
        if (userId.isEmpty()) return;

        notificationViewModel.getPreferences(userId).observe(getViewLifecycleOwner(), prefs -> {
            if (binding == null || prefs == null) return;
            binding.switchPushEnabled.setChecked(prefs.isPushEnabled());
            binding.switchBookingAlerts.setChecked(prefs.isBookingAlerts());
            binding.switchRideTrackingAlerts.setChecked(prefs.isRideTrackingAlerts());
            binding.switchBillingAlerts.setChecked(prefs.isBillingAlerts());
            binding.switchPromotions.setChecked(prefs.isPromotionalAlerts());
            binding.switchMaintenanceAlerts.setChecked(prefs.isMaintenanceAlerts());
            binding.switchSupportAlerts.setChecked(prefs.isSupportAlerts());
        });
    }

    private void setupActions() {
        binding.btnSavePreferences.setOnClickListener(v -> {
            if (userId.isEmpty()) return;

            NotificationPreferences prefs = new NotificationPreferences();
            prefs.setPushEnabled(binding.switchPushEnabled.isChecked());
            prefs.setBookingAlerts(binding.switchBookingAlerts.isChecked());
            prefs.setRideTrackingAlerts(binding.switchRideTrackingAlerts.isChecked());
            prefs.setBillingAlerts(binding.switchBillingAlerts.isChecked());
            prefs.setPromotionalAlerts(binding.switchPromotions.isChecked());
            prefs.setMaintenanceAlerts(binding.switchMaintenanceAlerts.isChecked());
            prefs.setSupportAlerts(binding.switchSupportAlerts.isChecked());

            notificationViewModel.savePreferences(userId, prefs);
            Toast.makeText(requireContext(), "Preferences saved successfully", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
