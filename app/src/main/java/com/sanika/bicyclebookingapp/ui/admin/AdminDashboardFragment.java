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

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminDashboardBinding;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.AdminViewModel;

import java.util.Map;

/**
 * Executive Admin Dashboard displaying real-time metrics, revenue, fleet telemetry,
 * and centralized module access for CycleGo India.
 */
public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private AdminViewModel adminViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupModuleNavigation();
        loadDashboardMetrics();
    }

    private void loadDashboardMetrics() {
        binding.progressBarDashboard.setVisibility(View.VISIBLE);

        adminViewModel.getDashboardMetrics().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarDashboard.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                bindMetrics(resource.data);
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Telemetry sync error: " + resource.message, Toast.LENGTH_SHORT).show();
                if (resource.data != null) {
                    bindMetrics(resource.data);
                }
            }
        });
    }

    private void bindMetrics(Map<String, Object> data) {
        // Revenue (INR)
        String todayRev = (String) data.getOrDefault("todayRevenueFormatted", "₹0.00");
        String monthRev = (String) data.getOrDefault("monthlyRevenueFormatted", "₹0.00");
        binding.tvDashTodayRevenue.setText(todayRev);
        binding.tvDashMonthRevenue.setText(monthRev);

        // Fleet Counts
        binding.tvDashTotalBikes.setText(String.valueOf(data.getOrDefault("totalBicycles", 0)));
        binding.tvDashAvailableBikes.setText(String.valueOf(data.getOrDefault("availableBicycles", 0)));
        binding.tvDashInUseBikes.setText(String.valueOf(data.getOrDefault("inUseBicycles", 0)));
        binding.tvDashMaintenanceBikes.setText(String.valueOf(data.getOrDefault("maintenanceBicycles", 0)));

        // Booking Counts
        binding.tvDashTodayBookings.setText(String.valueOf(data.getOrDefault("todayBookings", 0)));
        binding.tvDashActiveRides.setText(String.valueOf(data.getOrDefault("activeRides", 0)));
        binding.tvDashCompletedRides.setText(String.valueOf(data.getOrDefault("completedRides", 0)));
        binding.tvDashCancelledBookings.setText(String.valueOf(data.getOrDefault("cancelledBookings", 0)));

        // Customer & Grievances
        binding.tvDashTotalCustomers.setText(String.valueOf(data.getOrDefault("totalCustomers", 0)));
        binding.tvDashOpenComplaints.setText(String.valueOf(data.getOrDefault("openTicketsCount", 0)));

        // Popular Station & Bicycle
        String popStation = (String) data.getOrDefault("popularStation", "None");
        String topBike = (String) data.getOrDefault("mostRentedBicycle", "None");
        binding.tvDashPopularStation.setText("★ Most Popular Station: " + popStation);
        binding.tvDashTopBike.setText("🚲 Most Rented Bicycle: " + topBike);
    }

    private void setupModuleNavigation() {
        // Quick Cards
        binding.cardCustomers.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminCustomersFragment));

        binding.cardSupport.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminSupportFragment));

        // Module Buttons
        binding.btnNavCustomers.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminCustomersFragment));

        binding.btnNavBookings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminBookingsFragment));

        binding.btnNavBicycles.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.manageBicyclesFragment));

        binding.btnNavMaintenance.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminMaintenanceFragment));

        binding.cardDashMaintenance.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminMaintenanceFragment));

        binding.btnNavStations.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.manageStationsFragment));

        binding.btnNavPayments.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminPaymentsFragment));

        binding.btnNavPricing.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminPricingFragment));

        binding.btnNavCoupons.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminCouponsFragment));

        binding.btnNavSupport.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminSupportFragment));

        binding.btnNavSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminSettingsFragment));

        binding.btnNavNotifications.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminNotificationsFragment));

        binding.cardRevenueHero.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminAnalyticsFragment));

        binding.btnNavAnalytics.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.adminAnalyticsFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
