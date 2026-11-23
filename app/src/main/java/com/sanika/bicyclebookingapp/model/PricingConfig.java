package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Real-world Configurable Pricing Model for India Bicycle Rental.
 *
 * Monetary values are stored in integer PAISE (1 INR = 100 paise)
 * to ensure decimal precision and eliminate floating-point rounding errors.
 */
public class PricingConfig {

    public static final String CONFIG_ID_DEFAULT = "default";

    @DocumentId
    private String configId = CONFIG_ID_DEFAULT;

    // Rates in PAISE
    private long baseFeePaise = 1000L;             // Sample: ₹10.00
    private long ratePerMinutePaise = 200L;        // Sample: ₹2.00 / min
    private long ratePerHourPaise = 6000L;         // Sample: ₹60.00 / hour
    private long ratePerDayPaise = 30000L;         // Sample: ₹300.00 / day
    private long minimumFarePaise = 2000L;         // Sample: ₹20.00 min charge
    private int  maximumDurationMinutes = 720;     // 12 hours max continuous rental
    private long lateFeePerHourPaise = 5000L;      // Sample: ₹50.00 / hour late fee
    private long cancellationFeePaise = 1000L;     // Sample: ₹10.00 cancellation fee

    // Security Deposit Configuration
    private boolean securityDepositEnabled = false; // Disabled by default per India business requirements
    private long securityDepositPaise = 10000L;     // ₹100.00 if enabled

    // Tax / GST Configuration
    private boolean taxEnabled = false;            // Configurable tax
    private double taxPercentage = 5.0;            // 5% GST (2.5% CGST + 2.5% SGST)

    // Category Multipliers
    private double standardCategoryMultiplier = 1.0;
    private double electricCategoryMultiplier = 1.5;
    private double mountainCategoryMultiplier = 1.25;

    // Sample Configuration Flag
    private boolean sampleConfig = true;

    @ServerTimestamp
    private Date updatedAt;
    private String updatedBy;

    public PricingConfig() {}

    public static PricingConfig getDefaultSampleConfig() {
        PricingConfig config = new PricingConfig();
        config.setConfigId(CONFIG_ID_DEFAULT);
        config.setBaseFeePaise(1000L);            // ₹10.00
        config.setRatePerMinutePaise(200L);       // ₹2.00
        config.setRatePerHourPaise(6000L);        // ₹60.00
        config.setRatePerDayPaise(30000L);        // ₹300.00
        config.setMinimumFarePaise(2000L);        // ₹20.00
        config.setMaximumDurationMinutes(720);    // 12 hours
        config.setLateFeePerHourPaise(5000L);     // ₹50.00
        config.setCancellationFeePaise(1000L);    // ₹10.00
        config.setSecurityDepositEnabled(false);  // Disabled by default
        config.setSecurityDepositPaise(10000L);   // ₹100.00
        config.setTaxEnabled(false);
        config.setTaxPercentage(5.0);
        config.setStandardCategoryMultiplier(1.0);
        config.setElectricCategoryMultiplier(1.5);
        config.setMountainCategoryMultiplier(1.25);
        config.setSampleConfig(true);
        return config;
    }

    public static PricingConfig createSampleConfig() {
        return getDefaultSampleConfig();
    }

    public static PricingConfig fromMap(Map<String, Object> map) {
        if (map == null) return getDefaultSampleConfig();
        PricingConfig config = new PricingConfig();
        if (map.containsKey("configId")) config.setConfigId((String) map.get("configId"));
        if (map.containsKey("baseFeePaise")) config.setBaseFeePaise(((Number) map.get("baseFeePaise")).longValue());
        if (map.containsKey("ratePerMinutePaise")) config.setRatePerMinutePaise(((Number) map.get("ratePerMinutePaise")).longValue());
        if (map.containsKey("ratePerHourPaise")) config.setRatePerHourPaise(((Number) map.get("ratePerHourPaise")).longValue());
        if (map.containsKey("ratePerDayPaise")) config.setRatePerDayPaise(((Number) map.get("ratePerDayPaise")).longValue());
        if (map.containsKey("minimumFarePaise")) config.setMinimumFarePaise(((Number) map.get("minimumFarePaise")).longValue());
        if (map.containsKey("maximumDurationMinutes")) config.setMaximumDurationMinutes(((Number) map.get("maximumDurationMinutes")).intValue());
        if (map.containsKey("lateFeePerHourPaise")) config.setLateFeePerHourPaise(((Number) map.get("lateFeePerHourPaise")).longValue());
        if (map.containsKey("cancellationFeePaise")) config.setCancellationFeePaise(((Number) map.get("cancellationFeePaise")).longValue());
        if (map.containsKey("securityDepositEnabled")) config.setSecurityDepositEnabled((Boolean) map.get("securityDepositEnabled"));
        if (map.containsKey("securityDepositPaise")) config.setSecurityDepositPaise(((Number) map.get("securityDepositPaise")).longValue());
        if (map.containsKey("taxEnabled")) config.setTaxEnabled((Boolean) map.get("taxEnabled"));
        if (map.containsKey("taxPercentage")) config.setTaxPercentage(((Number) map.get("taxPercentage")).doubleValue());
        if (map.containsKey("standardCategoryMultiplier")) config.setStandardCategoryMultiplier(((Number) map.get("standardCategoryMultiplier")).doubleValue());
        if (map.containsKey("electricCategoryMultiplier")) config.setElectricCategoryMultiplier(((Number) map.get("electricCategoryMultiplier")).doubleValue());
        if (map.containsKey("mountainCategoryMultiplier")) config.setMountainCategoryMultiplier(((Number) map.get("mountainCategoryMultiplier")).doubleValue());
        if (map.containsKey("sampleConfig")) config.setSampleConfig((Boolean) map.get("sampleConfig"));
        return config;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("configId", configId);
        map.put("baseFeePaise", baseFeePaise);
        map.put("baseBookingFeePaise", baseFeePaise);
        map.put("ratePerMinutePaise", ratePerMinutePaise);
        map.put("perMinuteRatePaise", ratePerMinutePaise);
        map.put("ratePerHourPaise", ratePerHourPaise);
        map.put("ratePerDayPaise", ratePerDayPaise);
        map.put("minimumFarePaise", minimumFarePaise);
        map.put("maximumDurationMinutes", maximumDurationMinutes);
        map.put("lateFeePerHourPaise", lateFeePerHourPaise);
        map.put("cancellationFeePaise", cancellationFeePaise);
        map.put("securityDepositEnabled", securityDepositEnabled);
        map.put("securityDepositPaise", securityDepositPaise);
        map.put("taxEnabled", taxEnabled);
        map.put("taxPercentage", taxPercentage);
        map.put("standardCategoryMultiplier", standardCategoryMultiplier);
        map.put("electricCategoryMultiplier", electricCategoryMultiplier);
        map.put("mountainCategoryMultiplier", mountainCategoryMultiplier);
        map.put("sampleConfig", sampleConfig);
        map.put("updatedAt", updatedAt);
        map.put("updatedBy", updatedBy);
        return map;
    }

    // Getters and Setters (Paise)
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }

    public long getBaseFeePaise() { return baseFeePaise; }
    public void setBaseFeePaise(long baseFeePaise) { this.baseFeePaise = baseFeePaise; }
    public long getBaseBookingFeePaise() { return baseFeePaise; }
    public void setBaseBookingFeePaise(long baseFeePaise) { this.baseFeePaise = baseFeePaise; }

    public long getRatePerMinutePaise() { return ratePerMinutePaise; }
    public void setRatePerMinutePaise(long ratePerMinutePaise) { this.ratePerMinutePaise = ratePerMinutePaise; }
    public long getPerMinuteRatePaise() { return ratePerMinutePaise; }
    public void setPerMinuteRatePaise(long ratePerMinutePaise) { this.ratePerMinutePaise = ratePerMinutePaise; }

    public long getRatePerHourPaise() { return ratePerHourPaise; }
    public void setRatePerHourPaise(long ratePerHourPaise) { this.ratePerHourPaise = ratePerHourPaise; }
    public long getPerHourRatePaise() { return ratePerHourPaise; }
    public void setPerHourRatePaise(long ratePerHourPaise) { this.ratePerHourPaise = ratePerHourPaise; }

    public long getRatePerDayPaise() { return ratePerDayPaise; }
    public void setRatePerDayPaise(long ratePerDayPaise) { this.ratePerDayPaise = ratePerDayPaise; }

    public long getMinimumFarePaise() { return minimumFarePaise; }
    public void setMinimumFarePaise(long minimumFarePaise) { this.minimumFarePaise = minimumFarePaise; }
    public long getMinimumRideChargePaise() { return minimumFarePaise; }
    public void setMinimumRideChargePaise(long minimumFarePaise) { this.minimumFarePaise = minimumFarePaise; }

    public int getMaximumDurationMinutes() { return maximumDurationMinutes; }
    public void setMaximumDurationMinutes(int maximumDurationMinutes) { this.maximumDurationMinutes = maximumDurationMinutes; }

    public long getLateFeePerHourPaise() { return lateFeePerHourPaise; }
    public void setLateFeePerHourPaise(long lateFeePerHourPaise) { this.lateFeePerHourPaise = lateFeePerHourPaise; }

    public long getCancellationFeePaise() { return cancellationFeePaise; }
    public void setCancellationFeePaise(long cancellationFeePaise) { this.cancellationFeePaise = cancellationFeePaise; }

    public boolean isSecurityDepositEnabled() { return securityDepositEnabled; }
    public void setSecurityDepositEnabled(boolean securityDepositEnabled) { this.securityDepositEnabled = securityDepositEnabled; }

    public long getSecurityDepositPaise() { return securityDepositPaise; }
    public void setSecurityDepositPaise(long securityDepositPaise) { this.securityDepositPaise = securityDepositPaise; }

    public boolean isTaxEnabled() { return taxEnabled; }
    public void setTaxEnabled(boolean taxEnabled) { this.taxEnabled = taxEnabled; }
    public boolean isGstEnabled() { return taxEnabled; }
    public void setGstEnabled(boolean gstEnabled) { this.taxEnabled = gstEnabled; }

    public double getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(double taxPercentage) { this.taxPercentage = taxPercentage; }
    public double getGstPercentage() { return taxPercentage; }
    public void setGstPercentage(double gstPercentage) { this.taxPercentage = gstPercentage; }

    public double getStandardCategoryMultiplier() { return standardCategoryMultiplier; }
    public void setStandardCategoryMultiplier(double standardCategoryMultiplier) { this.standardCategoryMultiplier = standardCategoryMultiplier; }

    public double getElectricCategoryMultiplier() { return electricCategoryMultiplier; }
    public void setElectricCategoryMultiplier(double electricCategoryMultiplier) { this.electricCategoryMultiplier = electricCategoryMultiplier; }

    public double getMountainCategoryMultiplier() { return mountainCategoryMultiplier; }
    public void setMountainCategoryMultiplier(double mountainCategoryMultiplier) { this.mountainCategoryMultiplier = mountainCategoryMultiplier; }

    public boolean isSampleConfig() { return sampleConfig; }
    public void setSampleConfig(boolean sampleConfig) { this.sampleConfig = sampleConfig; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // Rupee helpers for UI and calculation
    public double getBaseFeeInr() { return CurrencyUtil.toRupees(baseFeePaise); }
    public void setBaseFeeInr(double inr) { this.baseFeePaise = CurrencyUtil.toPaise(inr); }

    public double getRatePerMinuteInr() { return CurrencyUtil.toRupees(ratePerMinutePaise); }
    public void setRatePerMinuteInr(double inr) { this.ratePerMinutePaise = CurrencyUtil.toPaise(inr); }

    public double getRatePerHourInr() { return CurrencyUtil.toRupees(ratePerHourPaise); }
    public void setRatePerHourInr(double inr) { this.ratePerHourPaise = CurrencyUtil.toPaise(inr); }

    public double getMinimumFareInr() { return CurrencyUtil.toRupees(minimumFarePaise); }
    public void setMinimumFareInr(double inr) { this.minimumFarePaise = CurrencyUtil.toPaise(inr); }

    public double getLateFeePerHourInr() { return CurrencyUtil.toRupees(lateFeePerHourPaise); }
    public void setLateFeePerHourInr(double inr) { this.lateFeePerHourPaise = CurrencyUtil.toPaise(inr); }

    public double getCancellationFeeInr() { return CurrencyUtil.toRupees(cancellationFeePaise); }
    public void setCancellationFeeInr(double inr) { this.cancellationFeePaise = CurrencyUtil.toPaise(inr); }

    public double getSecurityDepositInr() { return CurrencyUtil.toRupees(securityDepositPaise); }
    public void setSecurityDepositInr(double inr) { this.securityDepositPaise = CurrencyUtil.toPaise(inr); }

    public double getMultiplierForCategory(String category) {
        if (category == null) return 1.0;
        switch (category.trim().toUpperCase()) {
            case "ELECTRIC":
            case "E-BIKE":
                return electricCategoryMultiplier > 0 ? electricCategoryMultiplier : 1.5;
            case "MOUNTAIN":
            case "GEARED":
                return mountainCategoryMultiplier > 0 ? mountainCategoryMultiplier : 1.25;
            default:
                return standardCategoryMultiplier > 0 ? standardCategoryMultiplier : 1.0;
        }
    }

    public double getCategoryMultiplier(String category) {
        return getMultiplierForCategory(category);
    }
}
