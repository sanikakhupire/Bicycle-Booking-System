package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminSupportBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminSupportAdapter;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Support Grievances & Complaints Management screen for Administrators.
 */
public class AdminSupportFragment extends Fragment {

    private FragmentAdminSupportBinding binding;
    private AdminViewModel adminViewModel;
    private AdminSupportAdapter adapter;

    private List<SupportTicket> allTickets = new ArrayList<>();
    private String currentStatusFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminSupportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupRecyclerView();
        setupFilters();
        loadTickets();
    }

    private void setupRecyclerView() {
        adapter = new AdminSupportAdapter(this::showResolveTicketDialog);
        binding.rvSupport.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSupport.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupSupportStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipSupportOpen) {
                currentStatusFilter = SupportTicket.STATUS_OPEN;
            } else if (checkedId == R.id.chipSupportResolved) {
                currentStatusFilter = SupportTicket.STATUS_RESOLVED;
            } else {
                currentStatusFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void loadTickets() {
        binding.progressBarSupport.setVisibility(View.VISIBLE);

        adminViewModel.getSupportTickets().observe(getViewLifecycleOwner(), resource -> {
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

            boolean matches;
            if ("ALL".equals(currentStatusFilter)) {
                matches = true;
            } else if (SupportTicket.STATUS_OPEN.equals(currentStatusFilter)) {
                matches = SupportTicket.STATUS_OPEN.equalsIgnoreCase(status) || SupportTicket.STATUS_IN_PROGRESS.equalsIgnoreCase(status);
            } else {
                matches = currentStatusFilter.equalsIgnoreCase(status);
            }

            if (matches) {
                filtered.add(t);
            }
        }

        adapter.setTickets(filtered);
        binding.tvEmptySupport.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showResolveTicketDialog(SupportTicket ticket) {
        EditText etNotes = new EditText(requireContext());
        etNotes.setHint("Action taken / resolution notes (e.g. Dispatched cycle handler)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Resolve Grievance #" + (ticket.getTicketId() != null ? ticket.getTicketId() : ""))
                .setMessage("Enter the operational resolution notes for this customer issue:")
                .setView(etNotes)
                .setPositiveButton("Mark as Resolved", (dialog, which) -> {
                    String notes = etNotes.getText().toString().trim();
                    if (TextUtils.isEmpty(notes)) {
                        notes = "Resolved by Operations Administrator.";
                    }

                    adminViewModel.resolveSupportTicket(ticket.getTicketId(), notes,
                            () -> Toast.makeText(requireContext(), "Ticket marked as resolved!", Toast.LENGTH_SHORT).show(),
                            err -> Toast.makeText(requireContext(), "Failed: " + err, Toast.LENGTH_LONG).show());
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
