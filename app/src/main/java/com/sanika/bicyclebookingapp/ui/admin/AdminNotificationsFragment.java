package com.sanika.bicyclebookingapp.ui.admin;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminNotificationsBinding;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.ui.notification.NotificationAdapter;
import com.sanika.bicyclebookingapp.viewmodel.NotificationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin Operational Notification Center.
 * Aggregates real-time business and fleet events with one-click deep linking into
 * bookings, active rides, maintenance tickets, payment failures, and customer grievances.
 */
public class AdminNotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private FragmentAdminNotificationsBinding binding;
    private NotificationViewModel notificationViewModel;
    private NotificationAdapter adapter;

    private final List<AppNotification> adminAlerts = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        setupRecyclerView();
        setupFilters();
        setupActions();
        loadAlerts();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this);
        binding.rvAdminNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdminNotifications.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupAdminAlerts.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAdminUnread) {
                currentFilter = "UNREAD";
            } else if (checkedId == R.id.chipAdminBookingsRides) {
                currentFilter = "BOOKINGS_RIDES";
            } else if (checkedId == R.id.chipAdminRepairs) {
                currentFilter = "REPAIRS";
            } else if (checkedId == R.id.chipAdminFinanceSupport) {
                currentFilter = "FINANCE_SUPPORT";
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void setupActions() {
        binding.btnAdminMarkAllRead.setOnClickListener(v -> {
            notificationViewModel.markAllAsRead("ADMIN");
            Toast.makeText(requireContext(), "All operational alerts marked as read", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAlerts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        notificationViewModel.getAdminNotifications().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);

            if (resource.isSuccess() && resource.data != null) {
                adminAlerts.clear();
                adminAlerts.addAll(resource.data);
                applyFilter();
            } else if (resource.isError()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.tvEmpty.setText("Error loading alerts: " + resource.message);
            }
        });
    }

    private void applyFilter() {
        List<AppNotification> filtered = new ArrayList<>();
        for (AppNotification notif : adminAlerts) {
            String type = notif.getType() != null ? notif.getType() : "";
            switch (currentFilter) {
                case "UNREAD":
                    if (!notif.isRead()) filtered.add(notif);
                    break;
                case "BOOKINGS_RIDES":
                    if (type.contains("BOOKING") || type.contains("RIDE")) {
                        filtered.add(notif);
                    }
                    break;
                case "REPAIRS":
                    if (type.contains("DAMAGE") || type.contains("MAINTENANCE") || type.contains("INVENTORY")) {
                        filtered.add(notif);
                    }
                    break;
                case "FINANCE_SUPPORT":
                    if (type.contains("PAYMENT") || type.contains("COMPLAINT") || type.contains("REFUND")) {
                        filtered.add(notif);
                    }
                    break;
                case "ALL":
                default:
                    filtered.add(notif);
                    break;
            }
        }

        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onNotificationClick(AppNotification notification) {
        if (notification == null) return;

        if (!notification.isRead()) {
            notificationViewModel.markAsRead(notification.getNotificationId());
        }

        String type = notification.getType() != null ? notification.getType() : "";

        try {
            if (AppNotification.TYPE_ADMIN_DAMAGE_REPORT.equals(type)
                    || AppNotification.TYPE_ADMIN_MAINTENANCE_REQUEST.equals(type)) {
                Navigation.findNavController(requireView()).navigate(R.id.adminMaintenanceFragment);
            } else if (AppNotification.TYPE_ADMIN_NEW_BOOKING.equals(type)
                    || AppNotification.TYPE_ADMIN_OVERDUE_RIDE.equals(type)) {
                Navigation.findNavController(requireView()).navigate(R.id.adminBookingsFragment);
            } else if (AppNotification.TYPE_ADMIN_ACTIVE_RIDE.equals(type)) {
                Navigation.findNavController(requireView()).navigate(R.id.activeRidesFragment);
            } else if (AppNotification.TYPE_ADMIN_PAYMENT_FAILURE.equals(type)) {
                Navigation.findNavController(requireView()).navigate(R.id.adminPaymentsFragment);
            } else if (AppNotification.TYPE_ADMIN_CUSTOMER_COMPLAINT.equals(type)) {
                Navigation.findNavController(requireView()).navigate(R.id.adminSupportFragment);
            } else if (AppNotification.TYPE_ADMIN_LOW_INVENTORY.equals(type)) {
                Navigation.findNavController(requireView()).navigate(R.id.manageStationsFragment);
            } else if (AppNotification.TYPE_ADMIN_SUSPICIOUS_ACTIVITY.equals(type)) {
                Navigation.findNavController(requireView()).navigate(R.id.trackBicyclesFragment);
            } else {
                Toast.makeText(requireContext(), notification.getTitle(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), notification.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
