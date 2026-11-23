package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Complete, real-world Indian rental invoice model.
 * All monetary amounts are preserved in integer PAISE with rupee conversion helpers.
 */
public class Invoice {

    @DocumentId
    private String invoiceId;
    private String bookingId;
    private String userId;
    private String bicycleId;
    private String bicycleType = "Standard";
    private String stationName;
    private String returnStationName;

    private Date startTime;
    private Date endTime;
    private long durationMinutes = 0;
    private double distanceKm = 0.0;

    // Itemized amounts in PAISE
    private long baseFeePaise = 0;
    private long usageChargePaise = 0;
    private long extraChargesPaise = 0;
    private long lateFeePaise = 0;
    private long subtotalPaise = 0;

    // Promotional discount
    private String couponCode;
    private long discountPaise = 0;

    // Taxes
    private boolean taxApplied = false;
    private double taxRatePercent = 0.0;
    private long cgstPaise = 0;
    private long sgstPaise = 0;
    private long totalTaxPaise = 0;

    // Total and settlements
    private long minimumFarePaise = 0;
    private long totalAmountPaise = 0;
    private long securityDepositPaise = 0;
    private long amountPaidPaise = 0;
    private long amountOutstandingPaise = 0;
    private long refundAmountPaise = 0;

    private String paymentStatus = "PENDING"; // PENDING, PAID, REFUNDED, SETTLED
    private String paymentTxnId;

    // Snapshot of pricing rules applied at billing time
    private Map<String, Object> pricingSnapshot = new HashMap<>();

    @ServerTimestamp
    private Date createdAt;

    public Invoice() {}

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getBicycleType() { return bicycleType; }
    public void setBicycleType(String bicycleType) { this.bicycleType = bicycleType; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getReturnStationName() { return returnStationName; }
    public void setReturnStationName(String returnStationName) { this.returnStationName = returnStationName; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public long getBaseFeePaise() { return baseFeePaise; }
    public void setBaseFeePaise(long baseFeePaise) { this.baseFeePaise = baseFeePaise; }

    public long getUsageChargePaise() { return usageChargePaise; }
    public void setUsageChargePaise(long usageChargePaise) { this.usageChargePaise = usageChargePaise; }

    public long getExtraChargesPaise() { return extraChargesPaise; }
    public void setExtraChargesPaise(long extraChargesPaise) { this.extraChargesPaise = extraChargesPaise; }

    public long getLateFeePaise() { return lateFeePaise; }
    public void setLateFeePaise(long lateFeePaise) { this.lateFeePaise = lateFeePaise; }

    public long getSubtotalPaise() { return subtotalPaise; }
    public void setSubtotalPaise(long subtotalPaise) { this.subtotalPaise = subtotalPaise; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public long getDiscountPaise() { return discountPaise; }
    public void setDiscountPaise(long discountPaise) { this.discountPaise = discountPaise; }

    public boolean isTaxApplied() { return taxApplied; }
    public void setTaxApplied(boolean taxApplied) { this.taxApplied = taxApplied; }

    public double getTaxRatePercent() { return taxRatePercent; }
    public void setTaxRatePercent(double taxRatePercent) { this.taxRatePercent = taxRatePercent; }

    public long getCgstPaise() { return cgstPaise; }
    public void setCgstPaise(long cgstPaise) { this.cgstPaise = cgstPaise; }

    public long getSgstPaise() { return sgstPaise; }
    public void setSgstPaise(long sgstPaise) { this.sgstPaise = sgstPaise; }

    public long getTotalTaxPaise() { return totalTaxPaise; }
    public void setTotalTaxPaise(long totalTaxPaise) { this.totalTaxPaise = totalTaxPaise; }

    public long getMinimumFarePaise() { return minimumFarePaise; }
    public void setMinimumFarePaise(long minimumFarePaise) { this.minimumFarePaise = minimumFarePaise; }

    public long getTotalAmountPaise() { return totalAmountPaise; }
    public void setTotalAmountPaise(long totalAmountPaise) { this.totalAmountPaise = totalAmountPaise; }
    public long getTotalFarePaise() { return totalAmountPaise; }
    public void setTotalFarePaise(long totalAmountPaise) { this.totalAmountPaise = totalAmountPaise; }

    public long getSecurityDepositPaise() { return securityDepositPaise; }
    public void setSecurityDepositPaise(long securityDepositPaise) { this.securityDepositPaise = securityDepositPaise; }
    public long getDepositPaidPaise() { return securityDepositPaise; }
    public void setDepositPaidPaise(long depositPaidPaise) { this.securityDepositPaise = depositPaidPaise; }

    public long getAmountPaidPaise() { return amountPaidPaise; }
    public void setAmountPaidPaise(long amountPaidPaise) { this.amountPaidPaise = amountPaidPaise; }

    public long getAmountOutstandingPaise() { return amountOutstandingPaise; }
    public void setAmountOutstandingPaise(long amountOutstandingPaise) { this.amountOutstandingPaise = amountOutstandingPaise; }

    public long getRefundAmountPaise() { return refundAmountPaise; }
    public void setRefundAmountPaise(long refundAmountPaise) { this.refundAmountPaise = refundAmountPaise; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentTxnId() { return paymentTxnId; }
    public void setPaymentTxnId(String paymentTxnId) { this.paymentTxnId = paymentTxnId; }

    public Map<String, Object> getPricingSnapshot() { return pricingSnapshot; }
    public void setPricingSnapshot(Map<String, Object> pricingSnapshot) {
        this.pricingSnapshot = pricingSnapshot != null ? pricingSnapshot : new HashMap<>();
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    // Rupee getters for UI
    public double getBaseFeeInr() { return CurrencyUtil.toRupees(baseFeePaise); }
    public double getUsageChargeInr() { return CurrencyUtil.toRupees(usageChargePaise); }
    public double getLateFeeInr() { return CurrencyUtil.toRupees(lateFeePaise); }
    public double getSubtotalInr() { return CurrencyUtil.toRupees(subtotalPaise); }
    public double getDiscountInr() { return CurrencyUtil.toRupees(discountPaise); }
    public double getTotalTaxInr() { return CurrencyUtil.toRupees(totalTaxPaise); }
    public double getCgstInr() { return CurrencyUtil.toRupees(cgstPaise); }
    public double getSgstInr() { return CurrencyUtil.toRupees(sgstPaise); }
    public double getTotalAmountInr() { return CurrencyUtil.toRupees(totalAmountPaise); }
    public double getSecurityDepositInr() { return CurrencyUtil.toRupees(securityDepositPaise); }
    public double getAmountPaidInr() { return CurrencyUtil.toRupees(amountPaidPaise); }
    public double getAmountOutstandingInr() { return CurrencyUtil.toRupees(amountOutstandingPaise); }
    public double getRefundAmountInr() { return CurrencyUtil.toRupees(refundAmountPaise); }

    public String getFormattedTotal() { return CurrencyUtil.formatPaise(totalAmountPaise); }
}
