# CycleGo Development Roadmap

**Project:** CycleGo — Bicycle Booking & Ride Tracking Android Application  
**Package:** `com.sanika.bicyclebookingapp`  
**Platform:** Native Android (Java + XML Layouts)  
**Backend:** Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging, Cloud Functions  

---

## 📌 Executive Summary

This roadmap outlines the past, present, and future milestones for the CycleGo bicycle booking and ride management platform. CycleGo provides a complete end-to-end urban micro-mobility experience encompassing user reservations, real-time GPS tracking and live fare metering, station inventory management, ground-staff cycle handling, and administrative operations.

---

## 🗺️ Milestone Overview

```
[Phase 1: Audit & Discovery]  ──► COMPLETED
          │
[Phase 2: Security Hardening] ──► COMPLETED
          │
[Phase 3: Critical Bug Fixes] ──► COMPLETED
          │
[Phase 4: UI/UX Modernization]──► COMPLETED
          │
[Phase 5: Cycle Handler Ops]  ──► COMPLETED
          │
[Phase 6: Testing & QA]       ──► COMPLETED
          │
[Phase 7: Advanced Fleet & Offline] ──► PLANNED / FUTURE
```

---

## 🚀 Phase Details

### Phase 1: Comprehensive Codebase Audit & Baseline Documentation (Completed)
- [x] Full scan of all 53+ Java source files, 28+ XML layouts, and Gradle configurations.
- [x] Authored `PROJECT_AUDIT.md` highlighting architecture, security issues, performance bottlenecks, and missing role features.
- [x] Initialized Git tracking, established branch hygiene, and aligned with `origin/main`.
- [x] Sanitized `.gitignore` to prevent any exposure of sensitive files (`google-services.json`, keystores, build artifacts).

### Phase 2: Security Hardening & Secret Protection (Completed)
- [x] **API Key Externalization**: Replaced hardcoded Google Maps API key in `AndroidManifest.xml` with `@string/google_maps_key` resource references.
- [x] **Payment Details Protection**: Extracted personal UPI payment addresses out of Java source files into string resources and dynamic runtime lookups.
- [x] **Credential Deprecation**: Removed test credentials from legacy `PaymentManager` and marked unused payment services as `@Deprecated`.
- [x] **Role-Based Access Enforcement**: Secured `AdminActivity` with mandatory authentication and Firestore `isAdmin` privilege verification in `onCreate()`.

### Phase 3: Critical Bug Resolution & Performance Optimizations (Completed)
- [x] **55-Year Timer Bug (`ActiveRideFragment`)**: Fixed asynchronous race condition where `startTrackingService()` was invoked before `startTimeMs` was retrieved from Firestore. Tracking service now starts strictly after valid start timestamp assignment.
- [x] **Premature Auto-Termination (`RideTrackingService`)**: Implemented safeguards requiring at least 2 minutes of active ride duration and a minimum movement distance of 100 meters before station proximity checks trigger auto-completion.
- [x] **Firestore Read Throttling (`StationProximityChecker`)**: Added a 5-minute in-memory station cache with `CachedStation` entries, avoiding repetitive whole-collection reads during 10-second GPS polling intervals.
- [x] **Thread-Safe Date Formatting (`DateUtil`)**: Replaced shared static `SimpleDateFormat` instances with `ThreadLocal<SimpleDateFormat>` to prevent concurrent formatting errors.
- [x] **Soft-Hide Ride History (`HistoryFragment`)**: Resolved `PERMISSION_DENIED` errors caused by Firestore deletion rules by introducing `hiddenByUser = true` soft-deletion while maintaining transaction history for administrative accounting.
- [x] **Firestore Composite Indexes**: Added `{ status: ASC, createdAt: DESC }` index to `firestore.indexes.json` for ride history filtering.

### Phase 4: UI/UX Modernization & Responsiveness (Completed)
- [x] **Viewport & Clipping Fixes**: Converted `fragment_active_ride.xml` to `NestedScrollView` with `fillViewport="true"` to prevent button clipping on lower-resolution screens and large display font settings.
- [x] **Bottom Navigation Occlusion**: Added bottom scrolling padding (`paddingBottom="88dp"`, `clipToPadding="false"`) across `ProfileFragment` and list views to ensure bottom action buttons remain accessible.
- [x] **Station Search & Filtering**: Implemented live real-time textual filtering in `HomeFragment` map view (`etSearchStation`), allowing riders to search stations by name.
- [x] **Admin Analytics**: Enhanced `AllRidesHistoryFragment` with live revenue aggregation and ride count statistics.

### Phase 5: Ground Operations — Cycle Handler Module (Completed)
- [x] **Role Model Support**: Added `ROLE_HANDLER` enum in `User.java` and backwards-compatible role getters.
- [x] **Handler Portal (`CycleHandlerActivity`)**:
  - Station fleet overview displaying real-time available and total bicycle capacity.
  - Bicycle maintenance manager (`HandlerBicycleAdapter`) allowing staff to mark damaged bicycles for repair or return active units to the station pool with automatic inventory sync.
  - Ride verification and dispatch engine: cycle handlers can verify rider booking IDs, inspect bicycles, officially dispatch rides, or dock returned bicycles and finalize charges.
- [x] **Intelligent Splash Routing**: `SplashActivity` routes authenticated users to `AdminActivity`, `CycleHandlerActivity`, or `MainActivity` according to verified role.

---

## 🔮 Future Enhancements & Roadmap (Phases 6 - 8)

### Phase 6: Dynamic Pricing & Configuration
- **Firestore Pricing Config**: Shift hardcoded rates in `FareCalculator.java` (base fare, per-minute, per-km) to a real-time Firestore document `config/pricing`.
- **Dynamic Demand Multipliers**: Add surge and off-peak pricing support based on station bicycle availability percentages.

### Phase 7: Real-Time Fleet Telemetry & Theft Geo-Fencing
- **WebSocket / FCM Dispatch Alerts**: Push high-priority push notifications to cycle handlers when a station drops below 20% bicycle capacity.
- **Background Theft Push Notifications**: Connect `TheftAlertManager` to Firebase Cloud Functions for push notification dispatch when an unreserved bicycle leaves station bounds.

### Phase 8: Offline Capability & Room Sync
- **Local SQLite / Room Cache**: Cache stations and user booking history in Room database for instantaneous offline view and offline QR generation.
- **Battery Optimization**: Adapt GPS tracking frequency based on accelerometer activity (lowering frequency when stationary, increasing when moving).
