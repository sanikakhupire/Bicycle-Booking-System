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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentNotificationCenterBinding;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.viewmodel.NotificationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer screen displaying persistent in-app notifications, filterable categories,
 * unread state toggling, and deep linking into rides, payments, and bookings.
 */
public class NotificationCenterFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private FragmentNotificationCenterBinding binding;
    private NotificationViewModel notificationViewModel;
    private NotificationAdapter adapter;

    private final List<AppNotification> allNotifications = new ArrayList<>();
    private String currentFilter = "ALL";
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationCenterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        setupRecyclerView();
        setupFilters();
        setupActions();
        loadNotifications();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupNotifFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterUnread) {
                currentFilter = "UNREAD";
            } else if (checkedId == R.id.chipFilterBookings) {
                currentFilter = "BOOKINGS";
            } else if (checkedId == R.id.chipFilterPayments) {
                currentFilter = "PAYMENTS";
            } else if (checkedId == R.id.chipFilterAlerts) {
                currentFilter = "ALERTS";
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void setupActions() {
        binding.btnMarkAllRead.setOnClickListener(v -> {
            if (!userId.isEmpty()) {
                notificationViewModel.markAllAsRead(userId);
                Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnNotificationSettings.setOnClickListener(v -> {
            try {
                Navigation.findNavController(requireView())
                        .navigate(R.id.notificationPreferencesFragment);
            } catch (Exception ignored) {}
        });
    }

    private void loadNotifications() {
        if (userId.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.tvEmpty.setText("Please sign in to view your notifications");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        notificationViewModel.getCustomerNotifications(userId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);

            if (resource.isSuccess() && resource.data != null) {
                allNotifications.clear();
                allNotifications.addAll(resource.data);
                applyFilter();
            } else if (resource.isError()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.tvEmpty.setText("Error loading notifications: " + resource.message);
            }
        });
    }

    private void applyFilter() {
        List<AppNotification> filtered = new ArrayList<>();
        for (AppNotification notif : allNotifications) {
            switch (currentFilter) {
                case "UNREAD":
                    if (!notif.isRead()) filtered.add(notif);
                    break;
                case "BOOKINGS":
                    if (AppNotification.CHANNEL_BOOKINGS.equals(notif.getChannelId())
                            || AppNotification.CHANNEL_RIDES.equals(notif.getChannelId())) {
                        filtered.add(notif);
                    }
                    break;
                case "PAYMENTS":
                    if (AppNotification.CHANNEL_PAYMENTS.equals(notif.getChannelId())) {
                        filtered.add(notif);
                    }
                    break;
                case "ALERTS":
                    if (AppNotification.CHANNEL_ALERTS.equals(notif.getChannelId())) {
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

        // Mark as read
        if (!notification.isRead()) {
            notificationViewModel.markAsRead(notification.getNotificationId());
        }

        // Deep linking navigation
        String target = notification.getTargetType();
        String id = notification.getTargetId();

        try {
            if (AppNotification.TARGET_PAYMENT.equalsIgnoreCase(target)
                    || AppNotification.TARGET_REFUND.equalsIgnoreCase(target)) {
                Navigation.findNavController(requireView()).navigate(R.id.paymentHistoryFragment);
            } else if (AppNotification.TARGET_MAINTENANCE.equalsIgnoreCase(target)) {
                Bundle args = new Bundle();
                args.putString("bicycleId", id != null ? id : "");
                Navigation.findNavController(requireView()).navigate(R.id.reportDamageFragment, args);
            } else if (AppNotification.TARGET_RIDE.equalsIgnoreCase(target)) {
                // If ride is active, go to active ride; otherwise ride history
                if (id != null && !id.isEmpty()) {
                    Bundle args = new Bundle();
                    args.putString("bookingId", id);
                    Navigation.findNavController(requireView()).navigate(R.id.activeRideFragment, args);
                } else {
                    Navigation.findNavController(requireView()).navigate(R.id.historyFragment);
                }
            } else if (AppNotification.TARGET_BOOKING.equalsIgnoreCase(target)) {
                Navigation.findNavController(requireView()).navigate(R.id.historyFragment);
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
