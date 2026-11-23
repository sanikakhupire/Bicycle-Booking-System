package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * Message within a support ticket thread between customer and support personnel.
 */
public class TicketMessage implements Serializable {

    public static final String SENDER_CUSTOMER = "CUSTOMER";
    public static final String SENDER_SUPPORT  = "SUPPORT";
    public static final String SENDER_ADMIN    = "ADMIN";

    @DocumentId
    private String messageId;
    private String ticketId;
    private String senderId;
    private String senderName;
    private String senderRole = SENDER_CUSTOMER;
    private String message;
    private String attachmentUrl;

    @ServerTimestamp
    private Date createdAt;

    public TicketMessage() {}

    public TicketMessage(String ticketId, String senderId, String senderName, String senderRole, String message) {
        this.ticketId = ticketId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole != null ? senderRole : SENDER_CUSTOMER;
        this.message = message;
        this.createdAt = new Date();
    }

    public boolean isFromCustomer() {
        return SENDER_CUSTOMER.equalsIgnoreCase(senderRole);
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
