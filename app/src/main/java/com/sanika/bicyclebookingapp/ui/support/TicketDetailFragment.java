package com.sanika.bicyclebookingapp.ui.support;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.databinding.FragmentTicketDetailBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.TicketMessage;
import com.sanika.bicyclebookingapp.ui.support.adapter.TicketMessageAdapter;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.SupportViewModel;

import java.util.Locale;

/**
 * Fragment displaying ticket details, threaded conversation messages,
 * reply input bar, and customer reopening actions.
 */
public class TicketDetailFragment extends Fragment {

    private FragmentTicketDetailBinding binding;
    private SupportViewModel supportViewModel;
    private TicketMessageAdapter messageAdapter;

    private String ticketId;
    private String subject;
    private String category;
    private String priority;
    private String status;
    private String assignedStaff;
    private String bookingId;
    private String bicycleId;
    private long disputedPaise;
    private String resolutionSummary;

    private Uri replyAttachmentUri = null;

    private final ActivityResultLauncher<String> replyImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    replyAttachmentUri = uri;
                    binding.layoutReplyAttachmentPreview.setVisibility(View.VISIBLE);
                    binding.ivReplyAttachmentThumb.setImageURI(uri);
                    binding.tvReplyAttachmentName.setText(uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "photo.jpg");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTicketDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        supportViewModel = new ViewModelProvider(this).get(SupportViewModel.class);

        binding.toolbarTicketDetail.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        extractArguments();
        renderHeader();
        setupRecyclerView();
        setupReplyBar();
        loadMessages();
    }

    private void extractArguments() {
        if (getArguments() != null) {
            ticketId = getArguments().getString("ticketId", "");
            subject = getArguments().getString("subject", "Support Grievance");
            category = getArguments().getString("category", "GENERAL");
            priority = getArguments().getString("priority", "MEDIUM");
            status = getArguments().getString("status", SupportTicket.STATUS_OPEN);
            assignedStaff = getArguments().getString("assignedStaff", "");
            bookingId = getArguments().getString("bookingId", "");
            bicycleId = getArguments().getString("bicycleId", "");
            disputedPaise = getArguments().getLong("disputedPaise", 0);
            resolutionSummary = getArguments().getString("resolutionSummary", "");
        }
    }

    private void renderHeader() {
        String shortId = ticketId.length() > 8 ? ticketId.substring(0, 8) : ticketId;
        binding.tvDetailTicketId.setText("#" + shortId.toUpperCase());
        binding.tvDetailSubject.setText(subject);
        binding.tvDetailCategory.setText(category);
        binding.tvDetailPriority.setText(priority);
        updateStatusBadge(status);

        if (!TextUtils.isEmpty(assignedStaff)) {
            binding.tvDetailStaff.setText("Assigned: " + assignedStaff);
            binding.tvDetailStaff.setVisibility(View.VISIBLE);
        } else {
            binding.tvDetailStaff.setVisibility(View.GONE);
        }

        StringBuilder contextBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(bookingId)) {
            contextBuilder.append("Linked Booking: ").append(bookingId);
        }
        if (!TextUtils.isEmpty(bicycleId)) {
            if (contextBuilder.length() > 0) contextBuilder.append(" • ");
            contextBuilder.append("Bicycle: ").append(bicycleId);
        }
        if (disputedPaise > 0) {
            if (contextBuilder.length() > 0) contextBuilder.append(" • ");
            contextBuilder.append("Disputed: ₹").append(String.format(Locale.ENGLISH, "%.2f", disputedPaise / 100.0));
        }
        if (contextBuilder.length() > 0) {
            binding.tvDetailContext.setText(contextBuilder.toString());
            binding.tvDetailContext.setVisibility(View.VISIBLE);
        } else {
            binding.tvDetailContext.setVisibility(View.GONE);
        }

        // Resolution Summary
        if (SupportTicket.STATUS_RESOLVED.equalsIgnoreCase(status) && !TextUtils.isEmpty(resolutionSummary)) {
            binding.layoutResolutionSummary.setVisibility(View.VISIBLE);
            binding.tvResolutionText.setText(resolutionSummary);
        } else {
            binding.layoutResolutionSummary.setVisibility(View.GONE);
        }

        // Reopen banner if resolved or closed
        boolean canReopen = SupportTicket.STATUS_RESOLVED.equalsIgnoreCase(status)
                || SupportTicket.STATUS_CLOSED.equalsIgnoreCase(status);
        binding.layoutReopenBanner.setVisibility(canReopen ? View.VISIBLE : View.GONE);
        binding.btnReopenTicket.setOnClickListener(v -> showReopenDialog());
    }

    private void updateStatusBadge(String currentStatus) {
        binding.tvDetailStatus.setText(currentStatus.toUpperCase());
        switch (currentStatus.toUpperCase()) {
            case SupportTicket.STATUS_RESOLVED:
                binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                binding.tvDetailStatus.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case SupportTicket.STATUS_CLOSED:
                binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#EEEEEE"));
                binding.tvDetailStatus.setTextColor(Color.parseColor("#757575"));
                break;
            case SupportTicket.STATUS_WAITING_FOR_CUSTOMER:
                binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                binding.tvDetailStatus.setTextColor(Color.parseColor("#1565C0"));
                break;
            case SupportTicket.STATUS_IN_PROGRESS:
            case SupportTicket.STATUS_ASSIGNED:
                binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#FFF8E1"));
                binding.tvDetailStatus.setTextColor(Color.parseColor("#F57F17"));
                break;
            case SupportTicket.STATUS_REOPENED:
                binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                binding.tvDetailStatus.setTextColor(Color.parseColor("#C62828"));
                break;
            default:
                binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                binding.tvDetailStatus.setTextColor(Color.parseColor("#E65100"));
                break;
        }
    }

    private void setupRecyclerView() {
        String uid = FirebaseAuth.getInstance().getUid();
        messageAdapter = new TicketMessageAdapter(uid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvTicketMessages.setLayoutManager(layoutManager);
        binding.rvTicketMessages.setAdapter(messageAdapter);
    }

    private void setupReplyBar() {
        binding.btnAttachReplyPhoto.setOnClickListener(v -> replyImagePickerLauncher.launch("image/*"));

        binding.btnRemoveReplyAttachment.setOnClickListener(v -> {
            replyAttachmentUri = null;
            binding.layoutReplyAttachmentPreview.setVisibility(View.GONE);
            binding.ivReplyAttachmentThumb.setImageURI(null);
        });

        binding.btnSendReply.setOnClickListener(v -> sendReply());
    }

    private void loadMessages() {
        if (TextUtils.isEmpty(ticketId)) return;

        binding.progressBarMessages.setVisibility(View.VISIBLE);
        supportViewModel.getTicketMessages(ticketId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarMessages.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                messageAdapter.setMessages(resource.data);
                if (!resource.data.isEmpty()) {
                    binding.rvTicketMessages.scrollToPosition(resource.data.size() - 1);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Failed to load messages: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendReply() {
        String replyText = binding.etReplyMessage.getText() != null ? binding.etReplyMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(replyText) && replyAttachmentUri == null) {
            Toast.makeText(requireContext(), "Please enter a reply or attach a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to reply", Toast.LENGTH_SHORT).show();
            return;
        }

        TicketMessage msg = new TicketMessage(
                ticketId,
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Customer",
                TicketMessage.SENDER_CUSTOMER,
                replyText
        );

        if (replyAttachmentUri != null) {
            msg.setAttachmentUrl(replyAttachmentUri.toString());
        }

        binding.btnSendReply.setEnabled(false);

        supportViewModel.sendTicketMessage(ticketId, msg).observe(getViewLifecycleOwner(), res -> {
            if (binding == null) return;
            binding.btnSendReply.setEnabled(true);

            if (res.status == Resource.Status.SUCCESS) {
                binding.etReplyMessage.setText("");
                replyAttachmentUri = null;
                binding.layoutReplyAttachmentPreview.setVisibility(View.GONE);
                this.status = SupportTicket.STATUS_IN_PROGRESS;
                updateStatusBadge(this.status);
            } else if (res.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Failed to send: " + res.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReopenDialog() {
        EditText etReason = new EditText(requireContext());
        etReason.setHint("Why is this issue unresolved? (e.g. Refund not received in bank)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Reopen Ticket #" + (ticketId.length() > 8 ? ticketId.substring(0, 8) : ticketId).toUpperCase())
                .setMessage("Please provide the reason for reopening this grievance:")
                .setView(etReason)
                .setPositiveButton("Reopen Ticket", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (TextUtils.isEmpty(reason)) {
                        reason = "Customer requested reconsideration of resolution.";
                    }

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    String uid = user != null ? user.getUid() : "CUSTOMER";
                    String name = user != null && user.getDisplayName() != null ? user.getDisplayName() : "Customer";

                    supportViewModel.reopenTicket(ticketId, reason, uid, name).observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Ticket reopened! Our supervisor will review promptly.", Toast.LENGTH_LONG).show();
                            this.status = SupportTicket.STATUS_REOPENED;
                            updateStatusBadge(this.status);
                            binding.layoutReopenBanner.setVisibility(View.GONE);
                        } else if (res.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Failed to reopen: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
