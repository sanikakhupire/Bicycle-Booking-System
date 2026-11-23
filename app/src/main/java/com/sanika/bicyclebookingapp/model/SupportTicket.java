package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Support Ticket / Complaint model for customer grievance redressal.
 */
public class SupportTicket {

    public static final String STATUS_OPEN        = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED    = "RESOLVED";
    public static final String STATUS_CLOSED      = "CLOSED";

    public static final String CATEGORY_DAMAGED_BICYCLE = "DAMAGED_BICYCLE";
    public static final String CATEGORY_BILLING_ISSUE   = "BILLING_ISSUE";
    public static final String CATEGORY_STATION_PROBLEM = "STATION_PROBLEM";
    public static final String CATEGORY_THEFT_REPORT    = "THEFT_REPORT";
    public static final String CATEGORY_GENERAL         = "GENERAL";

    public static final String PRIORITY_LOW    = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH   = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    @DocumentId
    private String ticketId;
    private String userId;
    private String userName;
    private String userPhone;

    private String category = CATEGORY_GENERAL;
    private String bookingId;
    private String bicycleId;
    private String subject;
    private String description;

    private String status = STATUS_OPEN;
    private String priority = PRIORITY_MEDIUM;
    private String adminNotes;
    private String resolvedBy;

    @ServerTimestamp
    private Date createdAt;
    private Date resolvedAt;

    public SupportTicket() {}

    public SupportTicket(String userId, String userName, String category, String subject, String description) {
        this.userId = userId;
        this.userName = userName;
        this.category = category;
        this.subject = subject;
        this.description = description;
        this.status = STATUS_OPEN;
        this.priority = PRIORITY_MEDIUM;
    }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getBicycleId() { return bicycleId; }
    public void setBicycleId(String bicycleId) { this.bicycleId = bicycleId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Date resolvedAt) { this.resolvedAt = resolvedAt; }

    public boolean isOpen() {
        return STATUS_OPEN.equalsIgnoreCase(status) || STATUS_IN_PROGRESS.equalsIgnoreCase(status);
    }
}
