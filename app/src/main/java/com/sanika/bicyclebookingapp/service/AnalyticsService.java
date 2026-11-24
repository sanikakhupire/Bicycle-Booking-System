package com.sanika.bicyclebookingapp.service;

import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.BusinessAnalyticsData;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.Refund;
import com.sanika.bicyclebookingapp.model.RideReview;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * High-performance computation engine for CycleGo India Business Intelligence.
 * Aggregates real operational, ride, financial, fleet, and customer records
 * with precision integer paise arithmetic.
 */
public class AnalyticsService {

    private static AnalyticsService instance;

    public static synchronized AnalyticsService getInstance() {
        if (instance == null) {
            instance = new AnalyticsService();
        }
        return instance;
    }

    public BusinessAnalyticsData computeAnalytics(
            List<Booking> bookings,
            List<Payment> payments,
            List<Refund> refunds,
            List<Invoice> invoices,
            List<Bicycle> bicycles,
            List<Station> stations,
            List<MaintenanceRecord> maintenanceRecords,
            List<User> users,
            List<RideReview> reviews,
            List<SupportTicket> supportTickets,
            Date startDate,
            Date endDate
    ) {
        BusinessAnalyticsData data = new BusinessAnalyticsData();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfToday = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date startOfPastWeek = cal.getTime();

        cal.setTime(new Date());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfThisMonth = cal.getTime();

        cal.set(Calendar.DAY_OF_YEAR, 1);
        Date startOfThisYear = cal.getTime();

        // ─── 1. BICYCLES & FLEET UTILIZATION ──────────────────────────────────────
        int totalBikes = bicycles != null ? bicycles.size() : 0;
        int availableBikes = 0;
        int inUseBikes = 0;
        int maintenanceBikes = 0;
        double sumRating = 0.0;
        int ratedBikesCount = 0;

        if (bicycles != null) {
            for (Bicycle b : bicycles) {
                String st = b.getStatus() != null ? b.getStatus().toUpperCase() : Bicycle.STATUS_AVAILABLE;
                if (Bicycle.STATUS_AVAILABLE.equalsIgnoreCase(st)) availableBikes++;
                else if (Bicycle.STATUS_IN_USE.equalsIgnoreCase(st) || Bicycle.STATUS_RESERVED.equalsIgnoreCase(st)) inUseBikes++;
                else if (Bicycle.STATUS_MAINTENANCE.equalsIgnoreCase(st) || Bicycle.STATUS_DAMAGED.equalsIgnoreCase(st)) maintenanceBikes++;

                if (b.getAverageRating() > 0) {
                    sumRating += b.getAverageRating();
                    ratedBikesCount++;
                }
            }
        }
        data.setTotalBicycles(totalBikes);
        data.setAvailableBicycles(availableBikes);
        data.setInUseBicycles(inUseBikes);
        data.setMaintenanceBicycles(maintenanceBikes);
        data.setFleetAvailabilityPercent(totalBikes > 0 ? (availableBikes * 100.0) / totalBikes : 0.0);
        data.setFleetUtilizationPercent(totalBikes > 0 ? (inUseBikes * 100.0) / totalBikes : 0.0);
        data.setAverageBicycleRating(ratedBikesCount > 0 ? sumRating / ratedBikesCount : 5.0);

        // ─── 2. MAINTENANCE ANALYTICS ─────────────────────────────────────────────
        int totalMaint = 0;
        long totalMaintCostPaise = 0L;
        if (maintenanceRecords != null) {
            for (MaintenanceRecord mr : maintenanceRecords) {
                Date d = mr.getCreatedAt();
                if (isWithinRange(d, startDate, endDate)) {
                    totalMaint++;
                    totalMaintCostPaise += mr.getCostPaise();
                }
            }
        }
        data.setTotalMaintenanceRecords(totalMaint);
        data.setTotalMaintenanceCostPaise(totalMaintCostPaise);

        // ─── 2b. CUSTOMER RATINGS (FROM REVIEWS) ──────────────────────────────────
        double sumReviewRating = 0.0;
        int reviewCount = 0;
        if (reviews != null) {
            for (RideReview rev : reviews) {
                Date d = rev.getCreatedAt();
                if (isWithinRange(d, startDate, endDate)) {
                    if (rev.getRatingOverall() > 0) {
                        sumReviewRating += rev.getRatingOverall();
                        reviewCount++;
                    }
                }
            }
        }
        if (reviewCount > 0) {
            data.setAverageBicycleRating(sumReviewRating / reviewCount);
        }

        // ─── 3. BOOKINGS & RIDES ──────────────────────────────────────────────────
        int totalBookings = 0;
        int dailyRides = 0;
        int weeklyRides = 0;
        int monthlyRides = 0;
        int yearlyRides = 0;
        int activeRides = 0;
        int completedRides = 0;
        int cancelledRides = 0;
        int overdueRides = 0;

        double sumDurationMins = 0;
        double sumDistanceKm = 0;
        int ridesWithDuration = 0;

        Map<String, Integer> bikeTripCounts = new HashMap<>();
        Map<String, Integer> stationUsageCounts = new HashMap<>();
        Map<String, Integer> userRideCounts = new HashMap<>();
        Set<String> activeUsersInPeriod = new HashSet<>();

        if (bookings != null) {
            for (Booking bk : bookings) {
                Date created = bk.getCreatedAt() != null ? bk.getCreatedAt() :
                        (bk.getStartTime() != null ? bk.getStartTime() : bk.getEndTime());
                if (!isWithinRange(created, startDate, endDate)) {
                    continue;
                }

                totalBookings++;

                if (created != null) {
                    if (!created.before(startOfToday)) dailyRides++;
                    if (!created.before(startOfPastWeek)) weeklyRides++;
                    if (!created.before(startOfThisMonth)) monthlyRides++;
                    if (!created.before(startOfThisYear)) yearlyRides++;
                }

                String st = bk.getStatus() != null ? bk.getStatus().toUpperCase() : Booking.STATUS_PENDING;
                if (Booking.STATUS_ACTIVE.equalsIgnoreCase(st)) {
                    activeRides++;
                    // Check if ride is overdue (> 4 hours ongoing)
                    if (bk.getStartTime() != null) {
                        long activeMs = System.currentTimeMillis() - bk.getStartTime().getTime();
                        if (activeMs > 4 * 60 * 60 * 1000L) {
                            overdueRides++;
                        }
                    }
                } else if (Booking.STATUS_COMPLETED.equalsIgnoreCase(st)) {
                    completedRides++;
                    sumDurationMins += bk.getDurationMinutes();
                    sumDistanceKm += bk.getDistanceKm();
                    ridesWithDuration++;
                } else if (Booking.STATUS_CANCELLED.equalsIgnoreCase(st) || Booking.STATUS_EXPIRED.equalsIgnoreCase(st)) {
                    cancelledRides++;
                }

                // Station usage
                if (bk.getStationName() != null && !bk.getStationName().isEmpty()) {
                    stationUsageCounts.put(bk.getStationName(), stationUsageCounts.getOrDefault(bk.getStationName(), 0) + 1);
                }
                if (bk.getReturnStationName() != null && !bk.getReturnStationName().isEmpty() && !bk.getReturnStationName().equals(bk.getStationName())) {
                    stationUsageCounts.put(bk.getReturnStationName(), stationUsageCounts.getOrDefault(bk.getReturnStationName(), 0) + 1);
                }

                // Bicycle usage
                if (bk.getBicycleId() != null && !bk.getBicycleId().isEmpty()) {
                    bikeTripCounts.put(bk.getBicycleId(), bikeTripCounts.getOrDefault(bk.getBicycleId(), 0) + 1);
                }

                // User ride distribution
                if (bk.getUserId() != null && !bk.getUserId().isEmpty()) {
                    userRideCounts.put(bk.getUserId(), userRideCounts.getOrDefault(bk.getUserId(), 0) + 1);
                    activeUsersInPeriod.add(bk.getUserId());
                }
            }
        }

        data.setTotalBookings(totalBookings);
        data.setTotalRides(totalBookings);
        data.setDailyRides(dailyRides);
        data.setWeeklyRides(weeklyRides);
        data.setMonthlyRides(monthlyRides);
        data.setYearlyRides(yearlyRides);
        data.setActiveRides(activeRides);
        data.setCompletedRides(completedRides);
        data.setCancelledRides(cancelledRides);
        data.setOverdueRidesCount(overdueRides);
        data.setCancellationRatePercent(totalBookings > 0 ? (cancelledRides * 100.0) / totalBookings : 0.0);
        data.setAverageRideDurationMinutes(ridesWithDuration > 0 ? sumDurationMins / ridesWithDuration : 0.0);
        data.setAverageRideDistanceKm(ridesWithDuration > 0 ? sumDistanceKm / ridesWithDuration : 0.0);

        // ─── 4. FINANCIAL RECONCILIATION & REVENUE (PAISE) ────────────────────────
        long grossInvoiced = 0L;
        long totalPayments = 0L;
        long dailyRevenue = 0L;
        long weeklyRevenue = 0L;
        long monthlyRevenue = 0L;
        long yearlyRevenue = 0L;
        int paymentFailures = 0;

        if (invoices != null) {
            for (Invoice inv : invoices) {
                Date d = inv.getCreatedAt() != null ? inv.getCreatedAt() : inv.getStartTime();
                if (isWithinRange(d, startDate, endDate)) {
                    grossInvoiced += inv.getTotalAmountPaise() > 0 ? inv.getTotalAmountPaise() : CurrencyUtil.toPaise(inv.getTotalAmountInr());
                }
            }
        }
        data.setGrossInvoicedPaise(grossInvoiced);

        if (payments != null) {
            for (Payment p : payments) {
                Date pDate = p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt();
                if (!isWithinRange(pDate, startDate, endDate)) {
                    continue;
                }

                long amt = p.getAmountPaise() > 0 ? p.getAmountPaise() : CurrencyUtil.toPaise(p.getAmount());
                String status = p.getStatus() != null ? p.getStatus().toUpperCase() : Payment.STATUS_PENDING;

                if (Payment.STATUS_SUCCESS.equalsIgnoreCase(status)) {
                    totalPayments += amt;
                    if (pDate != null) {
                        if (!pDate.before(startOfToday)) dailyRevenue += amt;
                        if (!pDate.before(startOfPastWeek)) weeklyRevenue += amt;
                        if (!pDate.before(startOfThisMonth)) monthlyRevenue += amt;
                        if (!pDate.before(startOfThisYear)) yearlyRevenue += amt;
                    }
                } else if (Payment.STATUS_FAILED.equalsIgnoreCase(status)) {
                    paymentFailures++;
                }
            }
        }

        data.setPaymentsReceivedPaise(totalPayments);
        data.setTotalRevenuePaise(totalPayments);
        data.setDailyRevenuePaise(dailyRevenue);
        data.setWeeklyRevenuePaise(weeklyRevenue);
        data.setMonthlyRevenuePaise(monthlyRevenue);
        data.setYearlyRevenuePaise(yearlyRevenue);
        data.setPaymentFailuresCount(paymentFailures);
        data.setAverageRevenuePerRidePaise(completedRides > 0 ? totalPayments / completedRides : 0L);

        // Refunds
        long totalRefunds = 0L;
        int refundCount = 0;
        if (refunds != null) {
            for (Refund r : refunds) {
                Date d = r.getProcessedAt() != null ? r.getProcessedAt() : r.getCreatedAt();
                if (isWithinRange(d, startDate, endDate)) {
                    refundCount++;
                    totalRefunds += r.getAmountPaise() > 0 ? r.getAmountPaise() : CurrencyUtil.toPaise(r.getAmount());
                }
            }
        }
        data.setTotalRefundsPaise(totalRefunds);
        data.setRefundsCount(refundCount);
        data.setNetRevenuePaise(Math.max(0L, totalPayments - totalRefunds));
        data.setPendingAmountPaise(Math.max(0L, grossInvoiced - totalPayments));

        // ─── 5. CUSTOMERS & RETENTION ─────────────────────────────────────────────
        int totalCust = 0;
        int newCust = 0;
        int returningCust = 0;

        if (users != null) {
            for (User u : users) {
                if (u.isAdmin() || u.isHandler()) continue;
                totalCust++;
                Date regDate = u.getCreatedAt();
                if (isWithinRange(regDate, startDate, endDate)) {
                    newCust++;
                }
            }
        }
        for (Map.Entry<String, Integer> entry : userRideCounts.entrySet()) {
            if (entry.getValue() > 1) {
                returningCust++;
            }
        }

        data.setTotalCustomers(totalCust > 0 ? totalCust : userRideCounts.size());
        data.setNewCustomers(newCust);
        data.setActiveCustomers(activeUsersInPeriod.size());
        data.setReturningCustomers(returningCust);
        data.setCustomerRetentionRatePercent(activeUsersInPeriod.size() > 0 ? (returningCust * 100.0) / activeUsersInPeriod.size() : 0.0);
        data.setAverageRidesPerActiveCustomer(activeUsersInPeriod.size() > 0 ? (double) totalBookings / activeUsersInPeriod.size() : 0.0);

        // ─── 6. COMPLAINTS & GRIEVANCES ───────────────────────────────────────────
        int openTickets = 0;
        int totalTickets = 0;
        Map<String, Integer> complaintsByCat = new HashMap<>();

        if (supportTickets != null) {
            for (SupportTicket t : supportTickets) {
                Date d = t.getCreatedAt();
                if (isWithinRange(d, startDate, endDate)) {
                    totalTickets++;
                    String st = t.getStatus() != null ? t.getStatus().toUpperCase() : SupportTicket.STATUS_OPEN;
                    if (SupportTicket.STATUS_OPEN.equalsIgnoreCase(st) || SupportTicket.STATUS_IN_PROGRESS.equalsIgnoreCase(st)
                            || SupportTicket.STATUS_ASSIGNED.equalsIgnoreCase(st) || SupportTicket.STATUS_REOPENED.equalsIgnoreCase(st)) {
                        openTickets++;
                    }
                    String cat = t.getCategory() != null ? t.getCategory() : "GENERAL";
                    complaintsByCat.put(cat, complaintsByCat.getOrDefault(cat, 0) + 1);
                }
            }
        }
        data.setOpenTicketsCount(openTickets);
        data.setTotalTicketsCount(totalTickets);
        data.setComplaintsByCategory(complaintsByCat);

        // ─── 7. BICYCLE & STATION RANKINGS ────────────────────────────────────────
        List<BusinessAnalyticsData.BicycleMetric> mostBikes = new ArrayList<>();
        List<Map.Entry<String, Integer>> sortedBikes = new ArrayList<>(bikeTripCounts.entrySet());
        sortedBikes.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (int i = 0; i < Math.min(5, sortedBikes.size()); i++) {
            Map.Entry<String, Integer> e = sortedBikes.get(i);
            mostBikes.add(new BusinessAnalyticsData.BicycleMetric(e.getKey(), e.getValue()));
        }
        data.setMostRentedBicycles(mostBikes);

        List<BusinessAnalyticsData.BicycleMetric> leastBikes = new ArrayList<>();
        List<Map.Entry<String, Integer>> sortedLeast = new ArrayList<>(bikeTripCounts.entrySet());
        sortedLeast.sort(Map.Entry.comparingByValue());
        for (int i = 0; i < Math.min(5, sortedLeast.size()); i++) {
            Map.Entry<String, Integer> e = sortedLeast.get(i);
            leastBikes.add(new BusinessAnalyticsData.BicycleMetric(e.getKey(), e.getValue()));
        }
        data.setLeastRentedBicycles(leastBikes);

        List<BusinessAnalyticsData.StationUsage> topStations = new ArrayList<>();
        List<Map.Entry<String, Integer>> sortedStations = new ArrayList<>(stationUsageCounts.entrySet());
        sortedStations.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (int i = 0; i < Math.min(5, sortedStations.size()); i++) {
            Map.Entry<String, Integer> e = sortedStations.get(i);
            topStations.add(new BusinessAnalyticsData.StationUsage(e.getKey(), e.getKey(), e.getValue()));
        }
        data.setPopularStations(topStations);

        return data;
    }

    private boolean isWithinRange(Date date, Date startDate, Date endDate) {
        if (date == null) return true;
        if (startDate != null && date.before(startDate)) return false;
        if (endDate != null && date.after(endDate)) return false;
        return true;
    }
}
