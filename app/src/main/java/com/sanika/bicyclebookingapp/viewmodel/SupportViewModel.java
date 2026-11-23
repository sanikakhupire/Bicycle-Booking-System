package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.TicketMessage;
import com.sanika.bicyclebookingapp.repository.SupportRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel orchestrating Customer Support tickets, Help Center disputes,
 * threaded conversation replies, and status transitions.
 */
public class SupportViewModel extends ViewModel {

    private final SupportRepository repository;

    public SupportViewModel() {
        this.repository = new SupportRepository();
    }

    public SupportViewModel(SupportRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<String>> createTicket(SupportTicket ticket) {
        return repository.createTicket(ticket);
    }

    public LiveData<Resource<List<SupportTicket>>> getCustomerTickets(String userId) {
        return repository.getCustomerTickets(userId);
    }

    public LiveData<Resource<List<SupportTicket>>> getAllTickets(String statusFilter) {
        return repository.getAllTickets(statusFilter);
    }

    public LiveData<Resource<List<TicketMessage>>> getTicketMessages(String ticketId) {
        return repository.getTicketMessages(ticketId);
    }

    public LiveData<Resource<Void>> sendTicketMessage(String ticketId, TicketMessage message) {
        return repository.sendTicketMessage(ticketId, message);
    }

    public LiveData<Resource<Void>> assignTicket(String ticketId, String staffName, String priority) {
        return repository.assignTicket(ticketId, staffName, priority);
    }

    public LiveData<Resource<Void>> resolveTicket(String ticketId, String resolutionNotes, String adminId) {
        return repository.resolveTicket(ticketId, resolutionNotes, adminId);
    }

    public LiveData<Resource<Void>> reopenTicket(String ticketId, String reopenReason, String userId, String userName) {
        return repository.reopenTicket(ticketId, reopenReason, userId, userName);
    }

    public LiveData<Resource<Void>> closeTicket(String ticketId) {
        return repository.closeTicket(ticketId);
    }
}
