package com.sanika.bicyclebookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import com.sanika.bicyclebookingapp.service.AnalyticsService;
import com.sanika.bicyclebookingapp.service.ReportExporterService;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 11 Comprehensive Unit Tests:
 * - Real Business Metrics Computation (Daily, Weekly, Monthly, Yearly)
 * - Financial Reconciliation (Gross, Payments Received, Refunds, Pending, Net Revenue in PAISE)
 * - Operational & Fleet Telemetry (Utilization, Availability, Overdue rides, Avg Distance & Duration)
 * - Fleet & Customer Analytics (Rankings, Retention, Ratings, Maintenance costs)
 * - RFC-4180 CSV Export Generation for all 9 Reports
 * - Executive Printable Summary Generation
 */
public class Phase11AnalyticsReportingTest {

    private AnalyticsService analyticsService;
    private ReportExporterService reportExporterService;

    private List<Booking> sampleBookings;
    private List<Payment> samplePayments;
    private List<Refund> sampleRefunds;
    private List<Invoice> sampleInvoices;
    private List<Bicycle> sampleBicycles;
    private List<Station> sampleStations;
    private List<MaintenanceRecord> sampleMaintenance;
    private List<User> sampleUsers;
    private List<RideReview> sampleReviews;
    private List<SupportTicket> sampleTickets;

    @Before
    public void setUp() {
        analyticsService = AnalyticsService.getInstance();
        reportExporterService = ReportExporterService.getInstance();

        sampleBookings = new ArrayList<>();
        samplePayments = new ArrayList<>();
        sampleRefunds = new ArrayList<>();
        sampleInvoices = new ArrayList<>();
        sampleBicycles = new ArrayList<>();
        sampleStations = new ArrayList<>();
        sampleMaintenance = new ArrayList<>();
        sampleUsers = new ArrayList<>();
        sampleReviews = new ArrayList<>();
        sampleTickets = new ArrayList<>();

        // 1. Users (3 users)
        User u1 = new User("usr_1", "Aarav Sharma", "aarav@example.com", "9876543210");
        u1.setCreatedAt(new Date(System.currentTimeMillis() - 86400000L * 30)); // 30 days ago
        User u2 = new User("usr_2", "Diya Patel", "diya@example.com", "9876543211");
        u2.setCreatedAt(new Date(System.currentTimeMillis() - 86400000L * 2)); // 2 days ago
        User u3 = new User("usr_3", "Rohan Mehta", "rohan@example.com", "9876543212");
        u3.setCreatedAt(new Date()); // today
        sampleUsers.add(u1);
        sampleUsers.add(u2);
        sampleUsers.add(u3);

        // 2. Bicycles (4 bikes: 2 available, 1 in use, 1 maintenance)
        Bicycle b1 = new Bicycle("BIKE_101", "STN_1", "Standard");
        b1.setStatus(Bicycle.STATUS_AVAILABLE);
        b1.setAverageRating(4.8);

        Bicycle b2 = new Bicycle("BIKE_102", "STN_1", "Electric");
        b2.setStatus(Bicycle.STATUS_IN_USE);
        b2.setAverageRating(4.5);

        Bicycle b3 = new Bicycle("BIKE_103", "STN_2", "Mountain");
        b3.setStatus(Bicycle.STATUS_MAINTENANCE);
        b3.setAverageRating(4.0);

        Bicycle b4 = new Bicycle("BIKE_104", "STN_2", "Standard");
        b4.setStatus(Bicycle.STATUS_AVAILABLE);
        b4.setAverageRating(5.0);

        sampleBicycles.add(b1);
        sampleBicycles.add(b2);
        sampleBicycles.add(b3);
        sampleBicycles.add(b4);

        // 3. Stations (2 stations)
        Station s1 = new Station("STN_1", "MG Road Metro", "MG Road Central", 12.9716, 77.5946, 10);
        s1.setAvailableBicycles(5);
        s1.setAverageRating(4.7);

        Station s2 = new Station("STN_2", "Indiranagar 100ft", "Indiranagar Hub", 12.9784, 77.6408, 10);
        s2.setAvailableBicycles(4);
        s2.setAverageRating(4.9);

        sampleStations.add(s1);
        sampleStations.add(s2);

        // 4. Bookings & Rides
        // Ride 1: User 1, BIKE_101, COMPLETED, 30 mins, 4.0 km, ₹150 (15000 paise)
        Booking bk1 = new Booking();
        bk1.setBookingId("BK_001");
        bk1.setUserId("usr_1");
        bk1.setBicycleId("BIKE_101");
        bk1.setStationId("STN_1");
        bk1.setStationName("MG Road Metro");
        bk1.setReturnStationId("STN_2");
        bk1.setReturnStationName("Indiranagar 100ft");
        bk1.setStatus(Booking.STATUS_COMPLETED);
        bk1.setStartTime(new Date(System.currentTimeMillis() - 3600000L));
        bk1.setEndTime(new Date(System.currentTimeMillis() - 1800000L));
        bk1.setDurationMinutes(30);
        bk1.setDistanceKm(4.0);
        bk1.setFare(150.0);
        bk1.setPaymentStatus(Payment.STATUS_SUCCESS);
        sampleBookings.add(bk1);

        // Ride 2: User 1 (returning customer!), BIKE_101, COMPLETED, 20 mins, 2.5 km, ₹100
        Booking bk2 = new Booking();
        bk2.setBookingId("BK_002");
        bk2.setUserId("usr_1");
        bk2.setBicycleId("BIKE_101");
        bk2.setStationId("STN_1");
        bk2.setStatus(Booking.STATUS_COMPLETED);
        bk2.setStartTime(new Date(System.currentTimeMillis() - 172800000L)); // 2 days ago
        bk2.setEndTime(new Date(System.currentTimeMillis() - 171600000L));
        bk2.setDurationMinutes(20);
        bk2.setDistanceKm(2.5);
        bk2.setFare(100.0);
        bk2.setPaymentStatus(Payment.STATUS_SUCCESS);
        sampleBookings.add(bk2);

        // Ride 3: User 2, BIKE_102, ACTIVE (currently riding for 5 hours -> Overdue!)
        Booking bk3 = new Booking();
        bk3.setBookingId("BK_003");
        bk3.setUserId("usr_2");
        bk3.setBicycleId("BIKE_102");
        bk3.setStationId("STN_2");
        bk3.setStatus(Booking.STATUS_ACTIVE);
        bk3.setStartTime(new Date(System.currentTimeMillis() - 5 * 3600000L)); // 5 hours ago
        sampleBookings.add(bk3);

        // Booking 4: User 3, BIKE_104, CANCELLED
        Booking bk4 = new Booking();
        bk4.setBookingId("BK_004");
        bk4.setUserId("usr_3");
        bk4.setBicycleId("BIKE_104");
        bk4.setStationId("STN_2");
        bk4.setStatus(Booking.STATUS_CANCELLED);
        sampleBookings.add(bk4);

        // 5. Invoices (Total Invoiced: ₹250.00 = 25000 paise)
        Invoice inv1 = new Invoice();
        inv1.setInvoiceId("INV_001");
        inv1.setBookingId("BK_001");
        inv1.setUserId("usr_1");
        inv1.setTotalAmountPaise(15000L); // ₹150.00
        inv1.setPaymentStatus("PAID");
        inv1.setStartTime(bk1.getStartTime());
        sampleInvoices.add(inv1);

        Invoice inv2 = new Invoice();
        inv2.setInvoiceId("INV_002");
        inv2.setBookingId("BK_002");
        inv2.setUserId("usr_1");
        inv2.setTotalAmountPaise(10000L); // ₹100.00
        inv2.setPaymentStatus("PAID");
        inv2.setStartTime(bk2.getStartTime());
        sampleInvoices.add(inv2);

        // 6. Payments
        // Pay 1: ₹150.00 SUCCESS
        Payment p1 = new Payment("BK_001", "INV_001", "usr_1", 15000L, Payment.METHOD_UPI);
        p1.setPaymentId("PAY_001");
        p1.setStatus(Payment.STATUS_SUCCESS);
        p1.setCreatedAt(new Date());
        samplePayments.add(p1);

        // Pay 2: ₹100.00 SUCCESS
        Payment p2 = new Payment("BK_002", "INV_002", "usr_1", 10000L, Payment.METHOD_DEBIT_CARD);
        p2.setPaymentId("PAY_002");
        p2.setStatus(Payment.STATUS_SUCCESS);
        p2.setCreatedAt(new Date(System.currentTimeMillis() - 172800000L));
        samplePayments.add(p2);

        // Pay 3: ₹50.00 FAILED
        Payment p3 = new Payment("BK_004", "INV_004", "usr_3", 5000L, Payment.METHOD_UPI);
        p3.setPaymentId("PAY_003");
        p3.setStatus(Payment.STATUS_FAILED);
        p3.setFailureReason("User aborted UPI PIN entry");
        p3.setCreatedAt(new Date());
        samplePayments.add(p3);

        // 7. Refunds: ₹20.00 PROCESSED (2000 paise)
        Refund ref1 = new Refund("PAY_001", "BK_001", "usr_1", 2000L, Refund.REASON_CANCELLATION);
        ref1.setRefundId("REF_001");
        ref1.setStatus(Refund.STATUS_PROCESSED);
        ref1.setCreatedAt(new Date());
        sampleRefunds.add(ref1);

        // 8. Maintenance: BIKE_103 brake overhaul, cost ₹450.00 (45000 paise)
        MaintenanceRecord mr1 = new MaintenanceRecord();
        mr1.setRecordId("MNT_001");
        mr1.setBicycleId("BIKE_103");
        mr1.setStatus(MaintenanceRecord.STATUS_COMPLETED);
        mr1.setTotalCostPaise(45000L);
        mr1.setDamageCategory(MaintenanceRecord.CAT_BRAKES);
        mr1.setReportedAt(new Date());
        sampleMaintenance.add(mr1);

        // 9. Review: 5 stars
        RideReview rev1 = new RideReview("BK_001", "usr_1", "Aarav", "BIKE_101", 5.0f, 5.0f, RideReview.CONDITION_EXCELLENT, "Smooth ride!");
        rev1.setCreatedAt(new Date());
        sampleReviews.add(rev1);

        // 10. Support Ticket: 1 open ticket
        SupportTicket st1 = new SupportTicket();
        st1.setTicketId("TKT_001");
        st1.setUserId("usr_3");
        st1.setStatus(SupportTicket.STATUS_OPEN);
        st1.setSubject("UPI Payment failed but money deducted");
        sampleTickets.add(st1);
    }

    @Test
    public void testComprehensiveBusinessMetricsCalculation() {
        BusinessAnalyticsData data = analyticsService.computeAnalytics(
                sampleBookings, samplePayments, sampleRefunds, sampleInvoices,
                sampleBicycles, sampleStations, sampleMaintenance, sampleUsers,
                sampleReviews, sampleTickets, null, null
        );

        assertNotNull("BusinessAnalyticsData must not be null", data);

        // 1. Ride counts
        assertEquals("Total completed rides must be 2", 2, data.getTotalCompletedRides());
        assertEquals("Total bookings must be 4", 4, data.getTotalBookings());
        assertEquals("Active rides must be 1", 1, data.getActiveRides());
        assertEquals("Overdue rides (>4 hrs) must be 1", 1, data.getOverdueRides());
        assertEquals("Cancelled bookings must be 1", 1, data.getCancelledBookings());
        assertEquals("Cancellation rate should be 25.0%", 25.0, data.getCancellationRate(), 0.01);

        // 2. Financial Metrics (Paise & INR)
        // Payments Received = 15000 + 10000 = 25000 paise (₹250.00)
        assertEquals(25000L, data.getPaymentsReceivedPaise());
        // Refunds = 2000 paise (₹20.00)
        assertEquals(2000L, data.getTotalRefundsPaise());
        // Net Revenue = 25000 - 2000 = 23000 paise (₹230.00)
        assertEquals(23000L, data.getNetRevenuePaise());
        assertEquals("₹230.00", data.getFormattedNetRevenue());

        // Avg Revenue per ride = 25000 / 2 = 12500 paise (₹125.00)
        assertEquals(12500L, data.getAvgRevenuePerRidePaise());
        assertEquals("₹125.00", data.getFormattedAvgRevenuePerRide());

        // Failed payments
        assertEquals(1, data.getPaymentFailures());

        // 3. Fleet Operational Metrics
        // Total bikes = 4, Available = 2, In Use = 1, Maintenance = 1
        assertEquals(4, data.getTotalBicycles());
        assertEquals(2, data.getAvailableBicycles());
        assertEquals(1, data.getInUseBicycles());
        assertEquals(1, data.getMaintenanceBicycles());
        // Availability rate: 2/4 = 50.0%
        assertEquals(50.0, data.getFleetAvailabilityRate(), 0.01);
        // Utilization rate: 1/4 = 25.0%
        assertEquals(25.0, data.getFleetUtilizationRate(), 0.01);

        // Average duration = (30 + 20) / 2 = 25 mins
        assertEquals(25, data.getAvgRideDurationMinutes());
        // Average distance = (4.0 + 2.5) / 2 = 3.25 km
        assertEquals(3.25, data.getAvgDistanceKm(), 0.01);

        // 4. Fleet Maintenance Costs
        assertEquals(45000L, data.getTotalMaintenanceCostsPaise());
        assertEquals("₹450.00", data.getFormattedMaintenanceCosts());

        // 5. Customer Metrics
        // Total users = 3, Active users (with rides/bookings) = 3
        assertEquals(3, data.getTotalCustomers());
        assertEquals(3, data.getActiveCustomers());
        // Returning customers = 1 (usr_1 has 2 bookings)
        assertEquals(1, data.getReturningCustomers());

        // Customer rating
        assertEquals(5.0, data.getAvgCustomerRating(), 0.01);

        // Support tickets
        assertEquals(1, data.getTotalTickets());
        assertEquals(1, data.getOpenTickets());
    }

    @Test
    public void testDateRangeFiltering() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -2);
        Date startDate = cal.getTime(); // Last 2 hours only

        BusinessAnalyticsData filteredData = analyticsService.computeAnalytics(
                sampleBookings, samplePayments, sampleRefunds, sampleInvoices,
                sampleBicycles, sampleStations, sampleMaintenance, sampleUsers,
                sampleReviews, sampleTickets, startDate, new Date()
        );

        // BK_002 was 2 days ago, so only BK_001 should be within the last 2 hours
        assertEquals(1, filteredData.getTotalCompletedRides());
        // Revenue in last 2 hours should only be PAY_001 (₹150.00 = 15000 paise - 2000 refund = 13000 paise)
        assertEquals(15000L, filteredData.getPaymentsReceivedPaise());
        assertEquals(13000L, filteredData.getNetRevenuePaise());
    }

    @Test
    public void testAllNineReportCsvExports() {
        // 1. Booking Report
        String bookingCsv = reportExporterService.exportBookingReport(sampleBookings);
        assertNotNull(bookingCsv);
        assertTrue(bookingCsv.startsWith("\uFEFF")); // UTF-8 BOM
        assertTrue(bookingCsv.contains("Booking ID,User ID,Bicycle ID"));
        assertTrue(bookingCsv.contains("BK_001"));
        assertTrue(bookingCsv.contains("COMPLETED"));

        // 2. Ride Report
        String rideCsv = reportExporterService.exportRideReport(sampleBookings);
        assertNotNull(rideCsv);
        assertTrue(rideCsv.contains("Ride ID,User ID,Bicycle ID,Bicycle Type"));
        assertTrue(rideCsv.contains("BK_001"));
        assertTrue(rideCsv.contains("MG Road Metro"));

        // 3. Revenue Report
        String revenueCsv = reportExporterService.exportRevenueReport(sampleInvoices);
        assertNotNull(revenueCsv);
        assertTrue(revenueCsv.contains("Invoice ID,Booking ID,User ID"));
        assertTrue(revenueCsv.contains("INV_001"));
        assertTrue(revenueCsv.contains("150.00"));

        // 4. Payment Report
        String paymentCsv = reportExporterService.exportPaymentReport(samplePayments);
        assertNotNull(paymentCsv);
        assertTrue(paymentCsv.contains("Payment ID,Booking ID,User ID,Order ID,Transaction ID,Method,Amount (INR)"));
        assertTrue(paymentCsv.contains("PAY_001"));
        assertTrue(paymentCsv.contains("UPI"));

        // 5. Refund Report
        String refundCsv = reportExporterService.exportRefundReport(sampleRefunds);
        assertNotNull(refundCsv);
        assertTrue(refundCsv.contains("Refund ID,Payment ID,Booking ID,User ID,Amount (INR)"));
        assertTrue(refundCsv.contains("REF_001"));
        assertTrue(refundCsv.contains("20.00"));

        // 6. Fleet Report
        Map<String, Integer> rideCounts = new HashMap<>();
        rideCounts.put("BIKE_101", 2);
        Map<String, Long> maintCosts = new HashMap<>();
        maintCosts.put("BIKE_103", 45000L);
        String fleetCsv = reportExporterService.exportFleetReport(sampleBicycles, rideCounts, maintCosts);
        assertNotNull(fleetCsv);
        assertTrue(fleetCsv.contains("Bicycle ID,Type,Status"));
        assertTrue(fleetCsv.contains("BIKE_101"));
        assertTrue(fleetCsv.contains("450.00"));

        // 7. Maintenance Report
        String maintCsv = reportExporterService.exportMaintenanceReport(sampleMaintenance);
        assertNotNull(maintCsv);
        assertTrue(maintCsv.contains("Record ID,Bicycle ID,Category,Priority,Status"));
        assertTrue(maintCsv.contains("MNT_001"));
        assertTrue(maintCsv.contains("450.00"));

        // 8. Station Report
        Map<String, Integer> pickups = new HashMap<>();
        pickups.put("STN_1", 2);
        Map<String, Integer> dropoffs = new HashMap<>();
        dropoffs.put("STN_2", 1);
        String stationCsv = reportExporterService.exportStationReport(sampleStations, pickups, dropoffs);
        assertNotNull(stationCsv);
        assertTrue(stationCsv.contains("Station ID,Station Name,Address,Total Capacity"));
        assertTrue(stationCsv.contains("MG Road Metro"));

        // 9. Customer Report
        Map<String, Integer> userBookings = new HashMap<>();
        userBookings.put("usr_1", 2);
        Map<String, Long> userSpent = new HashMap<>();
        userSpent.put("usr_1", 25000L);
        String customerCsv = reportExporterService.exportCustomerReport(sampleUsers, userBookings, userSpent);
        assertNotNull(customerCsv);
        assertTrue(customerCsv.contains("User ID,Name,Email,Phone"));
        assertTrue(customerCsv.contains("Aarav Sharma"));
        assertTrue(customerCsv.contains("250.00"));
    }

    @Test
    public void testPrintableExecutiveSummary() {
        BusinessAnalyticsData data = analyticsService.computeAnalytics(
                sampleBookings, samplePayments, sampleRefunds, sampleInvoices,
                sampleBicycles, sampleStations, sampleMaintenance, sampleUsers,
                sampleReviews, sampleTickets, null, null
        );

        String summary = reportExporterService.generatePrintableSummary(data, "All Time");
        assertNotNull(summary);
        assertTrue(summary.contains("CYCLEGO BICYCLE RENTALS - BUSINESS REPORT"));
        assertTrue(summary.contains("NET REVENUE:"));
        assertTrue(summary.contains("₹230.00"));
        assertTrue(summary.contains("Avg Revenue / Ride:  ₹125.00"));
        assertTrue(summary.contains("Completed Rides:     2"));
        assertTrue(summary.contains("Fleet Availability:  50.0%"));
        assertTrue(summary.contains("Total Fleet Size:    4 bicycles"));
        assertTrue(summary.contains("Total Maint Costs:   ₹450.00"));
        assertTrue(summary.contains("END OF REPORT"));
    }

    @Test
    public void testRfc4180SpecialCharacterEscaping() {
        List<Booking> testList = new ArrayList<>();
        Booking specialBooking = new Booking();
        specialBooking.setBookingId("BK_COMMA,QUOTE\"AND\nNEWLINE");
        specialBooking.setUserId("usr_special");
        specialBooking.setStatus("COMPLETED");
        testList.add(specialBooking);

        String csv = reportExporterService.exportBookingReport(testList);
        // Double quotes must be escaped to "" and wrapped in quotes
        assertTrue(csv.contains("\"BK_COMMA,QUOTE\"\"AND\nNEWLINE\""));
    }
}
