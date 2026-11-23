package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * Support Ticket / Grievance model for customer dispute resolution and assistance.
 *
 * Status Lifecycle:
 * OPEN                 -> Newly submitted by customer
 * ASSIGNED             -> Assigned to a specific support agent
 * IN_PROGRESS          -> Actively being investigated
 * WAITING_FOR_CUSTOMER -> Support replied, waiting for customer information
 * RESOLVED             -> Issue fixed, resolution notes provided
 * CLOSED               -> Customer satisfied or ticket finalized
 * REOPENED             -> Customer reopened a resolved/closed ticket
 */
public class SupportTicket implements Serializable {

    // Statuses
    public static final String STATUS_OPEN                 = "OPEN";
    public static final String STATUS_ASSIGNED             = "ASSIGNED";
    public static final String STATUS_IN_PROGRESS          = "IN_PROGRESS";
    public static final String STATUS_WAITING_FOR_CUSTOMER = "WAITING_FOR_CUSTOMER";
    public static final String STATUS_RESOLVED             = "RESOLVED";
    public static final String STATUS_CLOSED               = "CLOSED";
    public static final String STATUS_REOPENED             = "REOPENED";

    // Categories
    public static final String CATEGORY_BILLING_DISPUTE = "BILLING_DISPUTE";
    public static final String CATEGORY_RIDE_PROBLEM    = "RIDE_PROBLEM";
    public static final String CATEGORY_PAYMENT_PROBLEM = "PAYMENT_PROBLEM";
    public static final String CATEGORY_DAMAGED_BICYCLE = "DAMAGED_BICYCLE";
    public static final String CATEGORY_STATION_PROBLEM = "STATION_PROBLEM";
    public static final String CATEGORY_UNSAFE_BICYCLE  = "UNSAFE_BICYCLE";
    public static final String CATEGORY_ACCOUNT_LOGIN   = "ACCOUNT_LOGIN";
    public static final String CATEGORY_REFUND_REQUEST  = "REFUND_REQUEST";
    public static final String CATEGORY_GENERAL         = "GENERAL";

    // Legacy compatibility constants
    public static final String CATEGORY_BILLING_ISSUE = CATEGORY_BILLING_DISPUTE;
    public static final String CATEGORY_THEFT_REPORT  = "THEFT_REPORT";

    // Priorities
    public static final String PRIORITY_LOW    = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH   = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    @DocumentId
    private String ticketId;
    private String userId;
    private String userName;
    private String userPhone;
    private String userEmail;

    private String category = CATEGORY_GENERAL;
    private String bookingId;
    private String bicycleId;
    private String stationId;
    private String subject;
    private String description;
    private String attachmentUrl;
    private long disputedAmountPaise;

    private String status = STATUS_OPEN;
    private String priority = PRIORITY_MEDIUM;
    private String assignedStaff;
    private String adminNotes;
    private String resolutionSummary;
    private String resolvedBy;

    @ServerTimestamp
    private Date createdAt;
    private Date assignedAt;
    private Date resolvedAt;
    private Date closedAt;
    private Date reopenedAt;
    private Date updatedAt;

    public SupportTicket() {}

    public SupportTicket(String userId, String userName, String category, String subject, String description) {
        this.userId = userId;
        this.userName = userName;
        this.category = category != null ? category : CATEGORY_GENERAL;
        this.subject = subject;
        this.description = description;
        this.status = STATUS_OPEN;
        this.priority = PRIORITY_MEDIUM;
    }

    public static boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        if (currentStatus.equalsIgnoreCase(targetStatus)) return true;

        switch (currentStatus.toUpperCase()) {
            case STATUS_OPEN:
                return STATUS_ASSIGNED.equalsIgnoreCase(targetStatus)
                        || STATUS_IN_PROGRESS.equalsIgnoreCase(targetStatus)
                        || STATUS_RESOLVED.equalsIgnoreCase(targetStatus)
                        || STATUS_CLOSED.equalsIgnoreCase(targetStatus);
            case STATUS_ASSIGNED:
                return STATUS_IN_PROGRESS.equalsIgnoreCase(targetStatus)
                        || STATUS_WAITING_FOR_CUSTOMER.equalsIgnoreCase(targetStatus)
                        || STATUS_RESOLVED.equalsIgnoreCase(targetStatus);
            case STATUS_IN_PROGRESS:
                return STATUS_WAITING_FOR_CUSTOMER.equalsIgnoreCase(targetStatus)
                        || STATUS_RESOLVED.equalsIgnoreCase(targetStatus)
                        || STATUS_CLOSED.equalsIgnoreCase(targetStatus);
            case STATUS_WAITING_FOR_CUSTOMER:
                return STATUS_IN_PROGRESS.equalsIgnoreCase(targetStatus)
                        || STATUS_RESOLVED.equalsIgnoreCase(targetStatus)
                        || STATUS_CLOSED.equalsIgnoreCase(targetStatus);
            case STATUS_RESOLVED:
                return STATUS_CLOSED.equalsIgnoreCase(targetStatus)
                        || STATUS_REOPENED.equalsIgnoreCase(targetStatus);
            case STATUS_CLOSED:
                return STATUS_REOPENED.equalsIgnoreCase(targetStatus);
            case STATUS_REOPENED:
                return STATUS_IN_PROGRESS.equalsIgnoreCase(targetStatus)
                        || STATUS_ASSIGNED.equalsIgnoreCase(targetStatus)
                        || STATUS_RESOLVED.equalsIgnoreCase(targetStatus);
            default:
                return false;
        }
    }

    public boolean isValidTransition(String targetStatus) {
        return isValidTransition(this.status, targetStatus);
    }

    public boolean isOpen() {
        return STATUS_OPEN.equalsIgnoreCase(status)
                || STATUS_ASSIGNED.equalsIgnoreCase(status)
                || STATUS_IN_PROGRESS.equalsIgnoreCase(status)
                || STATUS_WAITING_FOR_CUSTOMER.equalsIgnoreCase(status)
                || STATUS_REOPENED.equalsIgnoreCase(status);
    }

    // Getters and Setters
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(String assignedStaff) { this.assignedStaff = assignedStaff; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public long getDisputedAmountPaise() { return disputedAmountPaise; }
    public void setDisputedAmountPaise(long disputedAmountPaise) { this.disputedAmountPaise = disputedAmountPaise; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Date assignedAt) { this.assignedAt = assignedAt; }

    public Date getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Date resolvedAt) { this.resolvedAt = resolvedAt; }

    public Date getClosedAt() { return closedAt; }
    public void setClosedAt(Date closedAt) { this.closedAt = closedAt; }

    public Date getReopenedAt() { return reopenedAt; }
    public void setReopenedAt(Date reopenedAt) { this.reopenedAt = reopenedAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
