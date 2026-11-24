package com.sanika.bicyclebookingapp.model;

import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive business intelligence metrics and aggregated analytics data.
 * All monetary amounts are preserved in integer paise with Indian Rupee formatting.
 */
public class BusinessAnalyticsData {

    public static class BicycleMetric {
        private final String bicycleId;
        private final int rideCount;

        public BicycleMetric(String bicycleId, int rideCount) {
            this.bicycleId = bicycleId;
            this.rideCount = rideCount;
        }

        public String getBicycleId() { return bicycleId; }
        public int getRideCount() { return rideCount; }
    }

    public static class StationUsage {
        private final String stationId;
        private final String stationName;
        private final int pickupCount;

        public StationUsage(String stationId, String stationName, int pickupCount) {
            this.stationId = stationId;
            this.stationName = stationName;
            this.pickupCount = pickupCount;
        }

        public String getStationId() { return stationId; }
        public String getStationName() { return stationName; }
        public int getPickupCount() { return pickupCount; }
    }

    // ─── 1. RIDE METRICS ──────────────────────────────────────────────────────────
    private int totalBookings = 0;
    private int totalRides = 0;
    private int dailyRides = 0;   // Today
    private int weeklyRides = 0;  // Last 7 days
    private int monthlyRides = 0; // This month / last 30 days
    private int yearlyRides = 0;  // This calendar year
    private int activeRides = 0;
    private int completedRides = 0;
    private int cancelledRides = 0;
    private double cancellationRatePercent = 0.0;
    private int overdueRidesCount = 0;
    private double averageRideDurationMinutes = 0.0;
    private double averageRideDistanceKm = 0.0;

    // ─── 2. REVENUE & FINANCIAL METRICS (IN PAISE) ──────────────────────────────
    private long totalRevenuePaise = 0L;
    private long dailyRevenuePaise = 0L;
    private long weeklyRevenuePaise = 0L;
    private long monthlyRevenuePaise = 0L;
    private long yearlyRevenuePaise = 0L;
    private long averageRevenuePerRidePaise = 0L;

    // Financial Reconciliation
    private long grossInvoicedPaise = 0L;       // Total invoiced billing
    private long paymentsReceivedPaise = 0L;    // Actual verified payments received
    private long totalRefundsPaise = 0L;        // Total refunds disbursed
    private long pendingAmountPaise = 0L;       // Outstanding receivables
    private long netRevenuePaise = 0L;          // paymentsReceived - totalRefunds

    private int paymentFailuresCount = 0;
    private int refundsCount = 0;

    // ─── 3. FLEET ANALYTICS ─────────────────────────────────────────────────────
    private int totalBicycles = 0;
    private int availableBicycles = 0;
    private int inUseBicycles = 0;
    private int maintenanceBicycles = 0;
    private double fleetAvailabilityPercent = 0.0;
    private double fleetUtilizationPercent = 0.0;

    private int totalMaintenanceRecords = 0;
    private long totalMaintenanceCostPaise = 0L;
    private double averageBicycleRating = 5.0;

    // ─── 4. CUSTOMER ANALYTICS ──────────────────────────────────────────────────
    private int totalCustomers = 0;
    private int newCustomers = 0;
    private int activeCustomers = 0;
    private int returningCustomers = 0;
    private double customerRetentionRatePercent = 0.0;
    private double averageRidesPerActiveCustomer = 0.0;

    private int openTicketsCount = 0;
    private int totalTicketsCount = 0;

    // ─── 5. RANKINGS & BREAKDOWNS ───────────────────────────────────────────────
    private List<BicycleMetric> mostRentedBicycles = new ArrayList<>();
    private List<BicycleMetric> leastRentedBicycles = new ArrayList<>();
    private List<StationUsage> popularStations = new ArrayList<>();
    private Map<String, Integer> complaintsByCategory = new HashMap<>();

    public BusinessAnalyticsData() {}

    // Formatted INR Strings
    public String getTotalRevenueFormatted() { return CurrencyUtil.formatInrFromPaise(totalRevenuePaise); }
    public String getDailyRevenueFormatted() { return CurrencyUtil.formatInrFromPaise(dailyRevenuePaise); }
    public String getWeeklyRevenueFormatted() { return CurrencyUtil.formatInrFromPaise(weeklyRevenuePaise); }
    public String getMonthlyRevenueFormatted() { return CurrencyUtil.formatInrFromPaise(monthlyRevenuePaise); }
    public String getYearlyRevenueFormatted() { return CurrencyUtil.formatInrFromPaise(yearlyRevenuePaise); }
    public String getAverageRevenuePerRideFormatted() { return CurrencyUtil.formatInrFromPaise(averageRevenuePerRidePaise); }
    public String getGrossInvoicedFormatted() { return CurrencyUtil.formatInrFromPaise(grossInvoicedPaise); }
    public String getPaymentsReceivedFormatted() { return CurrencyUtil.formatInrFromPaise(paymentsReceivedPaise); }
    public String getTotalRefundsFormatted() { return CurrencyUtil.formatInrFromPaise(totalRefundsPaise); }
    public String getPendingAmountFormatted() { return CurrencyUtil.formatInrFromPaise(pendingAmountPaise); }
    public String getNetRevenueFormatted() { return CurrencyUtil.formatInrFromPaise(netRevenuePaise); }
    public String getTotalMaintenanceCostFormatted() { return CurrencyUtil.formatInrFromPaise(totalMaintenanceCostPaise); }

    // Alternative formatted names
    public String getFormattedGrossInvoiced() { return getGrossInvoicedFormatted(); }
    public String getFormattedPaymentsReceived() { return getPaymentsReceivedFormatted(); }
    public String getFormattedTotalRefunds() { return getTotalRefundsFormatted(); }
    public String getFormattedPendingAmount() { return getPendingAmountFormatted(); }
    public String getFormattedNetRevenue() { return getNetRevenueFormatted(); }
    public String getFormattedAvgRevenuePerRide() { return getAverageRevenuePerRideFormatted(); }
    public String getFormattedMaintenanceCosts() { return getTotalMaintenanceCostFormatted(); }

    // Getters and Setters
    public int getTotalBookings() { return totalBookings > 0 ? totalBookings : totalRides; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public int getTotalRides() { return totalRides; }
    public void setTotalRides(int totalRides) { this.totalRides = totalRides; }

    public int getDailyRides() { return dailyRides; }
    public void setDailyRides(int dailyRides) { this.dailyRides = dailyRides; }

    public int getWeeklyRides() { return weeklyRides; }
    public void setWeeklyRides(int weeklyRides) { this.weeklyRides = weeklyRides; }

    public int getMonthlyRides() { return monthlyRides; }
    public void setMonthlyRides(int monthlyRides) { this.monthlyRides = monthlyRides; }

    public int getYearlyRides() { return yearlyRides; }
    public void setYearlyRides(int yearlyRides) { this.yearlyRides = yearlyRides; }

    public int getActiveRides() { return activeRides; }
    public void setActiveRides(int activeRides) { this.activeRides = activeRides; }

    public int getCompletedRides() { return completedRides; }
    public void setCompletedRides(int completedRides) { this.completedRides = completedRides; }
    public int getTotalCompletedRides() { return completedRides; }

    public int getCancelledRides() { return cancelledRides; }
    public void setCancelledRides(int cancelledRides) { this.cancelledRides = cancelledRides; }
    public int getCancelledBookings() { return cancelledRides; }

    public double getCancellationRatePercent() { return cancellationRatePercent; }
    public void setCancellationRatePercent(double cancellationRatePercent) { this.cancellationRatePercent = cancellationRatePercent; }
    public double getCancellationRate() { return cancellationRatePercent; }

    public int getOverdueRidesCount() { return overdueRidesCount; }
    public void setOverdueRidesCount(int overdueRidesCount) { this.overdueRidesCount = overdueRidesCount; }
    public int getOverdueRides() { return overdueRidesCount; }

    public double getAverageRideDurationMinutes() { return averageRideDurationMinutes; }
    public void setAverageRideDurationMinutes(double averageRideDurationMinutes) { this.averageRideDurationMinutes = averageRideDurationMinutes; }
    public long getAvgRideDurationMinutes() { return Math.round(averageRideDurationMinutes); }

    public double getAverageRideDistanceKm() { return averageRideDistanceKm; }
    public void setAverageRideDistanceKm(double averageRideDistanceKm) { this.averageRideDistanceKm = averageRideDistanceKm; }
    public double getAvgDistanceKm() { return averageRideDistanceKm; }

    public long getTotalRevenuePaise() { return totalRevenuePaise; }
    public void setTotalRevenuePaise(long totalRevenuePaise) { this.totalRevenuePaise = totalRevenuePaise; }

    public long getDailyRevenuePaise() { return dailyRevenuePaise; }
    public void setDailyRevenuePaise(long dailyRevenuePaise) { this.dailyRevenuePaise = dailyRevenuePaise; }

    public long getWeeklyRevenuePaise() { return weeklyRevenuePaise; }
    public void setWeeklyRevenuePaise(long weeklyRevenuePaise) { this.weeklyRevenuePaise = weeklyRevenuePaise; }

    public long getMonthlyRevenuePaise() { return monthlyRevenuePaise; }
    public void setMonthlyRevenuePaise(long monthlyRevenuePaise) { this.monthlyRevenuePaise = monthlyRevenuePaise; }

    public long getYearlyRevenuePaise() { return yearlyRevenuePaise; }
    public void setYearlyRevenuePaise(long yearlyRevenuePaise) { this.yearlyRevenuePaise = yearlyRevenuePaise; }

    public long getAverageRevenuePerRidePaise() { return averageRevenuePerRidePaise; }
    public void setAverageRevenuePerRidePaise(long averageRevenuePerRidePaise) { this.averageRevenuePerRidePaise = averageRevenuePerRidePaise; }
    public long getAvgRevenuePerRidePaise() { return averageRevenuePerRidePaise; }

    public long getGrossInvoicedPaise() { return grossInvoicedPaise; }
    public void setGrossInvoicedPaise(long grossInvoicedPaise) { this.grossInvoicedPaise = grossInvoicedPaise; }

    public long getPaymentsReceivedPaise() { return paymentsReceivedPaise; }
    public void setPaymentsReceivedPaise(long paymentsReceivedPaise) { this.paymentsReceivedPaise = paymentsReceivedPaise; }

    public long getTotalRefundsPaise() { return totalRefundsPaise; }
    public void setTotalRefundsPaise(long totalRefundsPaise) { this.totalRefundsPaise = totalRefundsPaise; }

    public long getPendingAmountPaise() { return pendingAmountPaise; }
    public void setPendingAmountPaise(long pendingAmountPaise) { this.pendingAmountPaise = pendingAmountPaise; }

    public long getNetRevenuePaise() { return netRevenuePaise; }
    public void setNetRevenuePaise(long netRevenuePaise) { this.netRevenuePaise = netRevenuePaise; }

    public int getPaymentFailuresCount() { return paymentFailuresCount; }
    public void setPaymentFailuresCount(int paymentFailuresCount) { this.paymentFailuresCount = paymentFailuresCount; }
    public int getPaymentFailures() { return paymentFailuresCount; }

    public int getRefundsCount() { return refundsCount; }
    public void setRefundsCount(int refundsCount) { this.refundsCount = refundsCount; }

    public int getTotalBicycles() { return totalBicycles; }
    public void setTotalBicycles(int totalBicycles) { this.totalBicycles = totalBicycles; }

    public int getAvailableBicycles() { return availableBicycles; }
    public void setAvailableBicycles(int availableBicycles) { this.availableBicycles = availableBicycles; }

    public int getInUseBicycles() { return inUseBicycles; }
    public void setInUseBicycles(int inUseBicycles) { this.inUseBicycles = inUseBicycles; }

    public int getMaintenanceBicycles() { return maintenanceBicycles; }
    public void setMaintenanceBicycles(int maintenanceBicycles) { this.maintenanceBicycles = maintenanceBicycles; }

    public double getFleetAvailabilityPercent() { return fleetAvailabilityPercent; }
    public void setFleetAvailabilityPercent(double fleetAvailabilityPercent) { this.fleetAvailabilityPercent = fleetAvailabilityPercent; }
    public double getFleetAvailabilityRate() { return fleetAvailabilityPercent; }

    public double getFleetUtilizationPercent() { return fleetUtilizationPercent; }
    public void setFleetUtilizationPercent(double fleetUtilizationPercent) { this.fleetUtilizationPercent = fleetUtilizationPercent; }
    public double getFleetUtilizationRate() { return fleetUtilizationPercent; }

    public int getTotalMaintenanceRecords() { return totalMaintenanceRecords; }
    public void setTotalMaintenanceRecords(int totalMaintenanceRecords) { this.totalMaintenanceRecords = totalMaintenanceRecords; }

    public long getTotalMaintenanceCostPaise() { return totalMaintenanceCostPaise; }
    public void setTotalMaintenanceCostPaise(long totalMaintenanceCostPaise) { this.totalMaintenanceCostPaise = totalMaintenanceCostPaise; }
    public long getTotalMaintenanceCostsPaise() { return totalMaintenanceCostPaise; }

    public double getAverageBicycleRating() { return averageBicycleRating; }
    public void setAverageBicycleRating(double averageBicycleRating) { this.averageBicycleRating = averageBicycleRating; }
    public double getAvgCustomerRating() { return averageBicycleRating; }

    public int getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(int totalCustomers) { this.totalCustomers = totalCustomers; }

    public int getNewCustomers() { return newCustomers; }
    public void setNewCustomers(int newCustomers) { this.newCustomers = newCustomers; }

    public int getActiveCustomers() { return activeCustomers; }
    public void setActiveCustomers(int activeCustomers) { this.activeCustomers = activeCustomers; }

    public int getReturningCustomers() { return returningCustomers; }
    public void setReturningCustomers(int returningCustomers) { this.returningCustomers = returningCustomers; }

    public double getCustomerRetentionRatePercent() { return customerRetentionRatePercent; }
    public void setCustomerRetentionRatePercent(double customerRetentionRatePercent) { this.customerRetentionRatePercent = customerRetentionRatePercent; }
    public double getCustomerRetentionRate() { return customerRetentionRatePercent; }

    public double getAverageRidesPerActiveCustomer() { return averageRidesPerActiveCustomer; }
    public void setAverageRidesPerActiveCustomer(double averageRidesPerActiveCustomer) { this.averageRidesPerActiveCustomer = averageRidesPerActiveCustomer; }

    public int getOpenTicketsCount() { return openTicketsCount; }
    public void setOpenTicketsCount(int openTicketsCount) { this.openTicketsCount = openTicketsCount; }
    public int getOpenTickets() { return openTicketsCount; }

    public int getTotalTicketsCount() { return totalTicketsCount; }
    public void setTotalTicketsCount(int totalTicketsCount) { this.totalTicketsCount = totalTicketsCount; }
    public int getTotalTickets() { return totalTicketsCount; }

    public List<BicycleMetric> getMostRentedBicycles() { return mostRentedBicycles; }
    public void setMostRentedBicycles(List<BicycleMetric> mostRentedBicycles) { this.mostRentedBicycles = mostRentedBicycles; }

    public List<BicycleMetric> getLeastRentedBicycles() { return leastRentedBicycles; }
    public void setLeastRentedBicycles(List<BicycleMetric> leastRentedBicycles) { this.leastRentedBicycles = leastRentedBicycles; }

    public List<StationUsage> getPopularStations() { return popularStations; }
    public void setPopularStations(List<StationUsage> popularStations) { this.popularStations = popularStations; }

    public Map<String, Integer> getComplaintsByCategory() { return complaintsByCategory; }
    public void setComplaintsByCategory(Map<String, Integer> complaintsByCategory) { this.complaintsByCategory = complaintsByCategory; }
}
