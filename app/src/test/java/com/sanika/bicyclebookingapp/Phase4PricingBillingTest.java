package com.sanika.bicyclebookingapp;

import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.model.PricingConfig;
import com.sanika.bicyclebookingapp.service.BillingService;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 4 Unit Tests:
 * - India-based currency formatting (INR / ₹) with paise precision and Indian grouping
 * - Configurable pricing model & sample development pricing validation
 * - Central BillingService fare calculation:
 *   - Base booking fee + per-minute/hourly tiered usage charges
 *   - Minimum ride charge enforcement
 *   - Bicycle category multipliers (Standard, Electric, Mountain)
 *   - Promotional coupon discounts (flat and percentage with caps)
 *   - Late-return penalty calculation
 *   - Transparent GST tax breakdown (CGST + SGST)
 *   - Security deposit deduction, outstanding balance & refund reconciliation
 * - Pricing snapshot preservation in Booking to guarantee historical invoice immutability
 */
public class Phase4PricingBillingTest {

    private PricingConfig sampleConfig;

    @Before
    public void setUp() {
        // Base fee: ₹10 (1000 paise)
        // Per-min: ₹2 (200 paise)
        // Per-hr: ₹60 (6000 paise)
        // Min charge: ₹20 (2000 paise)
        // Late fee: ₹50/hr (5000 paise)
        // Security deposit disabled (0 paise)
        // GST disabled by default in base sample
        sampleConfig = PricingConfig.createSampleConfig();
        sampleConfig.setGstEnabled(false);
    }

    // =========================================================================
    // 1. Currency & Paise Precision Tests
    // =========================================================================

    @Test
    public void testRupeesToPaiseAndViceVersa() {
        assertEquals(1000L, CurrencyUtil.rupeesToPaise(10.0));
        assertEquals(250L, CurrencyUtil.rupeesToPaise(2.50));
        assertEquals(125075L, CurrencyUtil.rupeesToPaise(1250.75));

        assertEquals(10.0, CurrencyUtil.paiseToRupees(1000L), 0.001);
        assertEquals(2.50, CurrencyUtil.paiseToRupees(250L), 0.001);
        assertEquals(1250.75, CurrencyUtil.paiseToRupees(125075L), 0.001);
    }

    @Test
    public void testIndianCurrencyFormatting() {
        // Indian numbering format: e.g. ₹1,250.00, ₹1,00,000.00
        assertEquals("₹0.00", CurrencyUtil.formatInr(0.0));
        assertEquals("₹10.00", CurrencyUtil.formatInr(10.0));
        assertEquals("₹1,250.00", CurrencyUtil.formatInr(1250.0));
        assertEquals("₹1,00,000.00", CurrencyUtil.formatInr(100000.0));

        // Direct from paise
        assertEquals("₹20.00", CurrencyUtil.formatInrFromPaise(2000L));
        assertEquals("₹75.50", CurrencyUtil.formatInrFromPaise(7550L));
    }

    // =========================================================================
    // 2. Sample Development Pricing Validation
    // =========================================================================

    @Test
    public void testSamplePricingConfigurationDefaults() {
        assertTrue(sampleConfig.isSampleConfig());
        assertEquals(1000L, sampleConfig.getBaseBookingFeePaise());      // ₹10
        assertEquals(200L, sampleConfig.getPerMinuteRatePaise());        // ₹2
        assertEquals(6000L, sampleConfig.getPerHourRatePaise());          // ₹60
        assertEquals(2000L, sampleConfig.getMinimumRideChargePaise());   // ₹20
        assertFalse(sampleConfig.isSecurityDepositEnabled());
        assertEquals(10000L, sampleConfig.getSecurityDepositPaise());
        assertEquals(1.0, sampleConfig.getCategoryMultiplier("STANDARD"), 0.001);
        assertEquals(1.5, sampleConfig.getCategoryMultiplier("ELECTRIC"), 0.001);
        assertEquals(1.25, sampleConfig.getCategoryMultiplier("MOUNTAIN"), 0.001);
    }

    // =========================================================================
    // 3. Central Fare Calculation Tests (BillingService)
    // =========================================================================

    @Test
    public void testStandardRideFareCalculation() {
        // Ride: 30 minutes, 5.0 km, Standard bicycle, no coupon, no tax
        // Base fee = ₹10.00 (1000 paise)
        // Usage charge = 30 min * ₹2.00 = ₹60.00 (6000 paise)
        // Subtotal = ₹70.00 (7000 paise)
        // Total fare = ₹70.00 (7000 paise)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1001", "RIDE5001", "STANDARD",
                30L, 5.0, 0L, null, sampleConfig
        );

        assertNotNull(invoice);
        assertEquals(1000L, invoice.getBaseFeePaise());
        assertEquals(6000L, invoice.getUsageChargePaise());
        assertEquals(7000L, invoice.getSubtotalPaise());
        assertEquals(7000L, invoice.getTotalFarePaise());
        assertEquals(0L, invoice.getLateFeePaise());
        assertEquals(0L, invoice.getDiscountPaise());
        assertEquals(0L, invoice.getTotalTaxPaise());
    }

    @Test
    public void testMinimumRideChargeEnforcement() {
        // Very short ride: 2 minutes
        // Base fee = ₹10.00 (1000 paise)
        // Usage = 2 * ₹2.00 = ₹4.00 (400 paise)
        // Unadjusted subtotal = ₹14.00 (1400 paise)
        // Minimum ride charge is ₹20.00 (2000 paise) -> Total fare clamped to ₹20.00
        Invoice invoice = BillingService.calculateFare(
                "BOOK1002", "RIDE5002", "STANDARD",
                2L, 0.5, 0L, null, sampleConfig
        );

        assertEquals(1000L, invoice.getBaseFeePaise());
        assertEquals(1400L, invoice.getSubtotalPaise());
        assertEquals(2000L, invoice.getTotalFarePaise());
    }

    @Test
    public void testHourlyTieredFareCalculation() {
        // 90 minutes ride:
        // 1 full hour @ ₹60.00 = 6000 paise
        // 30 remaining minutes @ ₹2.00 = 6000 paise
        // Total usage charge = 12000 paise (₹120.00)
        // Base fee = 1000 paise (₹10.00)
        // Subtotal = 13000 paise (₹130.00)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1003", "RIDE5003", "STANDARD",
                90L, 15.0, 0L, null, sampleConfig
        );

        assertEquals(1000L, invoice.getBaseFeePaise());
        assertEquals(12000L, invoice.getUsageChargePaise());
        assertEquals(13000L, invoice.getSubtotalPaise());
        assertEquals(13000L, invoice.getTotalFarePaise());
    }

    @Test
    public void testBicycleCategoryMultiplier() {
        // Electric Bicycle multiplier is 1.5x on usage
        // 30 minutes ride:
        // Base fee = ₹10.00 (1000 paise)
        // Usage charge = (30 * ₹2) * 1.5 = ₹90.00 (9000 paise)
        // Subtotal = ₹100.00 (10000 paise)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1004", "RIDE5004", "ELECTRIC",
                30L, 8.0, 0L, null, sampleConfig
        );

        assertEquals(1000L, invoice.getBaseFeePaise());
        assertEquals(9000L, invoice.getUsageChargePaise());
        assertEquals(10000L, invoice.getSubtotalPaise());
        assertEquals(10000L, invoice.getTotalFarePaise());
    }

    // =========================================================================
    // 4. Coupons & Discounts
    // =========================================================================

    @Test
    public void testFlatCouponDiscount() {
        // Coupon: Flat ₹15 off (1500 paise)
        Coupon coupon = new Coupon("SAVE15", "FLAT", 1500L, 0L, 0L, true);

        // 30 min ride = ₹70.00 (7000 paise)
        // Discount = ₹15.00 (1500 paise)
        // Total = ₹55.00 (5500 paise)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1005", "RIDE5005", "STANDARD",
                30L, 5.0, 0L, coupon, sampleConfig
        );

        assertEquals(7000L, invoice.getSubtotalPaise());
        assertEquals(1500L, invoice.getDiscountPaise());
        assertEquals(5500L, invoice.getTotalFarePaise());
    }

    @Test
    public void testPercentageCouponWithMaxCap() {
        // Coupon: 20% off, capped at max ₹20 (2000 paise), min fare ₹50 (5000 paise)
        Coupon coupon = new Coupon("DISC20", "PERCENTAGE", 20L, 5000L, 2000L, true);

        // 90 min ride: Subtotal = ₹130.00 (13000 paise)
        // 20% of ₹130 = ₹26.00 (2600 paise) -> Capped at ₹20.00 (2000 paise)
        // Total = ₹130 - ₹20 = ₹110.00 (11000 paise)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1006", "RIDE5006", "STANDARD",
                90L, 15.0, 0L, coupon, sampleConfig
        );

        assertEquals(13000L, invoice.getSubtotalPaise());
        assertEquals(2000L, invoice.getDiscountPaise());
        assertEquals(11000L, invoice.getTotalFarePaise());
    }

    // =========================================================================
    // 5. Late Return Penalty Tests
    // =========================================================================

    @Test
    public void testLateReturnPenaltyCalculation() {
        // Set maximum duration to 180 min (3 hours)
        sampleConfig.setMaximumDurationMinutes(180);
        // Late fee rate = ₹50/hour (5000 paise)
        // Ride duration = 240 minutes (4 hours) -> 60 minutes late = 1 late hour
        // Late fee = 1 * 5000 = 5000 paise (₹50.00)
        // Normal usage: 4 hr * ₹60/hr = ₹240 (24000 paise)
        // Base = ₹10 (1000 paise)
        // Subtotal = 1000 + 24000 + 5000 (late fee) = 30000 paise (₹300.00)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1007", "RIDE5007", "STANDARD",
                240L, 30.0, 0L, null, sampleConfig
        );

        assertEquals(5000L, invoice.getLateFeePaise());
        assertEquals(24000L, invoice.getUsageChargePaise());
        assertEquals(30000L, invoice.getSubtotalPaise());
        assertEquals(30000L, invoice.getTotalFarePaise());
    }

    // =========================================================================
    // 6. Indian GST Tax Calculation Tests
    // =========================================================================

    @Test
    public void testGstTaxSplitCalculation() {
        // Enable GST: 5%
        sampleConfig.setGstEnabled(true);
        sampleConfig.setGstPercentage(5.0);

        // Ride: 30 minutes, Subtotal = ₹70.00 (7000 paise)
        // 5% GST on 7000 paise = 350 paise (₹3.50)
        // CGST (2.5%) = 175 paise (₹1.75)
        // SGST (2.5%) = 175 paise (₹1.75)
        // Total Fare = 7000 + 350 = 7350 paise (₹73.50)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1008", "RIDE5008", "STANDARD",
                30L, 5.0, 0L, null, sampleConfig
        );

        assertEquals(7000L, invoice.getSubtotalPaise());
        assertEquals(350L, invoice.getTotalTaxPaise());
        assertEquals(175L, invoice.getCgstPaise());
        assertEquals(175L, invoice.getSgstPaise());
        assertEquals(7350L, invoice.getTotalFarePaise());
    }

    // =========================================================================
    // 7. Security Deposit Reconciliation Tests
    // =========================================================================

    @Test
    public void testDepositReconciliationWithRefund() {
        // Customer paid security deposit: ₹200.00 (20000 paise)
        // Total ride fare: ₹70.00 (7000 paise)
        // Deposit settles ride fare:
        // Amount Paid = 7000 paise (settled from deposit)
        // Amount Outstanding = 0 paise
        // Refund Amount = 20000 - 7000 = 13000 paise (₹130.00)
        Invoice invoice = BillingService.calculateFare(
                "BOOK1009", "RIDE5009", "STANDARD",
                30L, 5.0, 20000L, null, sampleConfig
        );

        assertEquals(7000L, invoice.getTotalFarePaise());
        assertEquals(20000L, invoice.getDepositPaidPaise());
        assertEquals(7000L, invoice.getAmountPaidPaise());
        assertEquals(0L, invoice.getAmountOutstandingPaise());
        assertEquals(13000L, invoice.getRefundAmountPaise());
    }

    @Test
    public void testDepositReconciliationWithOutstandingBalance() {
        // Customer paid security deposit: ₹50.00 (5000 paise)
        // Total ride fare: ₹70.00 (7000 paise)
        // Deposit settles partial fare:
        // Amount Paid = 5000 paise
        // Amount Outstanding = 7000 - 5000 = 2000 paise (₹20.00)
        // Refund Amount = 0 paise
        Invoice invoice = BillingService.calculateFare(
                "BOOK1010", "RIDE5010", "STANDARD",
                30L, 5.0, 5000L, null, sampleConfig
        );

        assertEquals(7000L, invoice.getTotalFarePaise());
        assertEquals(5000L, invoice.getDepositPaidPaise());
        assertEquals(5000L, invoice.getAmountPaidPaise());
        assertEquals(2000L, invoice.getAmountOutstandingPaise());
        assertEquals(0L, invoice.getRefundAmountPaise());
    }

    // =========================================================================
    // 8. Pricing Snapshot & Immutability Test
    // =========================================================================

    @Test
    public void testPricingSnapshotPreservationInBooking() {
        // Create booking with current snapshot
        Booking booking = new Booking();
        booking.setBookingId("BOOK_SNAPSHOT_TEST");
        booking.setBicycleType("MOUNTAIN");
        booking.setDepositAmount(0.0);
        booking.setPricingSnapshot(sampleConfig.toMap());

        // Verify snapshot preserves original rates
        Map<String, Object> snapshot = booking.getPricingSnapshot();
        assertNotNull(snapshot);
        assertEquals(1000L, ((Number) snapshot.get("baseBookingFeePaise")).longValue());
        assertEquals(200L, ((Number) snapshot.get("perMinuteRatePaise")).longValue());

        // Now simulate Admin changing base rates in config
        sampleConfig.setBaseBookingFeePaise(5000L); // Changed to ₹50
        sampleConfig.setPerMinuteRatePaise(1000L);   // Changed to ₹10

        // Recover pricing config from booking snapshot
        PricingConfig historicalConfig = PricingConfig.fromMap(booking.getPricingSnapshot());

        // The historical config must still hold original ₹10 base fee and ₹2/min rate
        assertEquals(1000L, historicalConfig.getBaseBookingFeePaise());
        assertEquals(200L, historicalConfig.getPerMinuteRatePaise());
    }
}
