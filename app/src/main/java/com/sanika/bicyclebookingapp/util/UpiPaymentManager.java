package com.sanika.bicyclebookingapp.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

/**
 * UPI Payment using Android Intent.
 *
 * Different UPI apps return different response formats:
 * - GPay:     Status=SUCCESS&txnId=xxx&responseCode=00
 * - PhonePe:  Status=SUCCESS&txnRef=xxx
 * - Paytm:    STATUS=TXN_SUCCESS&TXNID=xxx
 * - BHIM:     status=success&txnId=xxx
 *
 * This class handles all formats.
 */
public class UpiPaymentManager {

    private static final String DEFAULT_PAYEE_VPA  = "cyclego@upi";
    private static final String DEFAULT_PAYEE_NAME = "CycleGo";

    public static final int UPI_REQUEST_CODE = 1001;

    public static void startUpiPayment(Activity activity, double amount,
                                        String transactionId, String note) {
        // Minimum UPI amount is ₹1
        if (amount < 1.0) amount = 1.0;

        String payeeVpa = DEFAULT_PAYEE_VPA;
        String payeeName = DEFAULT_PAYEE_NAME;
        try {
            payeeVpa = activity.getString(com.sanika.bicyclebookingapp.R.string.upi_payee_vpa);
            payeeName = activity.getString(com.sanika.bicyclebookingapp.R.string.upi_payee_name);
        } catch (Exception ignored) {}

        String amountStr = String.format("%.2f", amount);

        Uri uri = new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", payeeVpa)
                .appendQueryParameter("pn", payeeName)
                .appendQueryParameter("tr", transactionId)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amountStr)
                .appendQueryParameter("cu", "INR")
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);

        if (apps != null && !apps.isEmpty()) {
            Intent chooser = Intent.createChooser(intent, "Pay ₹" + amountStr + " via UPI");
            activity.startActivityForResult(chooser, UPI_REQUEST_CODE);
        } else {
            android.widget.Toast.makeText(activity,
                "No UPI app found. Please install GPay, PhonePe or Paytm.",
                android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Parses UPI payment result from onActivityResult.
     * Handles all major UPI app response formats.
     *
     * @return "SUCCESS", "PENDING", or "FAILURE"
     */
    public static String getPaymentStatus(int resultCode, Intent data) {
        // If user pressed back / cancelled
        if (resultCode == Activity.RESULT_CANCELED) return "FAILURE";

        if (data == null) return "FAILURE";

        // Try "response" key first (GPay, PhonePe, BHIM)
        String response = data.getStringExtra("response");

        // Some apps use "Status" key directly
        if (response == null) {
            response = data.getStringExtra("Status");
        }

        // Some apps use "status" (lowercase)
        if (response == null) {
            response = data.getStringExtra("status");
        }

        if (response == null) return "FAILURE";

        String lower = response.toLowerCase();

        // Check for success indicators
        if (lower.contains("success")
                || lower.contains("txn_success")
                || lower.contains("approved")) {
            return "SUCCESS";
        }

        // Check for pending
        if (lower.contains("pending")
                || lower.contains("initiated")) {
            return "PENDING";
        }

        return "FAILURE";
    }

    /**
     * Extracts transaction ID from UPI response.
     * Handles multiple response formats.
     */
    public static String getTransactionId(Intent data) {
        if (data == null) return "";

        // Try direct extras first
        String txnId = data.getStringExtra("txnId");
        if (txnId != null && !txnId.isEmpty()) return txnId;

        txnId = data.getStringExtra("TXNID");
        if (txnId != null && !txnId.isEmpty()) return txnId;

        txnId = data.getStringExtra("txnRef");
        if (txnId != null && !txnId.isEmpty()) return txnId;

        // Parse from response string
        String response = data.getStringExtra("response");
        if (response == null) return "";

        for (String part : response.split("&")) {
            String lower = part.toLowerCase();
            if (lower.startsWith("txnid=") || lower.startsWith("txnref=")) {
                String[] kv = part.split("=");
                if (kv.length > 1) return kv[1];
            }
        }

        return "TXN_" + System.currentTimeMillis();
    }
}
