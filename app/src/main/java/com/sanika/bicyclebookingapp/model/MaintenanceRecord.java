package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a bicycle maintenance or repair ticket in Firestore collection "maintenance_records".
 *
 * Maintenance Lifecycle:
 * REPORTED    -> Issue flagged by customer, handler, or admin
 * ASSIGNED    -> Maintenance personnel assigned with priority level
 * IN_PROGRESS -> Diagnosis done, bicycle actively undergoing repairs
 * COMPLETED   -> Repair finished, parts logged, cost recorded, ready for inspection
 * VERIFIED    -> Condition verified by admin; bicycle restored to AVAILABLE service
 * REJECTED    -> Deemed invalid, duplicate, or normal wear-and-tear
 */
public class MaintenanceRecord implements Serializable {

    // Statuses
    public static final String STATUS_REPORTED    = "REPORTED";
    public static final String STATUS_ASSIGNED    = "ASSIGNED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED   = "COMPLETED";
    public static final String STATUS_VERIFIED    = "VERIFIED";
    public static final String STATUS_REJECTED    = "REJECTED";

    // Priorities
    public static final String PRIORITY_LOW      = "LOW";
    public static final String PRIORITY_MEDIUM   = "MEDIUM";
    public static final String PRIORITY_HIGH     = "HIGH";
    public static final String PRIORITY_CRITICAL = "CRITICAL";

    // Damage Categories
    public static final String CAT_BRAKES         = "Brakes Malfunction";
    public static final String CAT_TIRES_WHEELS   = "Flat Tire / Wheel Damage";
    public static final String CAT_CHAIN_PEDALS   = "Broken Chain / Pedals";
    public static final String CAT_GEARS          = "Gear Shifting Issue";
    public static final String CAT_FRAME_FORK     = "Frame / Fork Structural Damage";
    public static final String CAT_LIGHTS_BELL    = "Lights / Bell Inoperative";
    public static final String CAT_HANDLEBAR      = "Handlebar / Alignment Problem";
    public static final String CAT_UNSAFE_GENERAL = "Unsafe Riding Condition";
    public static final String CAT_OTHER          = "Other Mechanical Problem";

    @DocumentId
    private String recordId;
    private String bicycleId;
    private String bookingId;
    private String stationId;
    private String stationName;

    // Customer / Reporter info
    private String reportedByUserId;
    private String reporterName;
    private String reporterPhone;
    private String reporterRole; // "CUSTOMER", "HANDLER", "ADMIN"

    // Problem description
    private String damageCategory = CAT_OTHER;
    private String description;
    private boolean isUnsafe = false;
    private String reportedImageUrl;

    // Maintenance triage
    private String status = STATUS_REPORTED;
    private String priority = PRIORITY_MEDIUM;
    private String assignedStaff;

    // Diagnosis & Repair notes
    private String diagnosisNotes;
    private String repairNotes;
    private String partsReplaced; // Comma separated or list description

    // Financials in INR
    private long costPaise = 0L; // Cost in integer paise

    // Proof / Photos
    private String beforeImageUrl;
    private String afterImageUrl;

    // Quality check & Verification
    private String verifiedByAdminId;
    private String verifiedByAdminName;
    private String verificationNotes;
    private String rejectionReason;

    // Timestamps
    @ServerTimestamp
    private Date reportedAt;
    private Date assignedAt;
    private Date startedAt;
    private Date completedAt;
    private Date verifiedAt;
    private Date updatedAt;

    public MaintenanceRecord() {}

    public MaintenanceRecord(String bicycleId, String reportedByUserId, String reporterName,
                             String damageCategory, String description, boolean isUnsafe) {
        this.bicycleId = bicycleId;
        this.reportedByUserId = reportedByUserId;
        this.reporterName = reporterName;
        this.damageCategory = damageCategory != null ? damageCategory : CAT_OTHER;
        this.description = description;
        this.isUnsafe = isUnsafe;
        this.status = STATUS_REPORTED;
        this.priority = isUnsafe ? PRIORITY_CRITICAL : PRIORITY_MEDIUM;
        this.costPaise = 0L;
    }

    // Status transition validators
    public static boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        if (currentStatus.equalsIgnoreCase(targetStatus)) return true;

        switch (currentStatus.toUpperCase()) {
            case STATUS_REPORTED:
                return STATUS_ASSIGNED.equalsIgnoreCase(targetStatus)
                        || STATUS_IN_PROGRESS.equalsIgnoreCase(targetStatus)
                        || STATUS_REJECTED.equalsIgnoreCase(targetStatus);
            case STATUS_ASSIGNED:
                return STATUS_IN_PROGRESS.equalsIgnoreCase(targetStatus)
                        || STATUS_REJECTED.equalsIgnoreCase(targetStatus);
            case STATUS_IN_PROGRESS:
                return STATUS_COMPLETED.equalsIgnoreCase(targetStatus)
                        || STATUS_REJECTED.equalsIgnoreCase(targetStatus);
            case STATUS_COMPLETED:
                return STATUS_VERIFIED.equalsIgnoreCase(targetStatus)
                        || STATUS_IN_PROGRESS.equalsIgnoreCase(targetStatus); // Re-open if failed QC
            case STATUS_VERIFIED:
            case STATUS_REJECTED:
                return false; // Terminal states
            default:
                return false;
        }
    }

    // Getters and Setters
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(String reportedByUserId) { this.reportedByUserId = reportedByUserId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }

    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String reporterRole) { this.reporterRole = reporterRole; }

    public String getDamageCategory() { return damageCategory; }
    public void setDamageCategory(String damageCategory) { this.damageCategory = damageCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isUnsafe() { return isUnsafe; }
    public void setUnsafe(boolean unsafe) { isUnsafe = unsafe; }

    public String getReportedImageUrl() { return reportedImageUrl; }
    public void setReportedImageUrl(String reportedImageUrl) { this.reportedImageUrl = reportedImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(String assignedStaff) { this.assignedStaff = assignedStaff; }

    public String getDiagnosisNotes() { return diagnosisNotes; }
    public void setDiagnosisNotes(String diagnosisNotes) { this.diagnosisNotes = diagnosisNotes; }

    public String getRepairNotes() { return repairNotes; }
    public void setRepairNotes(String repairNotes) { this.repairNotes = repairNotes; }

    public String getPartsReplaced() { return partsReplaced; }
    public void setPartsReplaced(String partsReplaced) { this.partsReplaced = partsReplaced; }

    public long getCostPaise() { return costPaise; }
    public void setCostPaise(long costPaise) { this.costPaise = costPaise; }

    public double getCostInr() { return costPaise / 100.0; }
    public void setCostInr(double inr) { this.costPaise = Math.round(inr * 100.0); }

    public String getFormattedCost() {
        return CurrencyUtil.formatInr(getCostInr());
    }

    public String getBeforeImageUrl() { return beforeImageUrl; }
    public void setBeforeImageUrl(String beforeImageUrl) { this.beforeImageUrl = beforeImageUrl; }

    public String getAfterImageUrl() { return afterImageUrl; }
    public void setAfterImageUrl(String afterImageUrl) { this.afterImageUrl = afterImageUrl; }

    public String getVerifiedByAdminId() { return verifiedByAdminId; }
    public void setVerifiedByAdminId(String verifiedByAdminId) { this.verifiedByAdminId = verifiedByAdminId; }

    public String getVerifiedByAdminName() { return verifiedByAdminName; }
    public void setVerifiedByAdminName(String verifiedByAdminName) { this.verifiedByAdminName = verifiedByAdminName; }

    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Date getReportedAt() { return reportedAt; }
    public void setReportedAt(Date reportedAt) { this.reportedAt = reportedAt; }

    public Date getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Date assignedAt) { this.assignedAt = assignedAt; }

    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public Date getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Date verifiedAt) { this.verifiedAt = verifiedAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
