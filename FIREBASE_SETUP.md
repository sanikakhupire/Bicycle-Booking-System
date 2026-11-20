# CycleGo Firebase Setup & Backend Configuration Guide

**Application:** CycleGo Bicycle Booking & Ride Tracking  
**Package Name:** `com.sanika.bicyclebookingapp`  
**Database:** Cloud Firestore  
**Authentication:** Firebase Auth  
**Cloud Messaging:** Firebase Cloud Messaging (FCM)  
**Serverless Functions:** Firebase Cloud Functions (Node.js)  

---

## 📋 Table of Contents
1. [Firebase Project Setup](#1-firebase-project-setup)
2. [Firebase Authentication Configuration](#2-firebase-authentication-configuration)
3. [Cloud Firestore Setup](#3-cloud-firestore-setup)
4. [Firestore Security Rules](#4-firestore-security-rules)
5. [Firestore Indexes Deployment](#5-firestore-indexes-deployment)
6. [Firestore Collections & Schema](#6-firestore-collections--schema)
7. [Cloud Functions Deployment](#7-cloud-functions-deployment)
8. [Configuring Admin and Cycle Handler Roles](#8-configuring-admin-and-cycle-handler-roles)

---

## 1. Firebase Project Setup

1. Navigate to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** and name it `CycleGo` (or your preferred name).
3. Disable or enable Google Analytics according to your preference.
4. Once created, click the **Android icon** to register the Android application:
   - **Android package name:** `com.sanika.bicyclebookingapp`
   - **App nickname:** `CycleGo`
   - **Debug signing certificate SHA-1:** Run the Gradle command below to obtain your local SHA-1 fingerprint:
     ```bash
     ./gradlew signingReport
     ```
5. Download the generated `google-services.json` file.
6. Place `google-services.json` directly into your local `app/` directory:
   ```
   D:\MAD PROJECT\app\google-services.json
   ```
   *(Note: This file is intentionally ignored by `.gitignore` to protect credentials).*

---

## 2. Firebase Authentication Configuration

1. In the Firebase Console, go to **Build > Authentication**.
2. Click **Get Started** and navigate to the **Sign-in method** tab.
3. Enable **Email/Password**:
   - Enable "Email/Password".
   - (Optional) "Email link (passwordless sign-in)" can remain disabled.
4. Click **Save**.

---

## 3. Cloud Firestore Setup

1. In the Firebase Console, go to **Build > Firestore Database**.
2. Click **Create Database**.
3. Choose a Firestore location closest to your target audience (e.g., `asia-south1` for Mumbai/India).
4. Select **Start in production mode** (rules will be applied in the next step).
5. Click **Create**.

---

## 4. Firestore Security Rules

Deploy the included `firestore.rules` file to protect data access and enforce role-based permissions.

### Option A: Via Firebase CLI (Recommended)
Ensure you have the Firebase CLI installed (`npm install -g firebase-tools`):
```bash
firebase login
firebase use --add <your-project-id>
firebase deploy --only firestore:rules
```

### Option B: Via Firebase Console
1. In Firebase Console, go to **Firestore Database > Rules**.
2. Paste the contents of `firestore.rules` from this repository:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    function isAdmin() {
      return isAuthenticated() &&
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isAdmin == true;
    }

    match /users/{userId} {
      allow read:   if isOwner(userId) || isAdmin();
      allow create: if isAuthenticated() && isOwner(userId);
      allow update: if isOwner(userId) || isAdmin();
      allow delete: if isAdmin();
    }

    match /stations/{stationId} {
      allow read:   if isAuthenticated();
      allow write:  if isAdmin();
    }

    match /bicycles/{bicycleId} {
      allow read:   if isAuthenticated();
      allow create: if isAdmin();
      allow update: if isAuthenticated();
      allow delete: if isAdmin();
    }

    match /bookings/{bookingId} {
      allow read:   if isAuthenticated() &&
                       (resource.data.userId == request.auth.uid || isAdmin());
      allow create: if isAuthenticated() &&
                       request.resource.data.userId == request.auth.uid;
      allow update: if isAuthenticated() &&
                       (resource.data.userId == request.auth.uid || isAdmin());
      allow delete: if isAdmin();
    }

    match /payments/{paymentId} {
      allow read:   if isAuthenticated() &&
                       (resource.data.userId == request.auth.uid || isAdmin());
      allow create: if isAuthenticated();
      allow update: if isAdmin();
      allow delete: if isAdmin();
    }
  }
}
```
3. Click **Publish**.

---

## 5. Firestore Indexes Deployment

The project requires composite indexes for complex filtering and ordering.

### Option A: Via Firebase CLI
```bash
firebase deploy --only firestore:indexes
```

### Option B: Auto-generation from App Logs
When you perform a compound query on Android for the first time, Firestore generates a direct index creation link in the Logcat. Simply click the link to build the index in the Firebase Console.

The configured indexes in `firestore.indexes.json` are:
1. `bookings`: `userId` ASC, `createdAt` DESC
2. `bookings`: `userId` ASC, `status` ASC
3. `bookings`: `status` ASC, `createdAt` DESC *(Admin All Rides History)*
4. `bicycles`: `stationId` ASC, `available` ASC

---

## 6. Firestore Collections & Schema

### `users`
| Field | Type | Description |
|---|---|---|
| `uid` | String | Firebase Authentication UID |
| `name` | String | User full name |
| `email` | String | User email address |
| `phone` | String | Contact number |
| `isAdmin` | Boolean | True for platform administrators |
| `role` | String | `"USER"`, `"ADMIN"`, or `"HANDLER"` |
| `createdAt` | Timestamp | Account creation date |

### `stations`
| Field | Type | Description |
|---|---|---|
| `name` | String | Station display name |
| `location` | GeoPoint | Latitude and Longitude coordinate |
| `availableBicycles` | Number | Count of active bicycles at dock |
| `totalCapacity` | Number | Maximum dock slots |
| `address` | String | Physical station address description |

### `bicycles`
| Field | Type | Description |
|---|---|---|
| `bicycleId` | String | Unique identifier |
| `stationId` | String | Docked station document ID |
| `stationName` | String | Docked station name |
| `type` | String | Model (Standard, Electric, Mountain) |
| `available` | Boolean | Availability flag for booking |
| `underMaintenance` | Boolean | Maintenance flag set by ground handler |
| `qrCode` | String | Identifier string encoded on frame QR code |
| `lastLocation` | GeoPoint | Latest GPS telemetry coordinate |
| `lastLocationTime` | Timestamp | Timestamp of last telemetry point |

### `bookings`
| Field | Type | Description |
|---|---|---|
| `bookingId` | String | Unique booking reference |
| `userId` | String | Rider's UID |
| `bicycleId` | String | Reserved bicycle ID |
| `stationId` | String | Origin station ID |
| `stationName` | String | Origin station name |
| `status` | String | `"PENDING"`, `"ACTIVE"`, `"COMPLETED"`, `"CANCELLED"` |
| `startTime` | Timestamp | Ride initiation time |
| `endTime` | Timestamp | Ride completion time |
| `distanceKm` | Number | Cumulative tracked distance |
| `currentFare` | Number | Dynamic live metered fare |
| `fare` | Number | Final settled fare amount |
| `durationMinutes` | Number | Total elapsed ride minutes |
| `returnStationId` | String | Station where bicycle was returned |
| `returnStationName`| String | Return station name |
| `hiddenByUser` | Boolean | Set to true when rider hides from history |

---

## 7. Cloud Functions Deployment

The `functions/` folder contains backend Node.js functions for server-side fare calculation and verification:

1. Navigate to the functions directory:
   ```bash
   cd functions
   npm install
   ```
2. Deploy the functions to Firebase:
   ```bash
   firebase deploy --only functions
   ```
*(Note: If Cloud Functions are not deployed, the application will automatically fall back to client-side fare calculation using `FareCalculator.java`).*

---

## 8. Configuring Admin and Cycle Handler Roles

To designate user roles for testing or operation:

1. Create a user account normally in the application (via Sign Up screen).
2. Note the user's `UID` from **Firebase Authentication**.
3. In **Firestore Database**, find the document under `users/{uid}`.
4. Add or update the fields:
   - **For Administrator:**
     - Set `isAdmin`: `true`
     - Set `role`: `"ADMIN"`
   - **For Cycle Handler (Ground Staff):**
     - Set `isAdmin`: `false`
     - Set `role`: `"HANDLER"`
5. Relaunch the application. The `SplashActivity` will automatically route the user to their designated portal (`AdminActivity` or `CycleHandlerActivity`).
