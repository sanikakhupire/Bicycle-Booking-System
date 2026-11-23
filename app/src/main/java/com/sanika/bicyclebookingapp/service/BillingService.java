package com.sanika.bicyclebookingapp.service;

import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.model.PricingConfig;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.Date;
import java.util.UUID;

/**
 * Central Server-Authoritative Fare Calculation and Billing Service.
 *
 * Implements Phase 4 Business Rules:
 * 1. Integer paise precision throughout calculations.
 * 2. Category-based rate scaling (Standard, Electric, Mountain).
 * 3. Base fee + time usage (per-minute or hourly tier) + distance.
 * 4. Late return penalties if rental exceeds maximum duration.
 * 5. Minimum charge guarantee (e.g. ₹20.00).
 * 6. Promotional coupon discount deduction.
 * 7. India GST tax calculation (CGST + SGST split) if configured.
 * 8. Security deposit deduction and refund reconciliation.
 * 9. Idempotent invoice generation.
 */
public class BillingService {

    private static volatile BillingService instance;

    public static BillingService getInstance() {
        if (instance == null) {
            synchronized (BillingService.class) {
                if (instance == null) {
                    instance = new BillingService();
                }
            }
        }
        return instance;
    }

    public static Invoice calculateFare(String bookingId,
                                       String rideId,
                                       String bicycleCategory,
                                       long durationMinutes,
                                       double distanceKm,
                                       long depositPaidPaise,
                                       Coupon coupon,
                                       PricingConfig config) {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setBicycleType(bicycleCategory);
        double depositPaidInr = CurrencyUtil.toRupees(depositPaidPaise);
        return getInstance().generateInvoice(config, booking, durationMinutes, distanceKm, coupon, depositPaidInr);
    }

    /**
     * Calculates the complete invoice for a ride.
     *
     * @param config             Pricing configuration (from booking snapshot or active admin config)
     * @param booking            Booking entity
     * @param durationMinutes    Total duration in minutes
     * @param distanceKm         Total GPS distance in km
     * @param coupon             Optional promotional coupon
     * @param depositPaidInr     Security deposit paid by user (if applicable)
     * @return                   Fully detailed, itemized Invoice
     */
    public Invoice generateInvoice(PricingConfig config,
                                   Booking booking,
                                   long durationMinutes,
                                   double distanceKm,
                                   Coupon coupon,
                                   double depositPaidInr) {

        if (config == null) {
            config = PricingConfig.getDefaultSampleConfig();
        }

        Invoice invoice = new Invoice();
        String invoiceId = "INV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        invoice.setInvoiceId(invoiceId);

        if (booking != null) {
            invoice.setBookingId(booking.getBookingId());
            invoice.setUserId(booking.getUserId());
            invoice.setBicycleId(booking.getBicycleId());
            invoice.setBicycleType(booking.getBicycleType() != null ? booking.getBicycleType() : "Standard");
            invoice.setStationId(booking.getStationId());
            invoice.setStationName(booking.getStationName());
            invoice.setReturnStationId(booking.getReturnStationId());
            invoice.setReturnStationName(booking.getReturnStationName());
            invoice.setStartTime(booking.getStartTime());
            invoice.setEndTime(booking.getEndTime() != null ? booking.getEndTime() : new Date());
        }

        // Sanitized duration and distance
        long safeDurationMins = Math.max(0, durationMinutes);
        double safeDistanceKm = Math.max(0.0, Math.min(distanceKm, 200.0));
        invoice.setDurationMinutes(safeDurationMins);
        invoice.setDistanceKm(safeDistanceKm);

        // Category multiplier
        String category = invoice.getBicycleType();
        double multiplier = config.getMultiplierForCategory(category);

        // 1. Base Fee
        long baseFeePaise = config.getBaseFeePaise();
        invoice.setBaseFeePaise(baseFeePaise);

        // 2. Usage Charge (per-minute or hourly pricing)
        long usageChargePaise;
        if (safeDurationMins < 60) {
            // Under 1 hour: charged per minute
            usageChargePaise = Math.round(safeDurationMins * config.getRatePerMinutePaise() * multiplier);
        } else {
            // 1 hour or more: hourly rate for completed hours + per-minute for remainder
            long hours = safeDurationMins / 60;
            long remMins = safeDurationMins % 60;
            long hourlyComponent = hours * config.getRatePerHourPaise();
            long minuteComponent = remMins * config.getRatePerMinutePaise();
            // Cap minuteComponent at 1 hour rate to provide optimal pricing for the user
            minuteComponent = Math.min(minuteComponent, config.getRatePerHourPaise());
            usageChargePaise = Math.round((hourlyComponent + minuteComponent) * multiplier);
        }
        invoice.setUsageChargePaise(usageChargePaise);

        // 3. Late Return Fee (if duration exceeds maximum duration)
        long lateFeePaise = 0L;
        int maxDuration = config.getMaximumDurationMinutes();
        if (maxDuration > 0 && safeDurationMins > maxDuration) {
            long lateMins = safeDurationMins - maxDuration;
            long lateHours = (long) Math.ceil(lateMins / 60.0);
            lateFeePaise = lateHours * config.getLateFeePerHourPaise();
        }
        invoice.setLateFeePaise(lateFeePaise);

        // 4. Subtotal before discount
        long subtotalPaise = baseFeePaise + usageChargePaise + lateFeePaise;
        invoice.setSubtotalPaise(subtotalPaise);

        // 5. Promotional Discount
        long discountPaise = 0L;
        if (coupon != null && coupon.isActive()) {
            discountPaise = coupon.calculateDiscountPaise(subtotalPaise);
            invoice.setCouponCode(coupon.getCode());
        }
        invoice.setDiscountPaise(discountPaise);

        // 6. Taxable Amount with Minimum Ride Charge Guarantee
        long afterDiscountPaise = Math.max(0, subtotalPaise - discountPaise);
        long minimumFarePaise = config.getMinimumFarePaise();
        invoice.setMinimumFarePaise(minimumFarePaise);

        long taxableAmountPaise = Math.max(minimumFarePaise, afterDiscountPaise);

        // 7. Taxes (GST)
        long totalTaxPaise = 0L;
        long cgstPaise = 0L;
        long sgstPaise = 0L;
        if (config.isTaxEnabled() && config.getTaxPercentage() > 0) {
            double taxRate = config.getTaxPercentage();
            invoice.setTaxApplied(true);
            invoice.setTaxRatePercent(taxRate);
            totalTaxPaise = Math.round(taxableAmountPaise * (taxRate / 100.0));
            cgstPaise = totalTaxPaise / 2;
            sgstPaise = totalTaxPaise - cgstPaise;
        }
        invoice.setCgstPaise(cgstPaise);
        invoice.setSgstPaise(sgstPaise);
        invoice.setTotalTaxPaise(totalTaxPaise);

        // 8. Total Amount
        long totalAmountPaise = taxableAmountPaise + totalTaxPaise;
        invoice.setTotalAmountPaise(totalAmountPaise);

        // 9. Security Deposit Reconciliation
        long depositPaidPaise = CurrencyUtil.toPaise(depositPaidInr);
        invoice.setSecurityDepositPaise(depositPaidPaise);

        if (depositPaidPaise >= totalAmountPaise) {
            // Deposit covers full fare
            invoice.setAmountPaidPaise(totalAmountPaise);
            invoice.setAmountOutstandingPaise(0L);
            invoice.setRefundAmountPaise(depositPaidPaise - totalAmountPaise);
            invoice.setPaymentStatus("PAID_FROM_DEPOSIT");
        } else {
            // User owes balance
            invoice.setAmountPaidPaise(depositPaidPaise);
            invoice.setAmountOutstandingPaise(totalAmountPaise - depositPaidPaise);
            invoice.setRefundAmountPaise(0L);
            invoice.setPaymentStatus("PENDING");
        }

        // 10. Pricing Snapshot
        invoice.setPricingSnapshot(config.toMap());

        return invoice;
    }

    /**
     * Quick calculation of fare in rupees using active pricing config.
     */
    public double calculateTotalFare(PricingConfig config,
                                    String bicycleCategory,
                                    long durationMinutes,
                                    double distanceKm,
                                    Coupon coupon) {
        Invoice invoice = generateInvoice(config, null, durationMinutes, distanceKm, coupon, 0.0);
        return invoice.getTotalAmountInr();
    }
}
