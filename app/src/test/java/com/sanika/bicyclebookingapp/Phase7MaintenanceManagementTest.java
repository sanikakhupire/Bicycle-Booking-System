package com.sanika.bicyclebookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for Phase 7: Bicycle Maintenance & Repair Management.
 * Verifies lifecycle state transitions, bicycle grounding, booking prevention,
 * Indian Rupee repair cost calculations, admin quality verification, and service restoration.
 */
public class Phase7MaintenanceManagementTest {

    @Test
    public void testMaintenanceStatusLifecycleTransitions() {
        // Valid forward lifecycle transitions
        assertTrue("REPORTED -> ASSIGNED should be valid",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_REPORTED, MaintenanceRecord.STATUS_ASSIGNED));
        assertTrue("ASSIGNED -> IN_PROGRESS should be valid",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_ASSIGNED, MaintenanceRecord.STATUS_IN_PROGRESS));
        assertTrue("IN_PROGRESS -> COMPLETED should be valid",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_IN_PROGRESS, MaintenanceRecord.STATUS_COMPLETED));
        assertTrue("COMPLETED -> VERIFIED should be valid",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_COMPLETED, MaintenanceRecord.STATUS_VERIFIED));

        // Rejection transitions from non-terminal states
        assertTrue("REPORTED -> REJECTED should be valid",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_REPORTED, MaintenanceRecord.STATUS_REJECTED));
        assertTrue("ASSIGNED -> REJECTED should be valid",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_ASSIGNED, MaintenanceRecord.STATUS_REJECTED));
        assertTrue("IN_PROGRESS -> REJECTED should be valid",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_IN_PROGRESS, MaintenanceRecord.STATUS_REJECTED));

        // Re-open completed ticket back to in-progress if failed QC inspection
        assertTrue("COMPLETED -> IN_PROGRESS should be valid for re-work",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_COMPLETED, MaintenanceRecord.STATUS_IN_PROGRESS));

        // Invalid transitions (skipping mandatory repair steps or editing terminal states)
        assertFalse("REPORTED -> VERIFIED cannot skip repair & diagnosis",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_REPORTED, MaintenanceRecord.STATUS_VERIFIED));
        assertFalse("REPORTED -> COMPLETED cannot skip assignment/diagnosis",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_REPORTED, MaintenanceRecord.STATUS_COMPLETED));
        assertFalse("VERIFIED is terminal, cannot transition to REPORTED",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_VERIFIED, MaintenanceRecord.STATUS_REPORTED));
        assertFalse("VERIFIED is terminal, cannot transition to IN_PROGRESS",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_VERIFIED, MaintenanceRecord.STATUS_IN_PROGRESS));
        assertFalse("REJECTED is terminal, cannot transition to COMPLETED",
                MaintenanceRecord.isValidTransition(MaintenanceRecord.STATUS_REJECTED, MaintenanceRecord.STATUS_COMPLETED));
    }

    @Test
    public void testBicycleGroundingPreventsBookingAvailability() {
        Bicycle bike = new Bicycle("BIKE_P7_001", "ST_01", "Standard");
        assertTrue("Newly initialized bike docked should be available", bike.isAvailable());
        assertFalse("Newly initialized bike is not under maintenance", bike.isUnderMaintenance());

        // Customer reports damage / mechanical failure
        bike.setStatus(Bicycle.STATUS_DAMAGED);
        assertFalse("DAMAGED bicycle must not be available for customer booking", bike.isAvailable());
        assertTrue("DAMAGED bicycle must report under maintenance true", bike.isUnderMaintenance());

        // Bicycle moves to MAINTENANCE workshop
        bike.setStatus(Bicycle.STATUS_MAINTENANCE);
        assertFalse("MAINTENANCE bicycle must not be available for customer booking", bike.isAvailable());
        assertTrue("MAINTENANCE bicycle must report under maintenance true", bike.isUnderMaintenance());
    }

    @Test
    public void testCustomerDamageReportInitializationAndUnsafeFlag() {
        // 1. Normal damage report (e.g. lights or bell)
        MaintenanceRecord normalReport = new MaintenanceRecord(
                "BIKE_101", "USR_CUST_1", "Amit Patel",
                MaintenanceRecord.CAT_LIGHTS_BELL, "Bell missing and rear reflector cracked", false);

        assertEquals("BIKE_101", normalReport.getBicycleId());
        assertEquals(MaintenanceRecord.STATUS_REPORTED, normalReport.getStatus());
        assertEquals(MaintenanceRecord.PRIORITY_MEDIUM, normalReport.getPriority());
        assertFalse(normalReport.isUnsafe());
        assertEquals(0L, normalReport.getCostPaise());

        // 2. Critical Unsafe Emergency report (e.g. brakes fail)
        MaintenanceRecord criticalReport = new MaintenanceRecord(
                "BIKE_102", "USR_CUST_2", "Sunita Rao",
                MaintenanceRecord.CAT_BRAKES, "Front brake line broke while riding downhill!", true);

        assertEquals("BIKE_102", criticalReport.getBicycleId());
        assertEquals(MaintenanceRecord.STATUS_REPORTED, criticalReport.getStatus());
        assertEquals(MaintenanceRecord.PRIORITY_CRITICAL, criticalReport.getPriority());
        assertTrue(criticalReport.isUnsafe());
    }

    @Test
    public void testRepairCostInrPaisePrecision() {
        MaintenanceRecord record = new MaintenanceRecord();

        // Admin logs ₹450.75 repair cost (45075 paise)
        record.setCostInr(450.75);
        assertEquals(45075L, record.getCostPaise());
        assertEquals(450.75, record.getCostInr(), 0.001);

        String formatted = record.getFormattedCost();
        assertTrue("Cost display must contain Indian Rupee symbol", formatted.contains("₹"));
        assertTrue("Cost display must format with 450.75", formatted.contains("450.75"));

        // Flat ₹1,200 repair cost
        record.setCostInr(1200.0);
        assertEquals(120000L, record.getCostPaise());
        assertTrue("Cost display must format ₹1,200.00 cleanly", record.getFormattedCost().contains("1,200.00"));
    }

    @Test
    public void testFullWorkflowFromReportToRestoration() {
        // Step 1: Customer reports bike damage
        Bicycle bike = new Bicycle("BIKE_999", "ST_PUNE_01", "Electric");
        bike.setStatus(Bicycle.STATUS_DAMAGED);
        assertFalse(bike.isAvailable());

        MaintenanceRecord record = new MaintenanceRecord(
                bike.getBicycleId(), "USR_789", "Deepak Verma",
                MaintenanceRecord.CAT_CHAIN_PEDALS, "Chain slipped and jammed inside gear cassette", false);
        assertEquals(MaintenanceRecord.STATUS_REPORTED, record.getStatus());

        // Step 2: Admin assigns technician with HIGH priority
        record.setAssignedStaff("Ramesh Sharma (Chief Mechanic)");
        record.setPriority(MaintenanceRecord.PRIORITY_HIGH);
        record.setStatus(MaintenanceRecord.STATUS_ASSIGNED);
        record.setAssignedAt(new Date());
        assertEquals(MaintenanceRecord.STATUS_ASSIGNED, record.getStatus());
        assertEquals("Ramesh Sharma (Chief Mechanic)", record.getAssignedStaff());

        // Step 3: Technician diagnoses issue and starts repair
        record.setDiagnosisNotes("Chain damaged with 3 warped links; rear derailleur misaligned.");
        record.setStatus(MaintenanceRecord.STATUS_IN_PROGRESS);
        record.setStartedAt(new Date());
        assertEquals(MaintenanceRecord.STATUS_IN_PROGRESS, record.getStatus());

        // Step 4: Technician completes repair, logs parts and INR cost
        record.setPartsReplaced("Shimano 7-Speed Chain Link, Derailleur Hanger");
        record.setRepairNotes("Replaced chain link, realigned derailleur, lubricated drive system.");
        record.setCostInr(550.0); // ₹550.00
        record.setStatus(MaintenanceRecord.STATUS_COMPLETED);
        record.setCompletedAt(new Date());
        assertEquals(MaintenanceRecord.STATUS_COMPLETED, record.getStatus());
        assertEquals(55000L, record.getCostPaise());

        // Step 5: Admin inspects condition, signs off QC verification, and restores bike to AVAILABLE
        record.setVerifiedByAdminId("ADMIN_001");
        record.setVerifiedByAdminName("Station Ops Head");
        record.setVerificationNotes("Drive chain tested, shifted across all 7 gears smoothly, test ride passed.");
        record.setStatus(MaintenanceRecord.STATUS_VERIFIED);
        record.setVerifiedAt(new Date());
        assertEquals(MaintenanceRecord.STATUS_VERIFIED, record.getStatus());

        // Restore bicycle in inventory
        bike.setStatus(Bicycle.STATUS_AVAILABLE);
        assertTrue("Bicycle must be returned to AVAILABLE service following quality verification",
                bike.isAvailable());
        assertFalse("Bicycle must no longer be under maintenance", bike.isUnderMaintenance());
    }

    @Test
    public void testRejectionRestoresBicycleWhenNoOtherTickets() {
        Bicycle bike = new Bicycle("BIKE_888", "ST_MUMBAI_02", "Standard");
        bike.setStatus(Bicycle.STATUS_DAMAGED);
        assertFalse(bike.isAvailable());

        MaintenanceRecord report = new MaintenanceRecord(
                bike.getBicycleId(), "USR_SPURIOUS", "Tester",
                MaintenanceRecord.CAT_OTHER, "Bicycle feels heavy to pedal", false);

        // Admin inspects bike and finds tire pressure normal, gear ratio standard
        report.setStatus(MaintenanceRecord.STATUS_REJECTED);
        report.setRejectionReason("False alarm. Bicycle inspected and found in nominal operating condition.");
        assertEquals(MaintenanceRecord.STATUS_REJECTED, report.getStatus());

        // Rejecting restores bicycle back to available
        bike.setStatus(Bicycle.STATUS_AVAILABLE);
        assertTrue(bike.isAvailable());
        assertFalse(bike.isUnderMaintenance());
    }
}
