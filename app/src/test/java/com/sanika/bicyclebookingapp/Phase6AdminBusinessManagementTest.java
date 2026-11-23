package com.sanika.bicyclebookingapp;

import com.sanika.bicyclebookingapp.model.AdminSettings;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 6 Unit Tests:
 * - Admin Dashboard telemetry and real-time metric aggregation
 * - Fleet status breakdown (AVAILABLE, IN_USE, RESERVED, MAINTENANCE, DAMAGED)
 * - Financial revenue aggregation in INR with date boundaries (Today vs Monthly)
 * - Popular station and most-rented bicycle ranking algorithms
 * - Support grievance ticket lifecycle (OPEN, IN_PROGRESS, RESOLVED)
 * - Admin operations settings serialization and business policies
 * - Role privileges verification (ADMIN, HANDLER, USER)
 */
public class Phase6AdminBusinessManagementTest {

    // =========================================================================
    // 1. Fleet Status Counting Logic
    // =========================================================================

    @Test
    public void testFleetStatusAggregation() {
        List<Bicycle> fleet = new ArrayList<>();
        fleet.add(createBike("B1", Bicycle.STATUS_AVAILABLE));
        fleet.add(createBike("B2", Bicycle.STATUS_AVAILABLE));
        fleet.add(createBike("B3", Bicycle.STATUS_IN_USE));
        fleet.add(createBike("B4", Bicycle.STATUS_RESERVED));
        fleet.add(createBike("B5", Bicycle.STATUS_MAINTENANCE));
        fleet.add(createBike("B6", Bicycle.STATUS_DAMAGED));

        int total = fleet.size();
        int available = 0, inUse = 0, reserved = 0, maintenance = 0;

        for (Bicycle b : fleet) {
            switch (b.getStatus().toUpperCase()) {
                case Bicycle.STATUS_AVAILABLE:
                    available++;
                    break;
                case Bicycle.STATUS_IN_USE:
                    inUse++;
                    break;
                case Bicycle.STATUS_RESERVED:
                    reserved++;
                    break;
                case Bicycle.STATUS_MAINTENANCE:
                case Bicycle.STATUS_DAMAGED:
                    maintenance++;
                    break;
            }
        }

        assertEquals(6, total);
        assertEquals(2, available);
        assertEquals(1, inUse);
        assertEquals(1, reserved);
        assertEquals(2, maintenance);
    }

    private Bicycle createBike(String id, String status) {
        Bicycle b = new Bicycle();
        b.setBicycleId(id);
        b.setStatus(status);
        b.setAvailable(Bicycle.STATUS_AVAILABLE.equalsIgnoreCase(status));
        return b;
    }

    // =========================================================================
    // 2. Revenue Aggregation with Date Filtering (Today vs Monthly)
    // =========================================================================

    @Test
    public void testRevenueDateRangeAggregation() {
        Calendar cal = Calendar.getInstance();

        // Today's payment
        Date today = cal.getTime();

        // Payment from 10 days ago (within month)
        cal.add(Calendar.DAY_OF_MONTH, -10);
        Date earlierThisMonth = cal.getTime();

        // Payment from 45 days ago (previous month)
        cal.add(Calendar.DAY_OF_MONTH, -35);
        Date lastMonth = cal.getTime();

        List<Payment> payments = new ArrayList<>();

        Payment p1 = new Payment();
        p1.setStatus(Payment.STATUS_SUCCESS);
        p1.setAmountPaise(7000L); // ₹70 today
        p1.setPaidAt(today);
        payments.add(p1);

        Payment p2 = new Payment();
        p2.setStatus(Payment.STATUS_SUCCESS);
        p2.setAmountPaise(13000L); // ₹130 earlier this month
        p2.setPaidAt(earlierThisMonth);
        payments.add(p2);

        Payment p3 = new Payment();
        p3.setStatus(Payment.STATUS_SUCCESS);
        p3.setAmountPaise(20000L); // ₹200 last month
        p3.setPaidAt(lastMonth);
        payments.add(p3);

        Payment pPending = new Payment();
        pPending.setStatus(Payment.STATUS_PENDING);
        pPending.setAmountPaise(5000L); // Unsettled
        pPending.setPaidAt(today);
        payments.add(pPending);

        // Calculate today and month boundaries
        Calendar boundaryCal = Calendar.getInstance();
        boundaryCal.set(Calendar.HOUR_OF_DAY, 0);
        boundaryCal.set(Calendar.MINUTE, 0);
        boundaryCal.set(Calendar.SECOND, 0);
        boundaryCal.set(Calendar.MILLISECOND, 0);
        Date startOfToday = boundaryCal.getTime();

        boundaryCal.set(Calendar.DAY_OF_MONTH, 1);
        Date startOfMonth = boundaryCal.getTime();

        long todayRevenuePaise = 0L;
        long monthlyRevenuePaise = 0L;
        int pendingCount = 0;

        for (Payment p : payments) {
            if (Payment.STATUS_SUCCESS.equalsIgnoreCase(p.getStatus())) {
                Date paid = p.getPaidAt();
                if (paid != null) {
                    if (!paid.before(startOfToday)) {
                        todayRevenuePaise += p.getAmountPaise();
                    }
                    if (!paid.before(startOfMonth)) {
                        monthlyRevenuePaise += p.getAmountPaise();
                    }
                }
            } else if (Payment.STATUS_PENDING.equalsIgnoreCase(p.getStatus())) {
                pendingCount++;
            }
        }

        assertEquals(7000L, todayRevenuePaise);
        assertEquals("₹70.00", CurrencyUtil.formatInrFromPaise(todayRevenuePaise));

        assertEquals(20000L, monthlyRevenuePaise); // 70 + 130
        assertEquals("₹200.00", CurrencyUtil.formatInrFromPaise(monthlyRevenuePaise));

        assertEquals(1, pendingCount);
    }

    // =========================================================================
    // 3. Popular Station & Most Rented Bicycle Ranking
    // =========================================================================

    @Test
    public void testPopularStationAndBicycleRanking() {
        List<Booking> bookings = new ArrayList<>();
        bookings.add(createBooking("STATION_A", "BIKE_1"));
        bookings.add(createBooking("STATION_A", "BIKE_2"));
        bookings.add(createBooking("STATION_A", "BIKE_1"));
        bookings.add(createBooking("STATION_B", "BIKE_1"));
        bookings.add(createBooking("STATION_B", "BIKE_3"));

        Map<String, Integer> stationCounts = new HashMap<>();
        Map<String, Integer> bikeCounts = new HashMap<>();

        for (Booking b : bookings) {
            stationCounts.put(b.getStationName(), stationCounts.getOrDefault(b.getStationName(), 0) + 1);
            bikeCounts.put(b.getBicycleId(), bikeCounts.getOrDefault(b.getBicycleId(), 0) + 1);
        }

        String topStation = "";
        int maxStation = 0;
        for (Map.Entry<String, Integer> e : stationCounts.entrySet()) {
            if (e.getValue() > maxStation) {
                maxStation = e.getValue();
                topStation = e.getKey();
            }
        }

        String topBike = "";
        int maxBike = 0;
        for (Map.Entry<String, Integer> e : bikeCounts.entrySet()) {
            if (e.getValue() > maxBike) {
                maxBike = e.getValue();
                topBike = e.getKey();
            }
        }

        assertEquals("STATION_A", topStation);
        assertEquals(3, maxStation);

        assertEquals("BIKE_1", topBike);
        assertEquals(3, maxBike);
    }

    private Booking createBooking(String station, String bikeId) {
        Booking b = new Booking();
        b.setStationName(station);
        b.setBicycleId(bikeId);
        b.setStatus(Booking.STATUS_COMPLETED);
        return b;
    }

    // =========================================================================
    // 4. Support Grievances & Complaints Model
    // =========================================================================

    @Test
    public void testSupportTicketLifecycle() {
        SupportTicket ticket = new SupportTicket(
                "USER101", "Pooja Patil",
                SupportTicket.CATEGORY_DAMAGED_BICYCLE,
                "Chain Loose", "Chain slipped near Deccan station."
        );

        assertTrue(ticket.isOpen());
        assertEquals(SupportTicket.STATUS_OPEN, ticket.getStatus());
        assertEquals(SupportTicket.CATEGORY_DAMAGED_BICYCLE, ticket.getCategory());

        // Resolve ticket
        ticket.setStatus(SupportTicket.STATUS_RESOLVED);
        ticket.setAdminNotes("Technician fixed chain and tested bicycle.");
        ticket.setResolvedBy("ADMIN_01");
        ticket.setResolvedAt(new Date());

        assertFalse(ticket.isOpen());
        assertEquals(SupportTicket.STATUS_RESOLVED, ticket.getStatus());
        assertEquals("ADMIN_01", ticket.getResolvedBy());
        assertTrue(ticket.getAdminNotes().contains("Technician"));
    }

    // =========================================================================
    // 5. Admin Settings Defaults & Map Serialization
    // =========================================================================

    @Test
    public void testAdminSettingsSerialization() {
        AdminSettings settings = AdminSettings.createDefaultSettings();
        assertEquals("CycleGo India", settings.getBusinessName());
        assertEquals(100, settings.getGeofenceRadiusMeters());
        assertEquals(10, settings.getAutoExpireHoldMinutes());
        assertTrue(settings.isMaintenanceAlertsEnabled());
        assertTrue(settings.isAutomatedRefundsEnabled());

        Map<String, Object> map = settings.toMap();
        assertNotNull(map);
        assertEquals("CycleGo India", map.get("businessName"));

        // Deserialize
        AdminSettings recovered = AdminSettings.fromMap(map);
        assertEquals(settings.getBusinessName(), recovered.getBusinessName());
        assertEquals(settings.getOperatingHours(), recovered.getOperatingHours());
        assertEquals(settings.getEmergencyPhone(), recovered.getEmergencyPhone());
        assertEquals(settings.getGeofenceRadiusMeters(), recovered.getGeofenceRadiusMeters());
    }

    // =========================================================================
    // 6. Role Hierarchy & Privilege Checks
    // =========================================================================

    @Test
    public void testUserRoleConstantsAndPrivilegeChecks() {
        assertEquals("USER", User.ROLE_USER);
        assertEquals("ADMIN", User.ROLE_ADMIN);
        assertEquals("HANDLER", User.ROLE_HANDLER);

        User normalUser = new User();
        normalUser.setRole(User.ROLE_USER);
        normalUser.setAdmin(false);
        assertFalse(normalUser.isAdmin());

        User adminUser = new User();
        adminUser.setRole(User.ROLE_ADMIN);
        adminUser.setAdmin(true);
        assertTrue(adminUser.isAdmin());

        User handlerUser = new User();
        handlerUser.setRole(User.ROLE_HANDLER);
        assertTrue(handlerUser.isHandler());
    }
}
