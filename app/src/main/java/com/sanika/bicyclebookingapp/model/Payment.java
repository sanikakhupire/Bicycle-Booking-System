package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.util.Date;

/**
 * Real-world Indian Payment Transaction Model.
 *
 * Supports:
 * - Razorpay Standard Checkout & UPI Intent
 * - Payment methods: UPI, UPI QR, Cards (RuPay/Visa/Mastercard), Netbanking, Wallets, Cash
 * - Monetary values in integer PAISE with rupee conversion helpers
 * - Full audit lifecycle: PENDING -> SUCCESS / FAILED / REFUNDED
 * - Gateway Order ID, Payment ID, and Signature tracking
 * - Strict PCI-DSS compliance: NEVER stores card numbers, CVVs, or UPI PINs.
 */
public class Payment {

    // Status Constants
    public static final String STATUS_PENDING        = "PENDING";
    public static final String STATUS_SUCCESS        = "SUCCESS";
    public static final String STATUS_FAILED         = "FAILED";
    public static final String STATUS_REFUND_PENDING = "REFUND_PENDING";
    public static final String STATUS_REFUNDED       = "REFUNDED";

    // Payment Method Constants
    public static final String METHOD_UPI            = "UPI";
    public static final String METHOD_UPI_INTENT     = "UPI_INTENT";
    public static final String METHOD_UPI_QR         = "UPI_QR";
    public static final String METHOD_DEBIT_CARD     = "DEBIT_CARD";
    public static final String METHOD_CREDIT_CARD    = "CREDIT_CARD";
    public static final String METHOD_NETBANKING     = "NETBANKING";
    public static final String METHOD_WALLET         = "WALLET";
    public static final String METHOD_CASH           = "CASH";
    public static final String METHOD_DEPOSIT        = "DEPOSIT";
    public static final String METHOD_SIMULATED      = "SIMULATED";

    @DocumentId
    private String paymentId;
    private String bookingId;
    private String invoiceId;
    private String userId;

    // Gateway identifiers
    private String orderId;            // Razorpay order_id (e.g. order_K...)
    private String gatewayPaymentId;   // Razorpay payment_id (e.g. pay_K...)
    private String transactionId;      // UPI / Bank transaction reference ID
    private String signature;          // HMAC SHA-256 verification signature

    // Monetary amounts in PAISE
    private long amountPaise = 0L;
    private double amount = 0.0;       // Rupee equivalent helper
    private String currency = CurrencyUtil.CURRENCY_CODE; // "INR"

    private String status = STATUS_PENDING;
    private String method = METHOD_UPI;
    private String description = "CycleGo ride payment";
    private String failureReason;

    // User contact for gateway prefill
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Refund tracking
    private String refundId;
    private long refundAmountPaise = 0L;

    // Environment flag
    private boolean testMode = true;

    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date paidAt;
    private Date verifiedAt;

    public Payment() {}

    public Payment(String bookingId, String userId, double amount) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.amountPaise = CurrencyUtil.toPaise(amount);
        this.status = STATUS_PENDING;
        this.method = METHOD_UPI;
        this.currency = CurrencyUtil.CURRENCY_CODE;
    }

    public Payment(String bookingId, String invoiceId, String userId, long amountPaise, String method) {
        this.bookingId = bookingId;
        this.invoiceId = invoiceId;
        this.userId = userId;
        this.amountPaise = amountPaise;
        this.amount = CurrencyUtil.toRupees(amountPaise);
        this.status = STATUS_PENDING;
        this.method = method != null ? method : METHOD_UPI;
        this.currency = CurrencyUtil.CURRENCY_CODE;
    }

    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
        this.amount = CurrencyUtil.toRupees(amountPaise);
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        this.amount = amount;
        this.amountPaise = CurrencyUtil.toPaise(amount);
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }

    public long getRefundAmountPaise() { return refundAmountPaise; }
    public void setRefundAmountPaise(long refundAmountPaise) { this.refundAmountPaise = refundAmountPaise; }
    public double getRefundAmountInr() { return CurrencyUtil.toRupees(refundAmountPaise); }

    public boolean isTestMode() { return testMode; }
    public void setTestMode(boolean testMode) { this.testMode = testMode; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getPaidAt() { return paidAt; }
    public void setPaidAt(Date paidAt) { this.paidAt = paidAt; }

    public Date getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Date verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getFormattedAmount() {
        return CurrencyUtil.formatInrFromPaise(amountPaise > 0 ? amountPaise : CurrencyUtil.toPaise(amount));
    }

    public boolean isSuccessful() {
        return STATUS_SUCCESS.equalsIgnoreCase(status);
    }
}
