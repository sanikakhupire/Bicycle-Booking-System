package com.sanika.bicyclebookingapp.service;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.BusinessAnalyticsData;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.Refund;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service to generate RFC-4180 standard CSV exports and printable business reports.
 * Supports all 9 required analytics report types with full Indian Rupee (INR / ₹) precision.
 */
public class ReportExporterService {

    public enum ReportType {
        BOOKING_REPORT("Bookings_Report"),
        RIDE_REPORT("Rides_Report"),
        REVENUE_REPORT("Revenue_Report"),
        PAYMENT_REPORT("Payments_Report"),
        REFUND_REPORT("Refunds_Report"),
        FLEET_REPORT("Fleet_Report"),
        MAINTENANCE_REPORT("Maintenance_Report"),
        STATION_REPORT("Stations_Report"),
        CUSTOMER_REPORT("Customers_Report");

        private final String filePrefix;

        ReportType(String filePrefix) {
            this.filePrefix = filePrefix;
        }

        public String getFilePrefix() {
            return filePrefix;
        }
    }

    private static ReportExporterService instance;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
    private final SimpleDateFormat fileTimestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);

    private ReportExporterService() {}

    public static synchronized ReportExporterService getInstance() {
        if (instance == null) {
            instance = new ReportExporterService();
        }
        return instance;
    }

    // ==========================================
    // 1. CSV EXPORT IMPLEMENTATIONS
    // ==========================================

    /**
     * 1. Booking Report
     */
    public String exportBookingReport(List<Booking> bookings) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Booking ID", "User ID", "Bicycle ID", "Start Station ID", "Return Station ID",
                "Status", "Start Time", "End Time", "Duration (Mins)", "Distance (KM)", "Total Fare (INR)", "Payment Status"));

        if (bookings != null) {
            for (Booking b : bookings) {
                sb.append(csvRow(
                        b.getBookingId(),
                        b.getUserId(),
                        b.getBicycleId(),
                        b.getStationId(),
                        b.getReturnStationId(),
                        b.getStatus(),
                        formatDate(b.getStartTime()),
                        formatDate(b.getEndTime()),
                        b.getDurationMinutes(),
                        String.format(Locale.ENGLISH, "%.2f", b.getDistanceKm()),
                        String.format(Locale.ENGLISH, "%.2f", b.getFare()),
                        b.getPaymentStatus()
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 2. Ride Report (Completed & Active Rides)
     */
    public String exportRideReport(List<Booking> rides) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Ride ID", "User ID", "Bicycle ID", "Bicycle Type", "Pickup Station", "Dropoff Station",
                "Status", "Start Time", "End Time", "Duration (Mins)", "Distance (KM)", "Total Fare (INR)"));

        if (rides != null) {
            for (Booking r : rides) {
                sb.append(csvRow(
                        r.getBookingId(),
                        r.getUserId(),
                        r.getBicycleId(),
                        r.getBicycleType() != null ? r.getBicycleType() : "Standard",
                        r.getStationName() != null ? r.getStationName() : r.getStationId(),
                        r.getReturnStationName() != null ? r.getReturnStationName() : r.getReturnStationId(),
                        r.getStatus(),
                        formatDate(r.getStartTime()),
                        formatDate(r.getEndTime()),
                        r.getDurationMinutes(),
                        String.format(Locale.ENGLISH, "%.2f", r.getDistanceKm()),
                        String.format(Locale.ENGLISH, "%.2f", r.getFare())
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 3. Revenue & Invoice Report
     */
    public String exportRevenueReport(List<Invoice> invoices) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Invoice ID", "Booking ID", "User ID", "Bicycle Type", "Start Time", "End Time",
                "Base Fee (INR)", "Usage Charge (INR)", "Extra / Late Fee (INR)", "Discount (INR)",
                "GST / Taxes (INR)", "Total Invoiced (INR)", "Payment Status"));

        if (invoices != null) {
            for (Invoice inv : invoices) {
                double base = CurrencyUtil.paiseToRupees(inv.getBaseFeePaise());
                double usage = CurrencyUtil.paiseToRupees(inv.getUsageChargePaise());
                double extra = CurrencyUtil.paiseToRupees(inv.getLateFeePaise() + inv.getExtraChargesPaise());
                double discount = CurrencyUtil.paiseToRupees(inv.getDiscountPaise());
                double tax = CurrencyUtil.paiseToRupees(inv.getTotalTaxPaise());
                double total = CurrencyUtil.paiseToRupees(inv.getTotalAmountPaise());

                sb.append(csvRow(
                        inv.getInvoiceId(),
                        inv.getBookingId(),
                        inv.getUserId(),
                        inv.getBicycleType(),
                        formatDate(inv.getStartTime()),
                        formatDate(inv.getEndTime()),
                        String.format(Locale.ENGLISH, "%.2f", base),
                        String.format(Locale.ENGLISH, "%.2f", usage),
                        String.format(Locale.ENGLISH, "%.2f", extra),
                        String.format(Locale.ENGLISH, "%.2f", discount),
                        String.format(Locale.ENGLISH, "%.2f", tax),
                        String.format(Locale.ENGLISH, "%.2f", total),
                        inv.getPaymentStatus()
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 4. Payment Transactions Report
     */
    public String exportPaymentReport(List<Payment> payments) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Payment ID", "Booking ID", "User ID", "Order ID", "Transaction ID",
                "Method", "Amount (INR)", "Status", "Created At", "Failure Reason"));

        if (payments != null) {
            for (Payment p : payments) {
                double amt = p.getAmountPaise() > 0 ? CurrencyUtil.paiseToRupees(p.getAmountPaise()) : p.getAmount();
                sb.append(csvRow(
                        p.getPaymentId(),
                        p.getBookingId(),
                        p.getUserId(),
                        p.getOrderId(),
                        p.getTransactionId() != null ? p.getTransactionId() : p.getGatewayPaymentId(),
                        p.getMethod(),
                        String.format(Locale.ENGLISH, "%.2f", amt),
                        p.getStatus(),
                        formatDate(p.getCreatedAt()),
                        p.getFailureReason()
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 5. Refund Transactions Report
     */
    public String exportRefundReport(List<Refund> refunds) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Refund ID", "Payment ID", "Booking ID", "User ID",
                "Amount (INR)", "Reason", "Status", "Gateway Refund ID", "Created At", "Processed At"));

        if (refunds != null) {
            for (Refund r : refunds) {
                double amt = CurrencyUtil.paiseToRupees(r.getAmountPaise());
                sb.append(csvRow(
                        r.getRefundId(),
                        r.getPaymentId(),
                        r.getBookingId(),
                        r.getUserId(),
                        String.format(Locale.ENGLISH, "%.2f", amt),
                        r.getReason(),
                        r.getStatus(),
                        r.getGatewayRefundId(),
                        formatDate(r.getCreatedAt()),
                        formatDate(r.getProcessedAt())
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 6. Fleet Report
     */
    public String exportFleetReport(List<Bicycle> bicycles, Map<String, Integer> rideCounts, Map<String, Long> maintenanceCosts) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Bicycle ID", "Type", "Status", "Station Name / ID", "Rating", "Total Rides", "Total Maint Cost (INR)"));

        if (bicycles != null) {
            for (Bicycle b : bicycles) {
                int rides = (rideCounts != null && rideCounts.containsKey(b.getBicycleId())) ? rideCounts.get(b.getBicycleId()) : 0;
                long costPaise = (maintenanceCosts != null && maintenanceCosts.containsKey(b.getBicycleId())) ? maintenanceCosts.get(b.getBicycleId()) : 0L;
                double costInr = CurrencyUtil.paiseToRupees(costPaise);

                sb.append(csvRow(
                        b.getBicycleId(),
                        b.getType(),
                        b.getStatus(),
                        b.getStationName() != null ? b.getStationName() : b.getStationId(),
                        String.format(Locale.ENGLISH, "%.1f", b.getAverageRating()),
                        rides,
                        String.format(Locale.ENGLISH, "%.2f", costInr)
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 7. Maintenance & Repairs Report
     */
    public String exportMaintenanceReport(List<MaintenanceRecord> records) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Record ID", "Bicycle ID", "Category", "Priority", "Status",
                "Reported By", "Assigned Staff", "Total Cost (INR)", "Parts Replaced", "Reported At", "Completed At"));

        if (records != null) {
            for (MaintenanceRecord m : records) {
                double costInr = m.getTotalCostPaise() > 0 ? CurrencyUtil.paiseToRupees(m.getTotalCostPaise()) : m.getCost();
                sb.append(csvRow(
                        m.getRecordId(),
                        m.getBicycleId(),
                        m.getCategory(),
                        m.getPriority(),
                        m.getStatus(),
                        m.getReporterName() != null ? m.getReporterName() : m.getReportedByUserId(),
                        m.getAssignedStaffName(),
                        String.format(Locale.ENGLISH, "%.2f", costInr),
                        m.getPartsReplaced(),
                        formatDate(m.getCreatedAt()),
                        formatDate(m.getCompletedAt())
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 8. Station Inventory & Flow Report
     */
    public String exportStationReport(List<Station> stations, Map<String, Integer> pickupCounts, Map<String, Integer> dropoffCounts) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("Station ID", "Station Name", "Address", "Total Capacity", "Available Bikes",
                "Pickups (Rides Started)", "Dropoffs (Rides Ended)", "Net Bicycle Flow", "Rating"));

        if (stations != null) {
            for (Station s : stations) {
                int pickups = (pickupCounts != null && pickupCounts.containsKey(s.getStationId())) ? pickupCounts.get(s.getStationId()) : 0;
                int dropoffs = (dropoffCounts != null && dropoffCounts.containsKey(s.getStationId())) ? dropoffCounts.get(s.getStationId()) : 0;
                int netFlow = dropoffs - pickups;

                sb.append(csvRow(
                        s.getStationId(),
                        s.getName(),
                        s.getAddress(),
                        s.getTotalCapacity(),
                        s.getAvailableBicycles(),
                        pickups,
                        dropoffs,
                        netFlow >= 0 ? "+" + netFlow : String.valueOf(netFlow),
                        String.format(Locale.ENGLISH, "%.1f", s.getAverageRating())
                ));
            }
        }
        return sb.toString();
    }

    /**
     * 9. Customer & User Report
     */
    public String exportCustomerReport(List<User> users, Map<String, Integer> bookingCounts, Map<String, Long> spentPaise) {
        StringBuilder sb = initCsvBuilder();
        sb.append(csvRow("User ID", "Name", "Email", "Phone", "Role",
                "Total Bookings", "Total Spent (INR)", "Registered At"));

        if (users != null) {
            for (User u : users) {
                int count = (bookingCounts != null && bookingCounts.containsKey(u.getUid())) ? bookingCounts.get(u.getUid()) : 0;
                long spent = (spentPaise != null && spentPaise.containsKey(u.getUid())) ? spentPaise.get(u.getUid()) : 0L;
                double spentInr = CurrencyUtil.paiseToRupees(spent);

                sb.append(csvRow(
                        u.getUid(),
                        u.getName(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getRole(),
                        count,
                        String.format(Locale.ENGLISH, "%.2f", spentInr),
                        formatDate(u.getCreatedAt())
                ));
            }
        }
        return sb.toString();
    }

    // ==========================================
    // 2. FILE SAVING & INTENT SHARING
    // ==========================================

    /**
     * Saves CSV string to app cache directory `reports/`.
     */
    public File saveReportToFile(Context context, ReportType type, String csvContent) throws IOException {
        File reportsDir = new File(context.getCacheDir(), "reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String fileName = type.getFilePrefix() + "_" + fileTimestampFormat.format(new Date()) + ".csv";
        File file = new File(reportsDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(csvContent);
            writer.flush();
        }
        return file;
    }

    /**
     * Creates an Intent to share or view the exported CSV file.
     */
    public Intent createShareIntent(Context context, File file, String title) {
        Uri fileUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, "CycleGo Commercial Business Report: " + title + "\nGenerated on: " + dateFormat.format(new Date()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return Intent.createChooser(intent, "Share Report via");
    }

    // ==========================================
    // 3. PRINTABLE EXECUTIVE SUMMARY GENERATOR
    // ==========================================

    /**
     * Generates a printable, human-readable summary of the business analytics.
     */
    public String generatePrintableSummary(BusinessAnalyticsData data, String periodLabel) {
        if (data == null) return "No analytics data available.";

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("         CYCLEGO BICYCLE RENTALS - BUSINESS REPORT   \n");
        sb.append("====================================================\n");
        sb.append("Report Period: ").append(periodLabel != null ? periodLabel : "Custom Range").append("\n");
        sb.append("Generated On:  ").append(dateFormat.format(new Date())).append("\n\n");

        sb.append("--- 1. FINANCIAL PERFORMANCE (INR / ₹) ---\n");
        sb.append("Gross Invoiced:      ").append(data.getFormattedGrossInvoiced()).append("\n");
        sb.append("Payments Received:   ").append(data.getFormattedPaymentsReceived()).append("\n");
        sb.append("Refunds Processed:   ").append(data.getFormattedTotalRefunds()).append("\n");
        sb.append("Pending Receivables: ").append(data.getFormattedPendingAmount()).append("\n");
        sb.append("NET REVENUE:         ").append(data.getFormattedNetRevenue()).append("\n");
        sb.append("Avg Revenue / Ride:  ").append(data.getFormattedAvgRevenuePerRide()).append("\n\n");

        sb.append("--- 2. RIDE & FLEET OPERATIONS ---\n");
        sb.append("Total Bookings:      ").append(data.getTotalBookings()).append("\n");
        sb.append("Completed Rides:     ").append(data.getTotalCompletedRides()).append("\n");
        sb.append("Active Rides Now:    ").append(data.getActiveRides()).append("\n");
        sb.append("Overdue Rides:       ").append(data.getOverdueRides()).append("\n");
        sb.append("Cancelled Bookings:  ").append(data.getCancelledBookings()).append(" (")
                .append(String.format(Locale.ENGLISH, "%.1f%%", data.getCancellationRate())).append(")\n");
        sb.append("Avg Ride Duration:   ").append(data.getAvgRideDurationMinutes()).append(" mins\n");
        sb.append("Avg Distance / Ride: ").append(String.format(Locale.ENGLISH, "%.2f km", data.getAvgDistanceKm())).append("\n");
        sb.append("Fleet Utilization:   ").append(String.format(Locale.ENGLISH, "%.1f%%", data.getFleetUtilizationRate())).append("\n");
        sb.append("Fleet Availability:  ").append(String.format(Locale.ENGLISH, "%.1f%%", data.getFleetAvailabilityRate())).append("\n\n");

        sb.append("--- 3. FLEET HEALTH & MAINTENANCE ---\n");
        sb.append("Total Fleet Size:    ").append(data.getTotalBicycles()).append(" bicycles\n");
        sb.append("Available Bicycles:  ").append(data.getAvailableBicycles()).append("\n");
        sb.append("In Use Bicycles:     ").append(data.getInUseBicycles()).append("\n");
        sb.append("Under Maintenance:   ").append(data.getMaintenanceBicycles()).append("\n");
        sb.append("Maintenance Tickets: ").append(data.getTotalMaintenanceRecords()).append("\n");
        sb.append("Total Maint Costs:   ").append(data.getFormattedMaintenanceCosts()).append("\n\n");

        sb.append("--- 4. CUSTOMER METRICS & SATISFACTION ---\n");
        sb.append("Total Customers:     ").append(data.getTotalCustomers()).append("\n");
        sb.append("New Customers:       ").append(data.getNewCustomers()).append("\n");
        sb.append("Active Customers:    ").append(data.getActiveCustomers()).append("\n");
        sb.append("Returning Customers: ").append(data.getReturningCustomers()).append("\n");
        sb.append("Retention Rate:      ").append(String.format(Locale.ENGLISH, "%.1f%%", data.getCustomerRetentionRate())).append("\n");
        sb.append("Avg Customer Rating: ").append(String.format(Locale.ENGLISH, "%.2f / 5.0", data.getAvgCustomerRating())).append("\n");
        sb.append("Open Complaints:     ").append(data.getOpenTickets()).append(" / ").append(data.getTotalTickets()).append("\n\n");

        sb.append("--- 5. TOP PERFORMING ASSETS ---\n");
        if (data.getMostRentedBicycles() != null && !data.getMostRentedBicycles().isEmpty()) {
            sb.append("Top Bicycles: ");
            for (int i = 0; i < Math.min(3, data.getMostRentedBicycles().size()); i++) {
                BusinessAnalyticsData.BicycleMetric bm = data.getMostRentedBicycles().get(i);
                sb.append(bm.getBicycleId()).append(" (").append(bm.getRideCount()).append(" rides) ");
            }
            sb.append("\n");
        }
        if (data.getPopularStations() != null && !data.getPopularStations().isEmpty()) {
            sb.append("Top Stations: ");
            for (int i = 0; i < Math.min(3, data.getPopularStations().size()); i++) {
                BusinessAnalyticsData.StationUsage su = data.getPopularStations().get(i);
                sb.append(su.getStationName()).append(" (").append(su.getPickupCount()).append(" pickups) ");
            }
            sb.append("\n");
        }

        sb.append("====================================================\n");
        sb.append("                END OF REPORT                       \n");
        sb.append("====================================================\n");

        return sb.toString();
    }

    // ==========================================
    // 4. RFC-4180 CSV HELPERS
    // ==========================================

    private StringBuilder initCsvBuilder() {
        StringBuilder sb = new StringBuilder();
        // UTF-8 Byte Order Mark (BOM) ensures Microsoft Excel and spreadsheet tools render UTF-8 (₹, etc.) accurately.
        sb.append("\uFEFF");
        return sb;
    }

    private String csvRow(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCsv(fields[i]));
        }
        sb.append("\r\n");
        return sb.toString();
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        boolean containsSpecial = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) {
            s = s.replace("\"", "\"\"");
        }
        if (containsSpecial) {
            return "\"" + s + "\"";
        }
        return s;
    }

    private String formatDate(Date date) {
        if (date == null) return "";
        return dateFormat.format(date);
    }
}
