package com.sanika.bicyclebookingapp.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.sanika.bicyclebookingapp.model.BusinessAnalyticsData;
import com.sanika.bicyclebookingapp.repository.AnalyticsRepository;
import com.sanika.bicyclebookingapp.service.ReportExporterService;
import com.sanika.bicyclebookingapp.util.Resource;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

/**
 * ViewModel for Business Analytics, Fleet Intelligence, and Multi-Format Reporting.
 */
public class AnalyticsViewModel extends AndroidViewModel {

    public enum DateFilterPreset {
        ALL_TIME("All Time"),
        TODAY("Today"),
        LAST_7_DAYS("Last 7 Days"),
        THIS_MONTH("This Month"),
        THIS_YEAR("Year to Date");

        private final String label;
        DateFilterPreset(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static class DateFilter {
        final Date start;
        final Date end;
        final String label;

        DateFilter(Date start, Date end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }
    }

    private final AnalyticsRepository repository;
    private final ReportExporterService exporterService;

    private final MutableLiveData<DateFilter> activeFilter = new MutableLiveData<>();
    private final LiveData<Resource<BusinessAnalyticsData>> analyticsLiveData;

    private final MutableLiveData<Resource<File>> exportStatus = new MutableLiveData<>();

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = AnalyticsRepository.getInstance();
        this.exporterService = ReportExporterService.getInstance();

        // SwitchMap triggers whenever date range changes
        analyticsLiveData = Transformations.switchMap(activeFilter, filter -> {
            if (filter == null) {
                return repository.loadAnalytics(null, null);
            }
            return repository.loadAnalytics(filter.start, filter.end);
        });

        // Default to ALL_TIME on initial load
        setPresetFilter(DateFilterPreset.ALL_TIME);
    }

    public LiveData<Resource<BusinessAnalyticsData>> getAnalytics() {
        return analyticsLiveData;
    }

    public LiveData<Resource<File>> getExportStatus() {
        return exportStatus;
    }

    public String getCurrentFilterLabel() {
        DateFilter df = activeFilter.getValue();
        return df != null ? df.label : "All Time";
    }

    public void setPresetFilter(DateFilterPreset preset) {
        Calendar cal = Calendar.getInstance();
        Date end = new Date();
        Date start = null;

        switch (preset) {
            case TODAY:
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTime();
                break;
            case LAST_7_DAYS:
                cal.add(Calendar.DAY_OF_YEAR, -7);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTime();
                break;
            case THIS_MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTime();
                break;
            case THIS_YEAR:
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTime();
                break;
            case ALL_TIME:
            default:
                start = null;
                end = null;
                break;
        }

        activeFilter.setValue(new DateFilter(start, end, preset.getLabel()));
    }

    public void setCustomDateRange(Date start, Date end) {
        activeFilter.setValue(new DateFilter(start, end, "Custom Range"));
    }

    public void refresh() {
        DateFilter df = activeFilter.getValue();
        activeFilter.setValue(df);
    }

    /**
     * Exports one of the 9 standard reports to a CSV file.
     */
    public void exportReport(ReportExporterService.ReportType type) {
        exportStatus.setValue(Resource.loading(null));
        try {
            String csvContent;
            switch (type) {
                case BOOKING_REPORT:
                    csvContent = exporterService.exportBookingReport(repository.getCachedBookings());
                    break;
                case RIDE_REPORT:
                    csvContent = exporterService.exportRideReport(repository.getCachedBookings());
                    break;
                case REVENUE_REPORT:
                    csvContent = exporterService.exportRevenueReport(repository.getCachedInvoices());
                    break;
                case PAYMENT_REPORT:
                    csvContent = exporterService.exportPaymentReport(repository.getCachedPayments());
                    break;
                case REFUND_REPORT:
                    csvContent = exporterService.exportRefundReport(repository.getCachedRefunds());
                    break;
                case FLEET_REPORT:
                    csvContent = exporterService.exportFleetReport(
                            repository.getCachedBicycles(),
                            repository.getBicycleRideCounts(),
                            repository.getBicycleMaintenanceCosts()
                    );
                    break;
                case MAINTENANCE_REPORT:
                    csvContent = exporterService.exportMaintenanceReport(repository.getCachedMaintenance());
                    break;
                case STATION_REPORT:
                    csvContent = exporterService.exportStationReport(
                            repository.getCachedStations(),
                            repository.getStationPickupCounts(),
                            repository.getStationDropoffCounts()
                    );
                    break;
                case CUSTOMER_REPORT:
                    csvContent = exporterService.exportCustomerReport(
                            repository.getCachedUsers(),
                            repository.getUserBookingCounts(),
                            repository.getUserSpentPaise()
                    );
                    break;
                default:
                    exportStatus.setValue(Resource.error("Unknown report type", null));
                    return;
            }

            File file = exporterService.saveReportToFile(getApplication(), type, csvContent);
            exportStatus.setValue(Resource.success(file));
        } catch (Exception e) {
            exportStatus.setValue(Resource.error("Export failed: " + e.getMessage(), null));
        }
    }

    public Intent createShareIntent(File file, String title) {
        return exporterService.createShareIntent(getApplication(), file, title);
    }

    public String generatePrintableSummary(BusinessAnalyticsData data) {
        return exporterService.generatePrintableSummary(data, getCurrentFilterLabel());
    }
}
