package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model representing an account & personal data erasure request
 * compliant with India's DPDP Act 2023 and GDPR guidelines.
 */
public class AccountDeletionRequest {

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_REJECTED  = "REJECTED";

    @DocumentId
    private String requestId;
    private String userId;
    private String userEmail;
    private String reason;
    private String status = STATUS_PENDING;

    @ServerTimestamp
    private Date createdAt;
    private Date processedAt;

    public AccountDeletionRequest() {}

    public AccountDeletionRequest(String userId, String userEmail, String reason) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.reason = reason;
        this.status = STATUS_PENDING;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getProcessedAt() { return processedAt; }
    public void setProcessedAt(Date processedAt) { this.processedAt = processedAt; }
}
