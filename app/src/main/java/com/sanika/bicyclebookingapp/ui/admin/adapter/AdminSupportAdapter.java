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
 * Adapter for Admin Support & Grievances.
 */
public class AdminSupportAdapter extends RecyclerView.Adapter<AdminSupportAdapter.ViewHolder> {

    public interface OnTicketActionListener {
        void onResolve(SupportTicket ticket);
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

            String userInfo = "Reported by " + (ticket.getUserName() != null ? ticket.getUserName() : "User")
                    + (ticket.getUserPhone() != null ? " • " + ticket.getUserPhone() : "");
            binding.tvTicketUser.setText(userInfo);

            String status = ticket.getStatus() != null ? ticket.getStatus().toUpperCase() : SupportTicket.STATUS_OPEN;
            binding.tvTicketStatus.setText(status);

            if (SupportTicket.STATUS_RESOLVED.equalsIgnoreCase(status)) {
                binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                binding.tvTicketStatus.setTextColor(Color.parseColor("#2E7D32"));
                binding.btnResolveTicket.setVisibility(View.GONE);
                if (ticket.getAdminNotes() != null && !ticket.getAdminNotes().isEmpty()) {
                    binding.tvTicketNotes.setVisibility(View.VISIBLE);
                    binding.tvTicketNotes.setText("Resolution: " + ticket.getAdminNotes());
                } else {
                    binding.tvTicketNotes.setVisibility(View.GONE);
                }
            } else {
                binding.tvTicketStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                binding.tvTicketStatus.setTextColor(Color.parseColor("#E65100"));
                binding.btnResolveTicket.setVisibility(View.VISIBLE);
                binding.tvTicketNotes.setVisibility(View.GONE);
            }

            if (ticket.getCreatedAt() != null) {
                binding.tvTicketDate.setText(dateFormat.format(ticket.getCreatedAt()));
            } else {
                binding.tvTicketDate.setText("Recent");
            }

            binding.btnResolveTicket.setOnClickListener(v -> {
                if (listener != null) listener.onResolve(ticket);
            });
        }
    }
}
