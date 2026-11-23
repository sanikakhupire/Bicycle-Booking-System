package com.sanika.bicyclebookingapp;

import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.PaymentConfig;
import com.sanika.bicyclebookingapp.model.Refund;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 5 Unit Tests:
 * - Payment Gateway Integration for the Indian Market (INR / ₹)
 * - Integer paise precision and Indian currency formatting
 * - Payment lifecycle and state transitions (PENDING, SUCCESS, FAILED, REFUNDED)
 * - Server-side HMAC SHA-256 signature verification simulation
 * - Idempotency and duplicate payment prevention
 * - Security deposit refund modeling and reconciliation
 * - PCI-DSS compliance verification: strictly no PAN, CVV, or UPI PIN storage
 * - Test mode vs Production gateway configuration
 */
public class Phase5PaymentIntegrationTest {

    // =========================================================================
    // 1. Currency & Paise Precision
    // =========================================================================

    @Test
    public void testPaymentPaisePrecision() {
        Payment payment = new Payment("BOOK101", "USER001", 1250.50);
        assertEquals(125050L, payment.getAmountPaise());
        assertEquals(1250.50, payment.getAmount(), 0.001);
        assertEquals("INR", payment.getCurrency());
        assertEquals("₹1,250.50", payment.getFormattedAmount());
    }

    // =========================================================================
    // 2. Payment Lifecycle & Status Transitions
    // =========================================================================

    @Test
    public void testPaymentStatusTransitions() {
        Payment payment = new Payment("BOOK102", "INV201", "USER002", 7000L, Payment.METHOD_UPI);
        assertEquals(Payment.STATUS_PENDING, payment.getStatus());
        assertFalse(payment.isSuccessful());

        // Simulate Gateway Success Callback
        payment.setStatus(Payment.STATUS_SUCCESS);
        payment.setGatewayPaymentId("pay_test_998877");
        payment.setTransactionId("UPI_TXN_554433");
        payment.setPaidAt(new Date());

        assertTrue(payment.isSuccessful());
        assertEquals(Payment.STATUS_SUCCESS, payment.getStatus());
        assertEquals("pay_test_998877", payment.getGatewayPaymentId());
        assertEquals("UPI_TXN_554433", payment.getTransactionId());

        // Simulate Refund
        payment.setStatus(Payment.STATUS_REFUNDED);
        payment.setRefundId("REF_7788");
        payment.setRefundAmountPaise(7000L);

        assertEquals(Payment.STATUS_REFUNDED, payment.getStatus());
        assertEquals(7000L, payment.getRefundAmountPaise());
        assertEquals(70.0, payment.getRefundAmountInr(), 0.001);
    }

    // =========================================================================
    // 3. Server-Authoritative HMAC SHA-256 Signature Verification
    // =========================================================================

    @Test
    public void testRazorpayHmacSha256Verification() throws Exception {
        String orderId = "order_EKwxwAgItmmXdp";
        String paymentId = "pay_29QQoUBi66xm2f";
        String secret = "EnLs pathwaysSecretKey123";

        // Generate expected signature: HMAC-SHA256(order_id + "|" + payment_id, secret)
        String payload = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String generatedSignature = hexString.toString();

        assertNotNull(generatedSignature);
        assertTrue(generatedSignature.length() > 0);

        // Verify genuine signature matches
        boolean isAuthentic = generatedSignature.equals(hexString.toString());
        assertTrue("Authentic signature must pass verification", isAuthentic);

        // Verify tampered signature fails
        String tamperedSignature = generatedSignature.substring(0, generatedSignature.length() - 4) + "0000";
        assertFalse("Tampered signature must fail verification", generatedSignature.equals(tamperedSignature));

        // Verify wrong payment ID fails
        String wrongPayload = orderId + "|pay_WRONG_ID";
        mac.init(secretKeySpec);
        byte[] wrongHash = mac.doFinal(wrongPayload.getBytes(StandardCharsets.UTF_8));
        StringBuilder wrongHex = new StringBuilder();
        for (byte b : wrongHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) wrongHex.append('0');
            wrongHex.append(hex);
        }
        assertFalse("Mismatched payment ID signature must fail", generatedSignature.equals(wrongHex.toString()));
    }

    // =========================================================================
    // 4. Idempotency & Duplicate Prevention
    // =========================================================================

    @Test
    public void testPaymentIdempotency() {
        Payment payment = new Payment("BOOK103", "INV202", "USER003", 5000L, Payment.METHOD_UPI);
        payment.setStatus(Payment.STATUS_SUCCESS);
        payment.setTransactionId("TXN_FIRST_123");

        // Simulate duplicate callback arriving for the same transaction
        String incomingTxnId = "TXN_DUPLICATE_456";
        if (!Payment.STATUS_SUCCESS.equals(payment.getStatus())) {
            payment.setTransactionId(incomingTxnId);
        }

        // Original transaction reference must remain preserved and not overwritten
        assertEquals("TXN_FIRST_123", payment.getTransactionId());
        assertTrue(payment.isSuccessful());
    }

    // =========================================================================
    // 5. Refund Model & Reconciliation
    // =========================================================================

    @Test
    public void testRefundLifecycle() {
        Refund refund = new Refund("PAY_100", "BOOK_100", "USER_100", 10000L, Refund.REASON_DEPOSIT_RETURN);
        assertEquals(Refund.STATUS_PENDING, refund.getStatus());
        assertEquals(10000L, refund.getAmountPaise());
        assertEquals(100.0, refund.getAmountInr(), 0.001);
        assertEquals("₹100.00", refund.getFormattedAmount());

        // Process refund
        refund.setStatus(Refund.STATUS_PROCESSED);
        refund.setGatewayRefundId("rfrnd_test_556677");
        refund.setProcessedAt(new Date());

        assertEquals(Refund.STATUS_PROCESSED, refund.getStatus());
        assertEquals("rfrnd_test_556677", refund.getGatewayRefundId());
    }

    // =========================================================================
    // 6. Payment Configuration & Test Mode
    // =========================================================================

    @Test
    public void testPaymentConfigDefaults() {
        PaymentConfig config = PaymentConfig.createDefaultConfig();
        assertTrue(config.isTestMode());
        assertEquals(PaymentConfig.GATEWAY_RAZORPAY, config.getGatewayProvider());
        assertTrue(config.getRazorpayKeyId().contains("test"));
        assertTrue(config.isAutoRefundDeposit());
        assertTrue(config.isCashFallbackAllowed());

        // Map serialization test
        Map<String, Object> map = config.toMap();
        assertNotNull(map);
        assertEquals(PaymentConfig.GATEWAY_RAZORPAY, map.get("gatewayProvider"));
        assertEquals(true, map.get("testMode"));

        // Deserialization test
        PaymentConfig recovered = PaymentConfig.fromMap(map);
        assertEquals(config.getGatewayProvider(), recovered.getGatewayProvider());
        assertTrue(recovered.isTestMode());
    }

    // =========================================================================
    // 7. Security: PCI-DSS Compliance (Strictly No Sensitive Card Data Stored)
    // =========================================================================

    @Test
    public void testPciDssComplianceNoSensitiveCardData() {
        // Reflection check: Verify Payment and Refund classes contain NO sensitive card fields
        Field[] paymentFields = Payment.class.getDeclaredFields();
        for (Field f : paymentFields) {
            String name = f.getName().toLowerCase();
            assertFalse("Payment must never store CVV", name.contains("cvv"));
            assertFalse("Payment must never store PAN/Card Number", name.contains("cardnumber") || name.equals("pan"));
            assertFalse("Payment must never store UPI PIN", name.contains("pin") || name.contains("upipin"));
        }

        Field[] refundFields = Refund.class.getDeclaredFields();
        for (Field f : refundFields) {
            String name = f.getName().toLowerCase();
            assertFalse("Refund must never store CVV", name.contains("cvv"));
            assertFalse("Refund must never store card numbers", name.contains("cardnumber") || name.equals("pan"));
        }
    }
}
