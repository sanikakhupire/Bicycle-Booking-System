package com.sanika.bicyclebookingapp.ui.support.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemCustomerTicketBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter displaying customer grievances, categories, and real-time status.
 */
public class CustomerTicketAdapter extends RecyclerView.Adapter<CustomerTicketAdapter.ViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(SupportTicket ticket);
    }

    private final List<SupportTicket> tickets = new ArrayList<>();
    private final OnTicketClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);

    public CustomerTicketAdapter(OnTicketClickListener listener) {
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
        ItemCustomerTicketBinding binding = ItemCustomerTicketBinding.inflate(
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
        private final ItemCustomerTicketBinding binding;

        ViewHolder(ItemCustomerTicketBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SupportTicket ticket) {
            String shortId = ticket.getTicketId() != null
                    ? (ticket.getTicketId().length() > 8 ? ticket.getTicketId().substring(0, 8) : ticket.getTicketId())
                    : "TICKET";
            binding.tvTicketId.setText("#" + shortId.toUpperCase());

            binding.tvTicketSubject.setText(ticket.getSubject() != null ? ticket.getSubject() : "Support Grievance");
            binding.tvTicketDescription.setText(ticket.getDescription() != null ? ticket.getDescription() : "");
            binding.tvTicketCategory.setText(ticket.getCategory() != null ? ticket.getCategory() : "GENERAL");
            binding.tvTicketPriority.setText(ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM");

            // Format Status & Color Badge
            String status = ticket.getStatus() != null ? ticket.getStatus().toUpperCase() : SupportTicket.STATUS_OPEN;
            binding.tvTicketStatus.setText(status);

            switch (status) {
                case SupportTicket.STATUS_RESOLVED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#2E7D32"));
                    break;
                case SupportTicket.STATUS_CLOSED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#EEEEEE"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#757575"));
                    break;
                case SupportTicket.STATUS_WAITING_FOR_CUSTOMER:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#1565C0"));
                    break;
                case SupportTicket.STATUS_IN_PROGRESS:
                case SupportTicket.STATUS_ASSIGNED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#FFF8E1"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#F57F17"));
                    break;
                case SupportTicket.STATUS_REOPENED:
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#C62828"));
                    break;
                default: // OPEN
                    binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#E65100"));
                    break;
            }

            // Context (Booking ID / Amount)
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

            // Date & Staff
            if (ticket.getCreatedAt() != null) {
                binding.tvTicketDate.setText(dateFormat.format(ticket.getCreatedAt()));
            } else {
                binding.tvTicketDate.setText("");
            }

            if (ticket.getAssignedStaff() != null && !ticket.getAssignedStaff().isEmpty()) {
                binding.tvAssignedStaff.setText("Assigned: " + ticket.getAssignedStaff());
                binding.tvAssignedStaff.setVisibility(View.VISIBLE);
            } else {
                binding.tvAssignedStaff.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTicketClick(ticket);
                }
            });
        }
    }
}
