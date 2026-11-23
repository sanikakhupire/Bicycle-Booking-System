package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Payment Gateway Configuration for India.
 *
 * Configurable via Firestore at collection 'payment_config/default'.
 * Security Rule: Key secret MUST NEVER be placed in client code or Firestore documents accessible by customers.
 */
public class PaymentConfig {

    public static final String CONFIG_ID_DEFAULT = "default";
    public static final String GATEWAY_RAZORPAY   = "RAZORPAY";

    @DocumentId
    private String configId = CONFIG_ID_DEFAULT;

    private String gatewayProvider = GATEWAY_RAZORPAY;
    private boolean testMode = true; // True by default for safe sandbox operations

    // Public Razorpay Key ID (Client-safe; Secret Key remains strictly on backend/Cloud Functions)
    private String razorpayKeyId = "rzp_test_placeholder";

    private String merchantName = "CycleGo India";
    private String merchantUpiVpa = "cyclego@upi";
    private String supportEmail = "support@cyclego.in";
    private String supportPhone = "+91 98765 43210";

    // Business policies
    private boolean autoRefundDeposit = true;
    private boolean cashFallbackAllowed = true;

    @ServerTimestamp
    private Date updatedAt;
    private String updatedBy;

    public PaymentConfig() {}

    public static PaymentConfig createDefaultConfig() {
        PaymentConfig config = new PaymentConfig();
        config.setConfigId(CONFIG_ID_DEFAULT);
        config.setGatewayProvider(GATEWAY_RAZORPAY);
        config.setTestMode(true);
        config.setRazorpayKeyId("rzp_test_placeholder");
        config.setMerchantName("CycleGo India");
        config.setMerchantUpiVpa("cyclego@upi");
        config.setAutoRefundDeposit(true);
        config.setCashFallbackAllowed(true);
        return config;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("configId", configId);
        map.put("gatewayProvider", gatewayProvider);
        map.put("testMode", testMode);
        map.put("razorpayKeyId", razorpayKeyId);
        map.put("merchantName", merchantName);
        map.put("merchantUpiVpa", merchantUpiVpa);
        map.put("supportEmail", supportEmail);
        map.put("supportPhone", supportPhone);
        map.put("autoRefundDeposit", autoRefundDeposit);
        map.put("cashFallbackAllowed", cashFallbackAllowed);
        map.put("updatedAt", updatedAt);
        map.put("updatedBy", updatedBy);
        return map;
    }

    public static PaymentConfig fromMap(Map<String, Object> map) {
        if (map == null) return createDefaultConfig();
        PaymentConfig config = new PaymentConfig();
        if (map.containsKey("configId")) config.setConfigId((String) map.get("configId"));
        if (map.containsKey("gatewayProvider")) config.setGatewayProvider((String) map.get("gatewayProvider"));
        if (map.containsKey("testMode")) config.setTestMode((Boolean) map.get("testMode"));
        if (map.containsKey("razorpayKeyId")) config.setRazorpayKeyId((String) map.get("razorpayKeyId"));
        if (map.containsKey("merchantName")) config.setMerchantName((String) map.get("merchantName"));
        if (map.containsKey("merchantUpiVpa")) config.setMerchantUpiVpa((String) map.get("merchantUpiVpa"));
        if (map.containsKey("supportEmail")) config.setSupportEmail((String) map.get("supportEmail"));
        if (map.containsKey("supportPhone")) config.setSupportPhone((String) map.get("supportPhone"));
        if (map.containsKey("autoRefundDeposit")) config.setAutoRefundDeposit((Boolean) map.get("autoRefundDeposit"));
        if (map.containsKey("cashFallbackAllowed")) config.setCashFallbackAllowed((Boolean) map.get("cashFallbackAllowed"));
        return config;
    }

    // Getters and Setters
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }

    public String getGatewayProvider() { return gatewayProvider; }
    public void setGatewayProvider(String gatewayProvider) { this.gatewayProvider = gatewayProvider; }

    public boolean isTestMode() { return testMode; }
    public void setTestMode(boolean testMode) { this.testMode = testMode; }

    public String getRazorpayKeyId() { return razorpayKeyId; }
    public void setRazorpayKeyId(String razorpayKeyId) { this.razorpayKeyId = razorpayKeyId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getMerchantUpiVpa() { return merchantUpiVpa; }
    public void setMerchantUpiVpa(String merchantUpiVpa) { this.merchantUpiVpa = merchantUpiVpa; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getSupportPhone() { return supportPhone; }
    public void setSupportPhone(String supportPhone) { this.supportPhone = supportPhone; }

    public boolean isAutoRefundDeposit() { return autoRefundDeposit; }
    public void setAutoRefundDeposit(boolean autoRefundDeposit) { this.autoRefundDeposit = autoRefundDeposit; }

    public boolean isCashFallbackAllowed() { return cashFallbackAllowed; }
    public void setCashFallbackAllowed(boolean cashFallbackAllowed) { this.cashFallbackAllowed = cashFallbackAllowed; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
