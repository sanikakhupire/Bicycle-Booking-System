package com.sanika.bicyclebookingapp.ui.admin;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.DialogAdminTicketActionBinding;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminSupportBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.TicketMessage;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminSupportAdapter;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.SupportViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Support Grievances & Complaints Management screen for Administrators.
 * Provides real-time search, multi-factor filtering, staff assignment, customer messaging,
 * ticket escalation, and resolution tracking.
 */
public class AdminSupportFragment extends Fragment {

    private FragmentAdminSupportBinding binding;
    private SupportViewModel supportViewModel;
    private AdminSupportAdapter adapter;

    private List<SupportTicket> allTickets = new ArrayList<>();
    private String currentStatusFilter = "ALL";
    private String currentPriorityFilter = "ALL";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminSupportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        supportViewModel = new ViewModelProvider(this).get(SupportViewModel.class);

        setupRecyclerView();
        setupFiltersAndSearch();
        loadTickets();
    }

    private void setupRecyclerView() {
        adapter = new AdminSupportAdapter(this::showAdminTicketActionDialog);
        binding.rvSupport.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSupport.setAdapter(adapter);
    }

    private void setupFiltersAndSearch() {
        // Search text watcher
        binding.etSearchSupport.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase(Locale.US) : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Status Chips
        binding.chipGroupSupportStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipSupportOpen) {
                currentStatusFilter = SupportTicket.STATUS_OPEN;
            } else if (checkedId == R.id.chipSupportAssigned) {
                currentStatusFilter = SupportTicket.STATUS_ASSIGNED;
            } else if (checkedId == R.id.chipSupportInProgress) {
                currentStatusFilter = SupportTicket.STATUS_IN_PROGRESS;
            } else if (checkedId == R.id.chipSupportWaiting) {
                currentStatusFilter = SupportTicket.STATUS_WAITING_FOR_CUSTOMER;
            } else if (checkedId == R.id.chipSupportReopened) {
                currentStatusFilter = SupportTicket.STATUS_REOPENED;
            } else if (checkedId == R.id.chipSupportResolved) {
                currentStatusFilter = SupportTicket.STATUS_RESOLVED;
            } else if (checkedId == R.id.chipSupportClosed) {
                currentStatusFilter = SupportTicket.STATUS_CLOSED;
            } else {
                currentStatusFilter = "ALL";
            }
            applyFilters();
        });

        // Priority Chips
        binding.chipGroupSupportPriority.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPriorityUrgent) {
                currentPriorityFilter = SupportTicket.PRIORITY_URGENT;
            } else if (checkedId == R.id.chipPriorityHigh) {
                currentPriorityFilter = SupportTicket.PRIORITY_HIGH;
            } else if (checkedId == R.id.chipPriorityMedium) {
                currentPriorityFilter = SupportTicket.PRIORITY_MEDIUM;
            } else if (checkedId == R.id.chipPriorityLow) {
                currentPriorityFilter = SupportTicket.PRIORITY_LOW;
            } else {
                currentPriorityFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void loadTickets() {
        binding.progressBarSupport.setVisibility(View.VISIBLE);

        supportViewModel.getAllTickets("ALL").observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarSupport.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                allTickets = resource.data;
                applyFilters();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        List<SupportTicket> filtered = new ArrayList<>();

        for (SupportTicket t : allTickets) {
            String status = t.getStatus() != null ? t.getStatus().toUpperCase() : SupportTicket.STATUS_OPEN;
            String priority = t.getPriority() != null ? t.getPriority().toUpperCase() : SupportTicket.PRIORITY_MEDIUM;

            boolean statusMatches = "ALL".equalsIgnoreCase(currentStatusFilter) || currentStatusFilter.equalsIgnoreCase(status);
            boolean priorityMatches = "ALL".equalsIgnoreCase(currentPriorityFilter) || currentPriorityFilter.equalsIgnoreCase(priority);

            boolean searchMatches = true;
            if (!searchQuery.isEmpty()) {
                String id = t.getTicketId() != null ? t.getTicketId().toLowerCase(Locale.US) : "";
                String user = t.getUserName() != null ? t.getUserName().toLowerCase(Locale.US) : "";
                String phone = t.getUserPhone() != null ? t.getUserPhone().toLowerCase(Locale.US) : "";
                String subject = t.getSubject() != null ? t.getSubject().toLowerCase(Locale.US) : "";
                String desc = t.getDescription() != null ? t.getDescription().toLowerCase(Locale.US) : "";

                searchMatches = id.contains(searchQuery)
                        || user.contains(searchQuery)
                        || phone.contains(searchQuery)
                        || subject.contains(searchQuery)
                        || desc.contains(searchQuery);
            }

            if (statusMatches && priorityMatches && searchMatches) {
                filtered.add(t);
            }
        }

        adapter.setTickets(filtered);
        binding.tvEmptySupport.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAdminTicketActionDialog(SupportTicket ticket) {
        DialogAdminTicketActionBinding dialogBinding = DialogAdminTicketActionBinding.inflate(
                LayoutInflater.from(requireContext())
        );

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        // Populate details
        String shortId = ticket.getTicketId() != null
                ? (ticket.getTicketId().length() > 8 ? ticket.getTicketId().substring(0, 8) : ticket.getTicketId())
                : "TICKET";
        dialogBinding.tvAdminDialogTicketId.setText("#" + shortId.toUpperCase());
        dialogBinding.tvAdminDialogSubject.setText(ticket.getSubject());
        dialogBinding.tvAdminDialogCustomer.setText("Customer: " + (ticket.getUserName() != null ? ticket.getUserName() : "User")
                + (ticket.getUserPhone() != null && !ticket.getUserPhone().isEmpty() ? " • " + ticket.getUserPhone() : ""));
        dialogBinding.tvAdminDialogCategory.setText(ticket.getCategory() != null ? ticket.getCategory() : "GENERAL");
        dialogBinding.tvAdminDialogPriority.setText(ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM");
        dialogBinding.tvAdminDialogStatus.setText(ticket.getStatus() != null ? ticket.getStatus().toUpperCase() : "OPEN");
        dialogBinding.tvAdminDialogDescription.setText(ticket.getDescription() != null ? ticket.getDescription() : "");

        if (!TextUtils.isEmpty(ticket.getAssignedStaff())) {
            dialogBinding.etAssignStaff.setText(ticket.getAssignedStaff());
        }

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
            dialogBinding.tvAdminDialogContext.setVisibility(View.VISIBLE);
            dialogBinding.tvAdminDialogContext.setText(contextStr.toString());
        } else {
            dialogBinding.tvAdminDialogContext.setVisibility(View.GONE);
        }

        // Attachment if available
        if (ticket.getAttachmentUrl() != null && !ticket.getAttachmentUrl().isEmpty()) {
            dialogBinding.ivAdminDialogAttachment.setVisibility(View.VISIBLE);
            try {
                dialogBinding.ivAdminDialogAttachment.setImageURI(Uri.parse(ticket.getAttachmentUrl()));
            } catch (Exception ignored) {
                dialogBinding.ivAdminDialogAttachment.setImageResource(R.drawable.ic_bicycle);
            }
        } else {
            dialogBinding.ivAdminDialogAttachment.setVisibility(View.GONE);
        }

        // Action 1: Assign Staff
        dialogBinding.btnAssignStaff.setOnClickListener(v -> {
            String staff = dialogBinding.etAssignStaff.getText().toString().trim();
            if (TextUtils.isEmpty(staff)) {
                dialogBinding.etAssignStaff.setError("Enter staff name");
                return;
            }
            supportViewModel.assignTicket(ticket.getTicketId(), staff, ticket.getPriority())
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Ticket assigned to " + staff, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else if (res.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Assignment failed: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Action 2: Send Admin Reply
        dialogBinding.btnSendAdminReply.setOnClickListener(v -> {
            String reply = dialogBinding.etAdminReply.getText().toString().trim();
            if (TextUtils.isEmpty(reply)) {
                dialogBinding.etAdminReply.setError("Enter reply message");
                return;
            }
            TicketMessage msg = new TicketMessage(
                    ticket.getTicketId(),
                    FirebaseAuth.getInstance().getUid(),
                    "Admin Operations",
                    TicketMessage.SENDER_ADMIN,
                    reply
            );
            supportViewModel.sendTicketMessage(ticket.getTicketId(), msg)
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Reply dispatched to customer!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else if (res.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Failed to send: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Action 3: Escalate Ticket
        dialogBinding.btnEscalateTicket.setOnClickListener(v -> {
            supportViewModel.assignTicket(ticket.getTicketId(),
                    ticket.getAssignedStaff() != null ? ticket.getAssignedStaff() : "Supervisor Escalation",
                    SupportTicket.PRIORITY_URGENT)
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Ticket escalated to URGENT priority!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else if (res.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Escalation failed: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Action 4: Resolve Ticket
        dialogBinding.btnResolveDialogTicket.setOnClickListener(v -> {
            String notes = dialogBinding.etResolutionNotes.getText().toString().trim();
            if (TextUtils.isEmpty(notes)) {
                notes = "Resolved by Operations Administrator.";
            }
            String adminUid = FirebaseAuth.getInstance().getUid();
            supportViewModel.resolveTicket(ticket.getTicketId(), notes, adminUid != null ? adminUid : "ADMIN")
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Grievance resolved successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else if (res.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Resolution failed: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Action 5: Close Ticket
        dialogBinding.btnCloseDialogTicket.setOnClickListener(v -> {
            supportViewModel.closeTicket(ticket.getTicketId())
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Ticket closed.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else if (res.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Failed: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
