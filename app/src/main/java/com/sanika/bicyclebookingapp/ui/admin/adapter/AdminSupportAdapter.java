package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAdminSupportBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Admin Support Grievances & Complaints triage.
 */
public class AdminSupportAdapter extends RecyclerView.Adapter<AdminSupportAdapter.ViewHolder> {

    public interface OnTicketActionListener {
        void onAction(SupportTicket ticket);
    }

    private final List<SupportTicket> tickets = new ArrayList<>();
    private final OnTicketActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);

    public AdminSupportAdapter(OnTicketActionListener listener) {
        this.listener = listener;
    }

    public void setTickets(List<SupportTicket> newTickets) {
        this.tickets.clear();
        if (newTickets != null) {
            this.tickets.addAll(newTickets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminSupportBinding binding = ItemAdminSupportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(tickets.get(position));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminSupportBinding binding;

        ViewHolder(ItemAdminSupportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SupportTicket ticket) {
            binding.tvTicketSubject.setText(ticket.getSubject() != null ? ticket.getSubject() : "Support Grievance");
            binding.tvTicketDescription.setText(ticket.getDescription() != null ? ticket.getDescription() : "");
            binding.tvTicketCategory.setText(ticket.getCategory() != null ? ticket.getCategory() : "GENERAL");
            binding.tvTicketPriority.setText(ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM");

            String userInfo = "Reported by " + (ticket.getUserName() != null ? ticket.getUserName() : "User")
                    + (ticket.getUserPhone() != null && !ticket.getUserPhone().isEmpty() ? " • " + ticket.getUserPhone() : "");
            binding.tvTicketUser.setText(userInfo);

            // Context (Booking ID / Disputed amount)
            StringBuilder contextStr = new StringBuilder();
            if (ticket.getBookingId() != null && !ticket.getBookingId().isEmpty()) {
                contextStr.append("Linked Booking: ").append(ticket.getBookingId());
            }
            if (ticket.getDisputedAmountPaise() > 0) {
                if (contextStr.length() > 0) contextStr.append(" • ");
                contextStr.append("Disputed: ₹").append(String.format(Locale.ENGLISH, "%.2f", ticket.getDisputedAmountPaise() / 100.0));
            }
            if (contextStr.length() > 0) {
                binding.tvTicketContext.setVisibility(View.VISIBLE);
                binding.tvTicketContext.setText(contextStr.toString());
            } else {
                binding.tvTicketContext.setVisibility(View.GONE);
            }

            // Priority badge styling
            String priority = ticket.getPriority() != null ? ticket.getPriority().toUpperCase() : SupportTicket.PRIORITY_MEDIUM;
            if (SupportTicket.PRIORITY_URGENT.equalsIgnoreCase(priority)) {
                binding.tvTicketPriority.setBackgroundColor(Color.parseColor("#FFEBEE"));
                binding.tvTicketPriority.setTextColor(Color.parseColor("#C62828"));
            } else if (SupportTicket.PRIORITY_HIGH.equalsIgnoreCase(priority)) {
                binding.tvTicketPriority.setBackgroundColor(Color.parseColor("#FFF3E0"));
                binding.tvTicketPriority.setTextColor(Color.parseColor("#E65100"));
            } else {
                binding.tvTicketPriority.setBackgroundColor(Color.parseColor("#F5F5F5"));
                binding.tvTicketPriority.setTextColor(Color.parseColor("#616161"));
            }

            // Status badge styling
            String status = ticket.getStatus() != null ? ticket.getStatus().toUpperCase() : SupportTicket.STATUS_OPEN;
            binding.tvTicketStatus.setText(status);

            switch (status) {
                case SupportTicket.STATUS_RESOLVED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#2E7D32"));
                    if (ticket.getAdminNotes() != null && !ticket.getAdminNotes().isEmpty()) {
                        binding.tvTicketNotes.setVisibility(View.VISIBLE);
                        binding.tvTicketNotes.setText("Resolution: " + ticket.getAdminNotes());
                    } else {
                        binding.tvTicketNotes.setVisibility(View.GONE);
                    }
                    break;
                case SupportTicket.STATUS_CLOSED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#EEEEEE"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#757575"));
                    binding.tvTicketNotes.setVisibility(View.GONE);
                    break;
                case SupportTicket.STATUS_WAITING_FOR_CUSTOMER:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#1565C0"));
                    binding.tvTicketNotes.setVisibility(View.GONE);
                    break;
                case SupportTicket.STATUS_IN_PROGRESS:
                case SupportTicket.STATUS_ASSIGNED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#FFF8E1"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#F57F17"));
                    binding.tvTicketNotes.setVisibility(View.GONE);
                    break;
                case SupportTicket.STATUS_REOPENED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#FFCDD2"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#B71C1C"));
                    binding.tvTicketNotes.setVisibility(View.GONE);
                    break;
                default:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#E65100"));
                    binding.tvTicketNotes.setVisibility(View.GONE);
                    break;
            }

            if (ticket.getAssignedStaff() != null && !ticket.getAssignedStaff().isEmpty()) {
                binding.tvTicketStaff.setText("Assigned Staff: " + ticket.getAssignedStaff());
                binding.tvTicketStaff.setVisibility(View.VISIBLE);
            } else {
                binding.tvTicketStaff.setVisibility(View.GONE);
            }

            if (ticket.getCreatedAt() != null) {
                binding.tvTicketDate.setText(dateFormat.format(ticket.getCreatedAt()));
            } else {
                binding.tvTicketDate.setText("");
            }

            binding.btnResolveTicket.setOnClickListener(v -> {
                if (listener != null) listener.onAction(ticket);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onAction(ticket);
            });
        }
    }
}
