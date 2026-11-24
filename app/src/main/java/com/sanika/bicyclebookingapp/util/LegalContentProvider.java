package com.sanika.bicyclebookingapp.util;

/**
 * Provides authoritative legal text, terms of rental, and privacy policy for CycleGo India.
 * Formatted in full accordance with:
 * - Information Technology Act, 2000 & SPDI Rules 2011
 * - Digital Personal Data Protection (DPDP) Act, 2023 (India)
 * - Consumer Protection (E-Commerce) Rules, 2020
 */
public final class LegalContentProvider {

    private LegalContentProvider() {}

    public static String getPrivacyPolicy() {
        return "CYCLEGO PRIVACY POLICY\n" +
                "Effective Date: January 1, 2026\n" +
                "Applicable Jurisdiction: Republic of India\n\n" +
                "1. INTRODUCTION\n" +
                "CycleGo (\"we\", \"our\", or \"us\") respects your privacy and is committed to protecting your personal data in accordance with the Digital Personal Data Protection Act, 2023 (DPDP Act) and the Information Technology (Reasonable Security Practices and Procedures and Sensitive Personal Data or Information) Rules, 2011.\n\n" +
                "2. DATA WE COLLECT\n" +
                "• Identity & Contact: Full name, phone number, and verified email address.\n" +
                "• Telemetry & Location: Precise GPS location collected ONLY while a ride session is active, used solely to compute distance, enforce station geo-fencing, and navigate safe routes. We do NOT track location when a ride is inactive.\n" +
                "• Payment Data: Transaction identifiers, gateway order references, and payment method summaries. CycleGo NEVER collects or stores credit/debit card numbers, CVVs, or UPI PINs. All financial transactions are processed through RBI-authorized payment aggregators (e.g., Razorpay).\n" +
                "• Device & Diagnostics: Crash logs, device model, and OS version to improve app stability.\n\n" +
                "3. PURPOSE OF DATA PROCESSING\n" +
                "• Validating bicycle bookings, unlocks, and safe dock returns.\n" +
                "• Calculating itemized rental fares, applicable GST, and issuing INR tax invoices.\n" +
                "• Providing customer support, resolving grievances, and processing refunds.\n" +
                "• Preventing bicycle theft, unauthorized vehicle abandonment, and fraudulent bookings.\n\n" +
                "4. DATA RETENTION & SECURITY\n" +
                "Data is encrypted in transit using TLS 1.3 and at rest in certified cloud infrastructure. Account data is retained only for the duration required by applicable statutory taxation and auditing guidelines.\n\n" +
                "5. YOUR RIGHTS (DPDP ACT 2023)\n" +
                "You have the right to access your personal data, request correction of inaccurate data, and request complete erasure/deletion of your account and associated personal identifiers.\n\n" +
                "6. GRIEVANCE OFFICER\n" +
                "Grievance Officer: Support Redressal Unit, CycleGo India\n" +
                "Email: grievance@cyclego.in\n" +
                "Response SLA: Within 48 business hours as mandated by Indian IT guidelines.";
    }

    public static String getTermsAndConditions() {
        return "CYCLEGO TERMS & CONDITIONS OF SERVICE\n" +
                "Effective Date: January 1, 2026\n\n" +
                "1. ELIGIBILITY & REGISTRATION\n" +
                "Riders must be at least 16 years of age and hold a registered, OTP-verified mobile account. You agree to provide accurate information and safeguard your credentials.\n\n" +
                "2. BICYCLE RENTAL & QR UNLOCK\n" +
                "• Bookings are held for up to 15 minutes before expiring.\n" +
                "• Unlocking requires scanning the genuine QR code docked at an authorized CycleGo station.\n" +
                "• Before riding, inspect the bicycle (brakes, tires, pedals). If defective, report immediately via the app to cancel without charge.\n\n" +
                "3. PRICING, INR BILLING & TAXES\n" +
                "• All fares are billed in Indian Rupees (INR / ₹) with integer paise precision.\n" +
                "• Charges include: base booking fee, per-minute usage rate, applicable late fees, and statutory GST (CGST + SGST) where levied.\n" +
                "• Security deposits are held during rides and refunded upon satisfactory dock return.\n\n" +
                "4. DOCKING & RETURN PROTOCOL\n" +
                "Rides must be terminated by docking the bicycle at an authorized station hub and locking the mechanism. Abandoning a bicycle outside geo-fenced hubs incurs recovery fees and potential account suspension.\n\n" +
                "5. DAMAGE, LOSS & USER LIABILITY\n" +
                "Riders are responsible for safe operation adhering to the Motor Vehicles Act and local traffic regulations. Reckless damage, vandalism, or non-return will result in charges equivalent to repair cost or bicycle replacement value.\n\n" +
                "6. JURISDICTION\n" +
                "These Terms are governed by the laws of India. Any disputes are subject to the exclusive jurisdiction of the competent courts.";
    }

    public static String getDataDeletionNotice() {
        return "RIGHT TO ERASURE & ACCOUNT DELETION (DPDP ACT 2023)\n\n" +
                "Under India's Digital Personal Data Protection Act, 2023, you have the statutory right to request the deletion of your CycleGo account and the erasure of your personal data.\n\n" +
                "What happens upon deletion:\n" +
                "• Your profile, phone number, email, and loyalty points are permanently erased.\n" +
                "• You cannot recover your account once processed.\n" +
                "• Active bookings must be ended and all outstanding ride balances settled before deletion.\n" +
                "• Statutory taxation invoices are retained in anonymized format solely for legal audit compliance.\n\n" +
                "You can submit an erasure request below. Our data protection team processes requests within 7 business days.";
    }
}
