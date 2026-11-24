# CycleGo India — Production Deployment, Security & Play Store Guide

This document details the complete production hardening, security configurations, and deployment requirements for **CycleGo (Bicycle Booking App)**.

---

## 1. Executive Security & Architecture Summary

| Area | Current Implementation Status | Production Action Required |
| :--- | :--- | :--- |
| **Authentication** | Firebase Auth (Email/Password, Role-Based Access) | Enable Phone OTP auth in Firebase Console for Indian market. |
| **Authorization** | Strict Firestore rules prevent privilege escalation to `ADMIN` or `HANDLER`. | Enforce in production via `firebase deploy --only firestore:rules`. |
| **App Attestation** | Firebase App Check with `DebugAppCheckProviderFactory` (Debug) & `PlayIntegrity` (Release). | Register SHA-256 fingerprint in Firebase Console under App Check. |
| **Payment Gateway** | Razorpay SDK (UPI, RuPay/Visa/Mastercard, Netbanking), integer paise accounting. | Replace Test Key with Live Key; set Secret Key in Cloud Functions env. |
| **Data Privacy** | India DPDP Act 2023 & IT Rules 2011 compliant Privacy Policy, Terms & Erasure flow. | Host Privacy Policy at public HTTPS endpoint for Google Play Console. |
| **Location Tracking** | `RideTrackingService` (Foreground Service, battery-conscious, strictly during active rides). | Complete Google Play Location in Foreground declaration form. |
| **Crash Telemetry** | Global `AppCrashHandler` with automatic sensitive data redaction (`SecureLog`). | Link Firebase Crashlytics dashboard. |

---

## 2. Release Signing & Keystore Configuration

### A. Generating a Production Keystore
Generate a PKCS12 keystore using `keytool` from the JDK:
```bash
keytool -genkeypair -v -keystore cyclego-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias cyclego-alias
```

### B. Configuring `app/build.gradle` for Production Signing
Create a `keystore.properties` file in your secure CI/CD or local machine (do **NOT** commit to version control):
```properties
RELEASE_STORE_FILE=../cyclego-release-key.jks
RELEASE_STORE_PASSWORD=YourSecureKeystorePassword
RELEASE_KEY_ALIAS=cyclego-alias
RELEASE_KEY_PASSWORD=YourSecureKeyPassword
```

In `app/build.gradle`:
```groovy
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        release {
            if (keystorePropertiesFile.exists()) {
                storeFile file(keystoreProperties['RELEASE_STORE_FILE'])
                storePassword keystoreProperties['RELEASE_STORE_PASSWORD']
                keyAlias keystoreProperties['RELEASE_KEY_ALIAS']
                keyPassword keystoreProperties['RELEASE_KEY_PASSWORD']
            }
        }
    }
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
            buildConfigField "String", "ENVIRONMENT", "\"production\""
            buildConfigField "boolean", "ENABLE_SECURE_LOGS", "false"
        }
    }
}
```

### C. Extracting SHA-256 Fingerprints
Required for Google Maps, Firebase Authentication, and Play Integrity:
```bash
keytool -list -v -keystore cyclego-release-key.jks -alias cyclego-alias
```

---

## 3. Firebase Production Hardening

### A. Deploying Firestore & Storage Rules
Run the following from the project root:
```bash
firebase deploy --only firestore:rules,storage:rules
```

### B. Deploying Backend Cloud Functions
Install dependencies and deploy server-authoritative functions:
```bash
cd functions
npm install
firebase functions:config:set razorpay.key_id="rzp_live_XXXXXXXXXXXXXX" razorpay.key_secret="YYYYYYYYYYYYYYYYYYYYYY"
firebase deploy --only functions
```

### C. Enforcing Firebase App Check
1. Go to **Firebase Console** → **App Check**.
2. Select your Android application (`com.sanika.bicyclebookingapp`).
3. Add your Play Store **SHA-256** fingerprint.
4. Set enforcement status to **Enforce** for Firestore, Cloud Storage, and Cloud Functions.

---

## 4. Razorpay Indian Payment Gateway Live Transition

1. **Activate Live Account**: Complete KYC on the [Razorpay Merchant Dashboard](https://dashboard.razorpay.com).
2. **Generate Live API Keys**: Under **Settings** → **API Keys**, generate a Live Key ID (`rzp_live_...`) and Secret.
3. **Configure Firestore `payment_config`**:
   Store the public Live Key ID in Firestore:
   ```json
   {
     "keyId": "rzp_live_XXXXXXXXXXXXXX",
     "testMode": false,
     "currency": "INR",
     "companyName": "CycleGo India"
   }
   ```
4. **Setup Webhooks**:
   In Razorpay Dashboard, set the Webhook URL to your Cloud Function:
   `https://<region>-<project-id>.cloudfunctions.net/razorpayWebhook`
   Enable events:
   * `order.paid`
   * `payment.failed`
   * `refund.processed`

---

## 5. Google Play Console Data Safety & Permissions

### Permissions Justification:
* `android.permission.ACCESS_FINE_LOCATION` & `android.permission.ACCESS_COARSE_LOCATION`:
  * *Purpose*: Real-time bicycle tracking, station navigation, and geo-fencing.
  * *Policy*: Collected **only** when a ride is active. Never collected in the background when no ride is ongoing.
* `android.permission.FOREGROUND_SERVICE_LOCATION`:
  * *Purpose*: Maintains uninterrupted trip tracking and distance computation if the user switches apps or turns off the screen while riding.
* `android.permission.CAMERA`:
  * *Purpose*: Scanning QR codes on bicycles to unlock dock locks.
* `android.permission.POST_NOTIFICATIONS`:
  * *Purpose*: Critical transactional alerts (Booking confirmed, Overdue ride alert, Payment receipt, Support resolution).

### Data Safety Form Answers:
* **Location**: Approximate & Precise location collected for App Functionality (Bicycle rental session tracking). Ephemeral during ride.
* **Personal Info**: Name, Email address, Phone number for Account Management.
* **Financial Info**: Purchase history and transaction IDs. No credit card or bank account numbers collected directly by the app.
* **App Info & Performance**: Crash logs and diagnostics via `AppCrashHandler`.

---

## 6. Indian Statutory Compliance (DPDP Act 2023)

1. **Consent Architecture**: In-app consent provided on signup and profile screens.
2. **Right to Erasure**: Implemented in `LegalActivity.java` writing to `account_deletion_requests`. Data protection team handles erasure requests within 7 business days.
3. **Grievance Redressal**: Functional support tickets and in-app complaints routed to `support_tickets`.
4. **GST Billing**: Itemized CGST & SGST calculated in integer paise and displayed on invoices.
