# CycleGo — Bicycle Booking & Ride Tracking Android App

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com)
[![Backend](https://img.shields.io/badge/Backend-Firebase%20Firestore-yellow.svg)](https://firebase.google.com)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue.svg)](https://developer.android.com/topic/architecture)

A production-ready bicycle booking, ride tracking, and micro-mobility management system built with **Android (Java)** and **Firebase Cloud Firestore**. Designed for riders, on-ground cycle handlers, and platform administrators.

---

## 📑 Documentation Index

- 📘 **[PROJECT_AUDIT.md](PROJECT_AUDIT.md)**: Deep-dive architecture analysis, identified defects, and security audit.
- 🗺️ **[DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md)**: Detailed phase milestones, completed work, and future capabilities.
- 🧪 **[TESTING_REPORT.md](TESTING_REPORT.md)**: Comprehensive QA report, bug validation traces, and performance benchmarks.
- ⚙️ **[FIREBASE_SETUP.md](FIREBASE_SETUP.md)**: Step-by-step setup guide for Firestore, security rules, indexes, and Cloud Functions.

---

## ✨ Features by Role

### 🚴 1. Rider (User) Features
- **Authentication**: Email & password authentication with secure profile management and password reset.
- **Interactive Station Map**: Google Maps interface showing nearby stations with real-time bicycle count.
- **Instant Search & Filter**: Dynamically filter docking stations by name directly on the home screen.
- **QR Code Ride Unlock**: Unlock bicycles instantly using an integrated ZXing camera scanner.
- **Live Ride Metering**: Real-time foreground tracking displaying elapsed time, distance in km, and live metered fare (`₹5 base + ₹8/km + ₹1/min`).
- **Safe Docking**: Automatic station return detection with safeguards preventing premature end-ride triggers.
- **Ride History**: View past rides, duration, and receipts with soft-hiding capabilities.

### 🛠️ 2. Cycle Handler (Ground Operations) Features
- **Dedicated Portal (`CycleHandlerActivity`)**: Direct portal access for ground staff to manage station inventory.
- **Fleet & Maintenance Management**: Inspect station bicycles and toggle units between Active and Maintenance states with automatic count sync.
- **Booking Verification**: Verify rider Booking IDs or QR codes before dispatching bicycles.
- **Manual Ride Dispatch & Docking**: Start rides on behalf of riders or manually dock returned units and finalize fares.

### 🛡️ 3. Platform Administrator Features
- **Station & Fleet Control**: Create, update, and manage stations and fleet bicycles across city hubs.
- **Real-Time Active Tracking**: Track live rides in progress with continuous GPS telemetry updates.
- **Revenue & Analytics**: Monitor total bookings, filter rides by status (Active/Completed/Cancelled), and view live aggregated revenue.
- **Theft Detection System**: Automated background geofence listener detecting unreserved bicycle movement exceeding 200m from docking stations.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Language** | Java 11 |
| **Architecture** | MVVM (Model-View-ViewModel) with Repository Pattern |
| **UI Components** | XML Layouts, Material Design 3, ViewBinding |
| **Navigation** | Android Jetpack Navigation Component with Safe Args |
| **Asynchronous State** | Android Architecture Components (`LiveData`, `ViewModel`) |
| **Mapping & Location** | Google Maps SDK for Android, Google Play Services Location (`FusedLocationProviderClient`) |
| **QR Code Engine** | ZXing (`zxing-android-embedded`) |
| **Image Loading** | Bumptech Glide |
| **Cloud Backend** | Cloud Firestore (NoSQL), Firebase Authentication, Firebase Cloud Functions |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 11 or higher
- Android SDK with `minSdk 26` and `compileSdk 36`
- Firebase account with a Cloud Firestore database

### 1. Clone Repository
```bash
git clone https://github.com/sanikakhupire/Bicycle-Booking-System.git
cd Bicycle-Booking-System
```

### 2. Configure Firebase
1. Download `google-services.json` from your Firebase Console.
2. Place it in the `app/` directory:
   ```
   app/google-services.json
   ```
3. For comprehensive Firestore rules and index deployment, refer to **[FIREBASE_SETUP.md](FIREBASE_SETUP.md)**.

### 3. Configure Google Maps API Key
Open `app/src/main/res/values/strings.xml` and ensure your Google Maps API key is configured:
```xml
<string name="google_maps_key" translatable="false">YOUR_GOOGLE_MAPS_API_KEY</string>
```

### 4. Build and Run
Open the project in Android Studio, sync Gradle, and run on a physical device or emulator with Google Play Services enabled.

---

## 🐛 Resolved Critical Bugs

- **55-Year Timer Bug**: Resolved an asynchronous race condition in `ActiveRideFragment` that was initiating `RideTrackingService` before `startTimeMs` was fetched from Firestore.
- **Premature Auto-Termination**: Added ride guard conditions in `RideTrackingService` requiring 2+ minutes of active duration and 100+ meters of displacement before station proximity triggers end-ride.
- **Firestore Read Exhaustion**: Built a 5-minute memory cache in `StationProximityChecker`, eliminating continuous 10-second whole-collection queries.
- **Date Formatting Thread-Safety**: Replaced shared static `SimpleDateFormat` formatters in `DateUtil` with `ThreadLocal` instances.
- **History Deletion Permission Error**: Migrated hard document deletes in `HistoryFragment` to a non-destructive `hiddenByUser` flag.

---

## 📄 License
This project is developed for educational and demonstration purposes.
