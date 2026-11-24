package com.sanika.bicyclebookingapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminAnalyticsBinding;
import com.sanika.bicyclebookingapp.model.BusinessAnalyticsData;
import com.sanika.bicyclebookingapp.service.ReportExporterService;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.AnalyticsViewModel;

import java.io.File;
import java.util.Locale;

/**
 * Fragment displaying Business Analytics, Financial Intelligence, Fleet Availability,
 * and Multi-Format CSV / Printable Reporting for Admin.
 */
public class AdminAnalyticsFragment extends Fragment {

    private FragmentAdminAnalyticsBinding binding;
    private AnalyticsViewModel viewModel;
    private BusinessAnalyticsData currentAnalytics;

    private final ReportExporterService.ReportType[] reportTypes = {
            ReportExporterService.ReportType.BOOKING_REPORT,
            ReportExporterService.ReportType.RIDE_REPORT,
            ReportExporterService.ReportType.REVENUE_REPORT,
            ReportExporterService.ReportType.PAYMENT_REPORT,
            ReportExporterService.ReportType.REFUND_REPORT,
            ReportExporterService.ReportType.FLEET_REPORT,
            ReportExporterService.ReportType.MAINTENANCE_REPORT,
            ReportExporterService.ReportType.STATION_REPORT,
            ReportExporterService.ReportType.CUSTOMER_REPORT
    };

    private final String[] reportLabels = {
            "1. Bookings Report (All reservations)",
            "2. Rides Report (Completed & live)",
            "3. Revenue & Invoicing Report (PAISE / INR)",
            "4. Payment Transactions Report (UPI/Cards)",
            "5. Refund Transactions Report",
            "6. Bicycle Fleet Report (Utilization & costs)",
            "7. Maintenance & Repairs Report",
            "8. Station Inventory & Flow Report",
            "9. Customer & User Report (Retention & spending)"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        setupDateFilterChips();
        setupReportSpinner();
        setupExportActions();
        observeAnalytics();
        observeExportStatus();
    }

    private void setupDateFilterChips() {
        binding.chipGroupDateFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipToday) {
                viewModel.setPresetFilter(AnalyticsViewModel.DateFilterPreset.TODAY);
            } else if (checkedId == R.id.chipLast7Days) {
                viewModel.setPresetFilter(AnalyticsViewModel.DateFilterPreset.LAST_7_DAYS);
            } else if (checkedId == R.id.chipThisMonth) {
                viewModel.setPresetFilter(AnalyticsViewModel.DateFilterPreset.THIS_MONTH);
            } else if (checkedId == R.id.chipThisYear) {
                viewModel.setPresetFilter(AnalyticsViewModel.DateFilterPreset.THIS_YEAR);
            } else {
                viewModel.setPresetFilter(AnalyticsViewModel.DateFilterPreset.ALL_TIME);
            }
        });
    }

    private void setupReportSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                reportLabels
        );
        binding.spinnerReportType.setAdapter(adapter);
    }

    private void setupExportActions() {
        binding.btnExportCsv.setOnClickListener(v -> {
            int selectedIdx = binding.spinnerReportType.getSelectedItemPosition();
            if (selectedIdx >= 0 && selectedIdx < reportTypes.length) {
                ReportExporterService.ReportType type = reportTypes[selectedIdx];
                viewModel.exportReport(type);
            }
        });

        binding.btnShareSummary.setOnClickListener(v -> {
            if (currentAnalytics != null) {
                String summary = viewModel.generatePrintableSummary(currentAnalytics);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CycleGo Executive Business Report");
                shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
                startActivity(Intent.createChooser(shareIntent, "Share Executive Report via"));
            } else {
                Toast.makeText(requireContext(), "Analytics data not yet loaded", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeAnalytics() {
        viewModel.getAnalytics().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;

            if (resource.status == Resource.Status.LOADING) {
                binding.progressBarAnalytics.setVisibility(View.VISIBLE);
            } else {
                binding.progressBarAnalytics.setVisibility(View.GONE);
            }

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                currentAnalytics = resource.data;
                bindData(currentAnalytics);
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Failed to sync analytics: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeExportStatus() {
        viewModel.getExportStatus().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.status == Resource.Status.LOADING) {
                Toast.makeText(requireContext(), "Generating RFC-4180 CSV export...", Toast.LENGTH_SHORT).show();
            } else if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                File file = resource.data;
                Toast.makeText(requireContext(), "Report exported successfully: " + file.getName(), Toast.LENGTH_LONG).show();
                Intent shareIntent = viewModel.createShareIntent(file, file.getName());
                startActivity(shareIntent);
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Export failed: " + resource.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindData(BusinessAnalyticsData d) {
        // Subtitle with active period
        binding.tvAnalyticsSubtitle.setText(String.format("Period: %s | Verified fleet & financial telemetry (₹)", viewModel.getCurrentFilterLabel()));

        // Financial
        binding.tvNetRevenue.setText(d.getFormattedNetRevenue());
        binding.tvGrossInvoiced.setText(d.getFormattedGrossInvoiced());
        binding.tvPaymentsReceived.setText(d.getFormattedPaymentsReceived());
        binding.tvTotalRefunds.setText(d.getFormattedTotalRefunds());
        binding.tvAvgRevPerRide.setText(d.getFormattedAvgRevenuePerRide());
        binding.tvPendingAmount.setText(d.getFormattedPendingAmount());

        // Operational
        binding.tvTotalRides.setText(String.valueOf(d.getTotalCompletedRides()));
        binding.tvActiveRides.setText(String.valueOf(d.getActiveRides()));
        binding.tvFleetUtilization.setText(String.format(Locale.ENGLISH, "%.1f%%", d.getFleetUtilizationRate()));
        binding.tvFleetAvailability.setText(String.format(Locale.ENGLISH, "%.1f%%", d.getFleetAvailabilityRate()));
        binding.tvAvgDuration.setText(d.getAvgRideDurationMinutes() + " mins");
        binding.tvAvgDistance.setText(String.format(Locale.ENGLISH, "%.2f km", d.getAvgDistanceKm()));

        // Fleet Health
        binding.tvTotalBikes.setText(String.valueOf(d.getTotalBicycles()));
        binding.tvAvailableBikes.setText(String.valueOf(d.getAvailableBicycles()));
        binding.tvInUseBikes.setText(String.valueOf(d.getInUseBicycles()));
        binding.tvMaintBikes.setText(String.valueOf(d.getMaintenanceBicycles()));
        binding.tvMaintTickets.setText(String.valueOf(d.getTotalMaintenanceRecords()));
        binding.tvMaintCost.setText(d.getFormattedMaintenanceCosts());

        // Customer
        binding.tvActiveCustomers.setText(String.valueOf(d.getActiveCustomers()));
        binding.tvRetentionRate.setText(String.format(Locale.ENGLISH, "%.1f%%", d.getCustomerRetentionRate()));
        binding.tvAvgRating.setText(String.format(Locale.ENGLISH, "%.1f ★", d.getAvgCustomerRating()));

        // Rankings
        String topBike = "None";
        if (d.getMostRentedBicycles() != null && !d.getMostRentedBicycles().isEmpty()) {
            BusinessAnalyticsData.BicycleMetric bm = d.getMostRentedBicycles().get(0);
            topBike = bm.getBicycleId() + " (" + bm.getRideCount() + " rides)";
        }
        binding.tvTopBike.setText("🚲 Top Bicycle: " + topBike);

        String topStation = "None";
        if (d.getPopularStations() != null && !d.getPopularStations().isEmpty()) {
            BusinessAnalyticsData.StationUsage su = d.getPopularStations().get(0);
            topStation = su.getStationName() + " (" + su.getPickupCount() + " rides)";
        }
        binding.tvTopStation.setText("📍 Top Station: " + topStation);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
