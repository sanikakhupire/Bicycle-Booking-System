package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.TicketMessage;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository orchestrating customer grievance management, ticket threaded messages,
 * resolution workflows, and administrative escalation.
 */
public class SupportRepository {

    public static final String COLLECTION_TICKETS = "support_tickets";
    public static final String SUBCOLLECTION_MESSAGES = "messages";

    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public SupportRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationRepository = new NotificationRepository(db);
    }

    public SupportRepository(FirebaseFirestore db, NotificationRepository notificationRepository) {
        this.db = db;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Customer creates a new support ticket / complaint.
     */
    public LiveData<Resource<String>> createTicket(SupportTicket ticket) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        DocumentReference docRef = db.collection(COLLECTION_TICKETS).document();
        ticket.setTicketId(docRef.getId());
        ticket.setCreatedAt(new Date());
        ticket.setUpdatedAt(new Date());

        docRef.set(ticket).addOnSuccessListener(unused -> {
            // Add initial message if description is present
            if (ticket.getDescription() != null && !ticket.getDescription().isEmpty()) {
                TicketMessage initialMsg = new TicketMessage(
                        ticket.getTicketId(),
                        ticket.getUserId(),
                        ticket.getUserName(),
                        TicketMessage.SENDER_CUSTOMER,
                        ticket.getDescription()
                );
                if (ticket.getAttachmentUrl() != null) {
                    initialMsg.setAttachmentUrl(ticket.getAttachmentUrl());
                }
                docRef.collection(SUBCOLLECTION_MESSAGES).add(initialMsg);
            }

            // Dispatch Customer Notification
            notificationRepository.dispatchNotification(null, new AppNotification(
                    ticket.getUserId(),
                    AppNotification.TYPE_SUPPORT_TICKET_UPDATE,
                    AppNotification.CHANNEL_ALERTS,
                    "Support Ticket Logged (#" + ticket.getTicketId().substring(0, Math.min(8, ticket.getTicketId().length())).toUpperCase() + ")",
                    "Your ticket regarding " + ticket.getCategory() + " has been received. Our support team will reply shortly.",
                    AppNotification.TARGET_SUPPORT,
                    ticket.getTicketId()
            ));

            // Dispatch Admin Alert
            notificationRepository.dispatchNotification(null, new AppNotification(
                    "ADMIN",
                    AppNotification.TYPE_ADMIN_CUSTOMER_COMPLAINT,
                    AppNotification.CHANNEL_ADMIN,
                    "New Support Grievance (" + ticket.getPriority() + ")",
                    "Customer " + ticket.getUserName() + " logged: " + ticket.getCategory() + " - " + ticket.getSubject(),
                    AppNotification.TARGET_SUPPORT,
                    ticket.getTicketId()
            ));

            result.setValue(Resource.success(ticket.getTicketId()));
        }).addOnFailureListener(e -> result.setValue(Resource.error("Failed to log ticket: " + e.getMessage(), null)));

        return result;
    }

    /**
     * Get real-time list of support tickets for a specific customer.
     */
    public LiveData<Resource<List<SupportTicket>>> getCustomerTickets(String userId) {
        MutableLiveData<Resource<List<SupportTicket>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_TICKETS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<SupportTicket> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            SupportTicket t = doc.toObject(SupportTicket.class);
                            if (t != null) {
                                t.setTicketId(doc.getId());
                                list.add(t);
                            }
                        }
                    }
                    list.sort((a, b) -> {
                        Date da = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                        Date db = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                        return db.compareTo(da);
                    });
                    result.setValue(Resource.success(list));
                });

        return result;
    }

    /**
     * Get real-time list of all support tickets for administrators with optional filtering.
     */
    public LiveData<Resource<List<SupportTicket>>> getAllTickets(String statusFilter) {
        MutableLiveData<Resource<List<SupportTicket>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Query query = db.collection(COLLECTION_TICKETS);
        if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            query = query.whereEqualTo("status", statusFilter.toUpperCase());
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                result.setValue(Resource.error(error.getMessage(), null));
                return;
            }

            List<SupportTicket> list = new ArrayList<>();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    SupportTicket t = doc.toObject(SupportTicket.class);
                    if (t != null) {
                        t.setTicketId(doc.getId());
                        list.add(t);
                    }
                }
            }
            list.sort((a, b) -> {
                Date da = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                Date db = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                return db.compareTo(da);
            });
            result.setValue(Resource.success(list));
        });

        return result;
    }

    /**
     * Real-time stream of threaded conversation messages for a ticket.
     */
    public LiveData<Resource<List<TicketMessage>>> getTicketMessages(String ticketId) {
        MutableLiveData<Resource<List<TicketMessage>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_TICKETS).document(ticketId)
                .collection(SUBCOLLECTION_MESSAGES)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<TicketMessage> messages = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            TicketMessage m = doc.toObject(TicketMessage.class);
                            if (m != null) {
                                m.setMessageId(doc.getId());
                                messages.add(m);
                            }
                        }
                    }
                    messages.sort((a, b) -> {
                        Date da = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                        Date db = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                        return da.compareTo(db);
                    });
                    result.setValue(Resource.success(messages));
                });

        return result;
    }

    /**
     * Send a threaded reply to a ticket.
     */
    public LiveData<Resource<Void>> sendTicketMessage(String ticketId, TicketMessage message) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        DocumentReference ticketRef = db.collection(COLLECTION_TICKETS).document(ticketId);
        message.setTicketId(ticketId);
        message.setCreatedAt(new Date());

        ticketRef.collection(SUBCOLLECTION_MESSAGES).add(message).addOnSuccessListener(ref -> {
            // Update ticket's updatedAt and status
            Map<String, Object> updates = new HashMap<>();
            updates.put("updatedAt", FieldValue.serverTimestamp());

            if (TicketMessage.SENDER_CUSTOMER.equalsIgnoreCase(message.getSenderRole())) {
                // If customer replied, move ticket back to IN_PROGRESS
                updates.put("status", SupportTicket.STATUS_IN_PROGRESS);
                notificationRepository.dispatchNotification(null, new AppNotification(
                        "ADMIN",
                        AppNotification.TYPE_ADMIN_CUSTOMER_COMPLAINT,
                        AppNotification.CHANNEL_ADMIN,
                        "Customer Replied on Ticket #" + ticketId.substring(0, Math.min(8, ticketId.length())),
                        message.getMessage(),
                        AppNotification.TARGET_SUPPORT,
                        ticketId
                ));
            } else {
                // If support/admin replied, mark WAITING_FOR_CUSTOMER
                updates.put("status", SupportTicket.STATUS_WAITING_FOR_CUSTOMER);
                // Fetch customer ID from ticket to notify
                ticketRef.get().addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        String custUid = snap.getString("userId");
                        if (custUid != null) {
                            notificationRepository.dispatchNotification(null, new AppNotification(
                                    custUid,
                                    AppNotification.TYPE_SUPPORT_TICKET_UPDATE,
                                    AppNotification.CHANNEL_ALERTS,
                                    "Support Reply Received",
                                    message.getMessage(),
                                    AppNotification.TARGET_SUPPORT,
                                    ticketId
                            ));
                        }
                    }
                });
            }

            ticketRef.update(updates);
            result.setValue(Resource.success(null));
        }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Assign ticket to support staff and set priority.
     */
    public LiveData<Resource<Void>> assignTicket(String ticketId, String staffName, String priority) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedStaff", staffName);
        updates.put("priority", priority != null ? priority : SupportTicket.PRIORITY_MEDIUM);
        updates.put("status", SupportTicket.STATUS_ASSIGNED);
        updates.put("assignedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_TICKETS).document(ticketId)
                .update(updates)
                .addOnSuccessListener(v -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Resolve ticket with resolution summary.
     */
    public LiveData<Resource<Void>> resolveTicket(String ticketId, String resolutionNotes, String adminId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", SupportTicket.STATUS_RESOLVED);
        updates.put("adminNotes", resolutionNotes);
        updates.put("resolutionSummary", resolutionNotes);
        updates.put("resolvedBy", adminId);
        updates.put("resolvedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        DocumentReference ticketRef = db.collection(COLLECTION_TICKETS).document(ticketId);
        ticketRef.update(updates).addOnSuccessListener(v -> {
            // Notify customer
            ticketRef.get().addOnSuccessListener(snap -> {
                if (snap.exists()) {
                    String custUid = snap.getString("userId");
                    if (custUid != null) {
                        notificationRepository.dispatchNotification(null, new AppNotification(
                                custUid,
                                AppNotification.TYPE_SUPPORT_TICKET_UPDATE,
                                AppNotification.CHANNEL_ALERTS,
                                "Grievance Ticket Resolved",
                                "Your ticket #" + ticketId.substring(0, Math.min(8, ticketId.length())) + " has been resolved: " + resolutionNotes,
                                AppNotification.TARGET_SUPPORT,
                                ticketId
                        ));
                    }
                }
            });
            result.setValue(Resource.success(null));
        }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Customer reopens a resolved or closed ticket.
     */
    public LiveData<Resource<Void>> reopenTicket(String ticketId, String reopenReason, String userId, String userName) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        DocumentReference ticketRef = db.collection(COLLECTION_TICKETS).document(ticketId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", SupportTicket.STATUS_REOPENED);
        updates.put("reopenedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        ticketRef.update(updates).addOnSuccessListener(v -> {
            // Append message to conversation thread
            TicketMessage msg = new TicketMessage(
                    ticketId,
                    userId,
                    userName,
                    TicketMessage.SENDER_CUSTOMER,
                    "⚠️ Ticket Reopened: " + reopenReason
            );
            ticketRef.collection(SUBCOLLECTION_MESSAGES).add(msg);

            // Alert admin
            notificationRepository.dispatchNotification(null, new AppNotification(
                    "ADMIN",
                    AppNotification.TYPE_ADMIN_CUSTOMER_COMPLAINT,
                    AppNotification.CHANNEL_ADMIN,
                    "Ticket Reopened by Customer",
                    "Ticket #" + ticketId.substring(0, Math.min(8, ticketId.length())) + " reopened: " + reopenReason,
                    AppNotification.TARGET_SUPPORT,
                    ticketId
            ));

            result.setValue(Resource.success(null));
        }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Closes a ticket.
     */
    public LiveData<Resource<Void>> closeTicket(String ticketId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", SupportTicket.STATUS_CLOSED);
        updates.put("closedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_TICKETS).document(ticketId)
                .update(updates)
                .addOnSuccessListener(v -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
