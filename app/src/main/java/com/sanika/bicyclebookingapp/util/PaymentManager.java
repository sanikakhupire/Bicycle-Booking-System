package com.sanika.bicyclebookingapp.util;

import android.app.Activity;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

/**
 * Handles Razorpay payment flow.
 *
 * Setup:
 * 1. Create account at https://razorpay.com
 * 2. Get your Key ID from Dashboard → Settings → API Keys
 * 3. Replace YOUR_RAZORPAY_KEY_ID below with your actual key
 *
 * For testing use Razorpay test mode — no real money is charged.
 * Test card: 4111 1111 1111 1111, any future expiry, any CVV
 */
public class PaymentManager {

    // Replace with your Razorpay Key ID from dashboard
    private static final String RAZORPAY_KEY_ID = "rzp_test_SjX95bpNchghj1";

    public interface PaymentCallback {
        void onSuccess(String paymentId);
        void onFailure(String reason);
    }

    /**
     * Opens Razorpay checkout for the given fare amount.
     *
     * @param activity   The calling activity (must implement PaymentResultListener)
     * @param bookingId  Used as order description
     * @param fareRupees Fare amount in rupees (e.g. 14.0)
     * @param userName   User's name for prefill
     * @param userEmail  User's email for prefill
     * @param userPhone  User's phone for prefill
     */
    public static void startPayment(Activity activity, String bookingId,
                                     double fareRupees, String userName,
                                     String userEmail, String userPhone) {
        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_KEY_ID);
        checkout.setImage(com.sanika.bicyclebookingapp.R.mipmap.ic_launcher);

        try {
            JSONObject options = new JSONObject();
            options.put("name", "CycleGo");
            options.put("description", "Ride fare for booking " + bookingId.substring(0, 8));
            options.put("currency", "INR");
            // Razorpay expects amount in paise (1 rupee = 100 paise)
            options.put("amount", (int)(fareRupees * 100));

            // Prefill user details
            JSONObject prefill = new JSONObject();
            prefill.put("name", userName != null ? userName : "");
            prefill.put("email", userEmail != null ? userEmail : "");
            prefill.put("contact", userPhone != null ? userPhone : "");
            options.put("prefill", prefill);

            // Theme
            JSONObject theme = new JSONObject();
            theme.put("color", "#1B5E20");
            options.put("theme", theme);

            checkout.open(activity, options);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
