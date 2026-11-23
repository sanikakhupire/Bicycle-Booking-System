package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for managing bicycle maintenance, damage reports, repairs, and lifecycle restoration.
 */
public class MaintenanceRepository {

    public static final String COLLECTION_MAINTENANCE = "maintenance_records";
    public static final String COLLECTION_BICYCLES    = "bicycles";
    public static final String COLLECTION_STATIONS    = "stations";

    private final FirebaseFirestore db;

    public MaintenanceRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public MaintenanceRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Customer or Handler reports bicycle damage / unsafe condition.
     * Atomically creates maintenance record and grounds the bicycle (sets DAMAGED, underMaintenance=true, available=false).
     */
    public LiveData<Resource<String>> reportDamage(MaintenanceRecord record) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (record.getBicycleId() == null || record.getBicycleId().trim().isEmpty()) {
            result.setValue(Resource.error("Bicycle ID is required to report damage", null));
            return result;
        }

        record.setStatus(MaintenanceRecord.STATUS_REPORTED);
        record.setReportedAt(new Date());

        // Run atomic transaction to update bicycle status and station count
        db.runTransaction(transaction -> {
            DocumentReference bikeRef = db.collection(COLLECTION_BICYCLES).document(record.getBicycleId());
            var bikeSnap = transaction.get(bikeRef);

            if (!bikeSnap.exists()) {
                throw new RuntimeException("Bicycle not found: " + record.getBicycleId());
            }

            Boolean wasAvailable = bikeSnap.getBoolean("available");
            String stationId = bikeSnap.getString("stationId");
            if (record.getStationId() == null && stationId != null) {
                record.setStationId(stationId);
                record.setStationName(bikeSnap.getString("stationName"));
            }

            // Ground bicycle
            Map<String, Object> bikeUpdates = new HashMap<>();
            bikeUpdates.put("status", record.isUnsafe() ? Bicycle.STATUS_DAMAGED : Bicycle.STATUS_MAINTENANCE);
            bikeUpdates.put("underMaintenance", true);
            bikeUpdates.put("available", false);
            transaction.update(bikeRef, bikeUpdates);

            // If bicycle was docked and available, decrement station available count
            if (Boolean.TRUE.equals(wasAvailable) && stationId != null && !stationId.isEmpty()) {
                DocumentReference stationRef = db.collection(COLLECTION_STATIONS).document(stationId);
                transaction.update(stationRef, "availableBicycles", FieldValue.increment(-1));
            }

            // Create record
            DocumentReference newRecordRef = db.collection(COLLECTION_MAINTENANCE).document();
            record.setRecordId(newRecordRef.getId());
            transaction.set(newRecordRef, record);

            return newRecordRef.getId();
        }).addOnSuccessListener(recordId -> {
            NotificationRepository notifRepo = new NotificationRepository(db);
            notifRepo.dispatchNotification(null, new AppNotification(
                    record.getReportedByUserId(),
                    AppNotification.TYPE_MAINTENANCE_ALERT,
                    AppNotification.CHANNEL_ALERTS,
                    "Damage Report Received",
                    "Bicycle " + record.getBicycleId() + " has been grounded for inspection. Thank you for keeping riders safe!",
                    AppNotification.TARGET_MAINTENANCE,
                    record.getBicycleId()
            ));
            notifRepo.dispatchNotification(null, new AppNotification(
                    "ADMIN",
                    AppNotification.TYPE_ADMIN_DAMAGE_REPORT,
                    AppNotification.CHANNEL_ADMIN,
                    "⚠️ Bicycle Damage Grounding",
                    "Bicycle " + record.getBicycleId() + " reported for " + record.getDamageCategory() + " (" + (record.isUnsafe() ? "CRITICAL" : "MEDIUM") + ")",
                    AppNotification.TARGET_MAINTENANCE,
                    record.getBicycleId()
            ));
            result.setValue(Resource.success(recordId));
        }).addOnFailureListener(e -> {
            result.setValue(Resource.error("Failed to submit maintenance report: " + e.getMessage(), null));
        });

        return result;
    }

    /**
     * Get live list of all maintenance records, optionally filtered by status.
     */
    public LiveData<Resource<List<MaintenanceRecord>>> getMaintenanceRecords(String statusFilter) {
        MutableLiveData<Resource<List<MaintenanceRecord>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Query query = db.collection(COLLECTION_MAINTENANCE);
        if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            query = query.whereEqualTo("status", statusFilter.toUpperCase());
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                result.setValue(Resource.error(error.getMessage(), null));
                return;
            }

            List<MaintenanceRecord> records = new ArrayList<>();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    MaintenanceRecord r = doc.toObject(MaintenanceRecord.class);
                    if (r != null) {
                        r.setRecordId(doc.getId());
                        records.add(r);
                    }
                }
            }
            // Sort by reportedAt descending
            records.sort((a, b) -> {
                Date da = a.getReportedAt() != null ? a.getReportedAt() : new Date(0);
                Date db = b.getReportedAt() != null ? b.getReportedAt() : new Date(0);
                return db.compareTo(da);
            });
            result.setValue(Resource.success(records));
        });

        return result;
    }

    /**
     * Get maintenance history specifically for a single bicycle.
     */
    public LiveData<Resource<List<MaintenanceRecord>>> getMaintenanceHistoryForBicycle(String bicycleId) {
        MutableLiveData<Resource<List<MaintenanceRecord>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_MAINTENANCE)
                .whereEqualTo("bicycleId", bicycleId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<MaintenanceRecord> records = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            MaintenanceRecord r = doc.toObject(MaintenanceRecord.class);
                            if (r != null) {
                                r.setRecordId(doc.getId());
                                records.add(r);
                            }
                        }
                    }
                    records.sort((a, b) -> {
                        Date da = a.getReportedAt() != null ? a.getReportedAt() : new Date(0);
                        Date db = b.getReportedAt() != null ? b.getReportedAt() : new Date(0);
                        return db.compareTo(da);
                    });
                    result.setValue(Resource.success(records));
                });

        return result;
    }

    /**
     * Step 1: Assign staff personnel and priority.
     */
    public LiveData<Resource<Void>> assignStaffAndPriority(String recordId, String staffName, String priority) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedStaff", staffName);
        updates.put("priority", priority != null ? priority : MaintenanceRecord.PRIORITY_MEDIUM);
        updates.put("status", MaintenanceRecord.STATUS_ASSIGNED);
        updates.put("assignedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_MAINTENANCE).document(recordId)
                .update(updates)
                .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Step 2: Add diagnosis and move status to IN_PROGRESS.
     */
    public LiveData<Resource<Void>> startDiagnosisAndRepair(String recordId, String diagnosisNotes) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("diagnosisNotes", diagnosisNotes);
        updates.put("status", MaintenanceRecord.STATUS_IN_PROGRESS);
        updates.put("startedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_MAINTENANCE).document(recordId)
                .update(updates)
                .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Step 3: Complete repair — log parts replaced, repair notes, cost in paise, and images.
     */
    public LiveData<Resource<Void>> completeRepair(String recordId, String repairNotes, String partsReplaced,
                                                  long costPaise, String beforeImageUrl, String afterImageUrl) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("repairNotes", repairNotes);
        updates.put("partsReplaced", partsReplaced);
        updates.put("costPaise", costPaise);
        if (beforeImageUrl != null && !beforeImageUrl.isEmpty()) {
            updates.put("beforeImageUrl", beforeImageUrl);
        }
        if (afterImageUrl != null && !afterImageUrl.isEmpty()) {
            updates.put("afterImageUrl", afterImageUrl);
        }
        updates.put("status", MaintenanceRecord.STATUS_COMPLETED);
        updates.put("completedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_MAINTENANCE).document(recordId)
                .update(updates)
                .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Step 4: Verify bicycle condition & restore bicycle to AVAILABLE service.
     * Atomically marks record VERIFIED and restores bicycle availability and station count.
     */
    public LiveData<Resource<Void>> verifyAndRestoreBicycle(String recordId, String bicycleId,
                                                           String adminId, String adminName,
                                                           String verificationNotes) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.runTransaction(transaction -> {
            DocumentReference recordRef = db.collection(COLLECTION_MAINTENANCE).document(recordId);
            DocumentReference bikeRef   = db.collection(COLLECTION_BICYCLES).document(bicycleId);

            var recordSnap = transaction.get(recordRef);
            var bikeSnap   = transaction.get(bikeRef);

            if (!recordSnap.exists()) {
                throw new RuntimeException("Maintenance record not found");
            }
            if (!bikeSnap.exists()) {
                throw new RuntimeException("Bicycle not found");
            }

            // 1. Update maintenance record to VERIFIED
            Map<String, Object> recordUpdates = new HashMap<>();
            recordUpdates.put("status", MaintenanceRecord.STATUS_VERIFIED);
            recordUpdates.put("verifiedByAdminId", adminId);
            recordUpdates.put("verifiedByAdminName", adminName);
            recordUpdates.put("verificationNotes", verificationNotes);
            recordUpdates.put("verifiedAt", new Date());
            recordUpdates.put("updatedAt", new Date());
            transaction.update(recordRef, recordUpdates);

            // 2. Restore bicycle to AVAILABLE
            Map<String, Object> bikeUpdates = new HashMap<>();
            bikeUpdates.put("status", Bicycle.STATUS_AVAILABLE);
            bikeUpdates.put("underMaintenance", false);
            bikeUpdates.put("available", true);
            transaction.update(bikeRef, bikeUpdates);

            // 3. Increment station count if bicycle belongs to a station
            String stationId = bikeSnap.getString("stationId");
            if (stationId != null && !stationId.isEmpty()) {
                DocumentReference stationRef = db.collection(COLLECTION_STATIONS).document(stationId);
                transaction.update(stationRef, "availableBicycles", FieldValue.increment(1));
            }

            return null;
        }).addOnSuccessListener(unused -> {
            NotificationRepository notifRepo = new NotificationRepository(db);
            notifRepo.dispatchNotification(null, new AppNotification(
                    "ADMIN",
                    AppNotification.TYPE_ADMIN_MAINTENANCE_REQUEST,
                    AppNotification.CHANNEL_ADMIN,
                    "Bicycle Quality Verified & Restored",
                    "Bicycle " + bicycleId + " condition signed off and restored to AVAILABLE in station fleet.",
                    AppNotification.TARGET_MAINTENANCE,
                    bicycleId
            ));
            result.setValue(Resource.success(null));
        }).addOnFailureListener(e -> {
            result.setValue(Resource.error("Failed to verify and restore bicycle: " + e.getMessage(), null));
        });

        return result;
    }

    /**
     * Reject maintenance report (e.g. invalid report, false alarm).
     * If no other active maintenance records exist for this bicycle, restores it to AVAILABLE.
     */
    public LiveData<Resource<Void>> rejectReport(String recordId, String bicycleId, String reason) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", MaintenanceRecord.STATUS_REJECTED);
        updates.put("rejectionReason", reason);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_MAINTENANCE).document(recordId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    // Check if bicycle can be restored
                    if (bicycleId != null && !bicycleId.isEmpty()) {
                        db.collection(COLLECTION_MAINTENANCE)
                                .whereEqualTo("bicycleId", bicycleId)
                                .whereIn("status", List.of(
                                        MaintenanceRecord.STATUS_REPORTED,
                                        MaintenanceRecord.STATUS_ASSIGNED,
                                        MaintenanceRecord.STATUS_IN_PROGRESS,
                                        MaintenanceRecord.STATUS_COMPLETED
                                ))
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    if (snapshots == null || snapshots.isEmpty()) {
                                        // No active maintenance left, restore bicycle
                                        Map<String, Object> bikeUpdates = new HashMap<>();
                                        bikeUpdates.put("status", Bicycle.STATUS_AVAILABLE);
                                        bikeUpdates.put("underMaintenance", false);
                                        bikeUpdates.put("available", true);
                                        db.collection(COLLECTION_BICYCLES).document(bicycleId).update(bikeUpdates);
                                    }
                                });
                    }
                    result.setValue(Resource.success(null));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
