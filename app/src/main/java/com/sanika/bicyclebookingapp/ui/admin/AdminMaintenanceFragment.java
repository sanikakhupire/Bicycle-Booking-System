package com.sanika.bicyclebookingapp.ui.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.DialogAdminMaintenanceActionBinding;
import com.sanika.bicyclebookingapp.databinding.DialogBicycleMaintenanceHistoryBinding;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminMaintenanceBinding;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminMaintenanceAdapter;
import com.sanika.bicyclebookingapp.viewmodel.MaintenanceViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for monitoring, diagnosing, triaging, repairing, and verifying
 * bicycles in the maintenance and quality workflow.
 */
public class AdminMaintenanceFragment extends Fragment implements AdminMaintenanceAdapter.OnMaintenanceActionListener {

    private FragmentAdminMaintenanceBinding binding;
    private MaintenanceViewModel maintenanceViewModel;
    private AdminMaintenanceAdapter adapter;

    private final List<MaintenanceRecord> fullList = new ArrayList<>();
    private String currentStatusFilter = "ALL";
    private String currentSearchQuery = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminMaintenanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        maintenanceViewModel = new ViewModelProvider(this).get(MaintenanceViewModel.class);

        setupRecyclerView();
        setupFilters();
        setupSearch();
        loadData();
    }

    private void setupRecyclerView() {
        adapter = new AdminMaintenanceAdapter(this);
        binding.rvMaintenance.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMaintenance.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipReported) {
                currentStatusFilter = MaintenanceRecord.STATUS_REPORTED;
            } else if (checkedId == R.id.chipAssigned) {
                currentStatusFilter = MaintenanceRecord.STATUS_ASSIGNED;
            } else if (checkedId == R.id.chipInProgress) {
                currentStatusFilter = MaintenanceRecord.STATUS_IN_PROGRESS;
            } else if (checkedId == R.id.chipCompleted) {
                currentStatusFilter = MaintenanceRecord.STATUS_COMPLETED;
            } else if (checkedId == R.id.chipVerified) {
                currentStatusFilter = MaintenanceRecord.STATUS_VERIFIED;
            } else if (checkedId == R.id.chipRejected) {
                currentStatusFilter = MaintenanceRecord.STATUS_REJECTED;
            } else {
                currentStatusFilter = "ALL";
            }
            loadData();
        });
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query != null ? query.trim() : "";
                applySearchFilter();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText != null ? newText.trim() : "";
                applySearchFilter();
                return true;
            }
        });
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        maintenanceViewModel.getMaintenanceRecords(currentStatusFilter).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);

            if (resource.isSuccess() && resource.data != null) {
                fullList.clear();
                fullList.addAll(resource.data);
                applySearchFilter();
            } else if (resource.isError()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.tvEmpty.setText("Failed to load records: " + resource.message);
            }
        });
    }

    private void applySearchFilter() {
        if (fullList.isEmpty()) {
            adapter.submitList(new ArrayList<>());
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (currentSearchQuery.isEmpty()) {
            adapter.submitList(new ArrayList<>(fullList));
            binding.tvEmpty.setVisibility(View.GONE);
            return;
        }

        String q = currentSearchQuery.toLowerCase();
        List<MaintenanceRecord> filtered = new ArrayList<>();
        for (MaintenanceRecord r : fullList) {
            boolean matchBike = r.getBicycleId() != null && r.getBicycleId().toLowerCase().contains(q);
            boolean matchStaff = r.getAssignedStaff() != null && r.getAssignedStaff().toLowerCase().contains(q);
            boolean matchCat = r.getDamageCategory() != null && r.getDamageCategory().toLowerCase().contains(q);
            boolean matchReporter = r.getReporterName() != null && r.getReporterName().toLowerCase().contains(q);
            boolean matchDesc = r.getDescription() != null && r.getDescription().toLowerCase().contains(q);

            if (matchBike || matchStaff || matchCat || matchReporter || matchDesc) {
                filtered.add(r);
            }
        }

        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onManageAction(MaintenanceRecord record) {
        Dialog dialog = new Dialog(requireContext());
        DialogAdminMaintenanceActionBinding actionBinding =
                DialogAdminMaintenanceActionBinding.inflate(getLayoutInflater());
        dialog.setContentView(actionBinding.getRoot());
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        actionBinding.tvDialogSubtitle.setText(
                "Bicycle: " + record.getBicycleId() + " • Status: " + record.getStatus());

        // Prepopulate Priority Spinner
        List<String> priorities = List.of(
                MaintenanceRecord.PRIORITY_LOW,
                MaintenanceRecord.PRIORITY_MEDIUM,
                MaintenanceRecord.PRIORITY_HIGH,
                MaintenanceRecord.PRIORITY_CRITICAL
        );
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities);
        actionBinding.spinnerPriority.setAdapter(priorityAdapter);
        if (record.getPriority() != null) {
            int pos = priorities.indexOf(record.getPriority().toUpperCase());
            if (pos >= 0) actionBinding.spinnerPriority.setSelection(pos);
        }

        // Prepopulate text fields if existing
        if (record.getAssignedStaff() != null) {
            actionBinding.etAssignedStaff.setText(record.getAssignedStaff());
        }
        if (record.getDiagnosisNotes() != null) {
            actionBinding.etDiagnosis.setText(record.getDiagnosisNotes());
        }
        if (record.getPartsReplaced() != null) {
            actionBinding.etPartsReplaced.setText(record.getPartsReplaced());
        }
        if (record.getRepairNotes() != null) {
            actionBinding.etRepairNotes.setText(record.getRepairNotes());
        }
        if (record.getCostPaise() > 0) {
            actionBinding.etCostInr.setText(String.format(java.util.Locale.ENGLISH, "%.2f", record.getCostInr()));
        }
        if (record.getVerificationNotes() != null) {
            actionBinding.etVerificationNotes.setText(record.getVerificationNotes());
        }

        // 1. Save Staff Assignment
        actionBinding.btnSaveAssignment.setOnClickListener(v -> {
            String staff = actionBinding.etAssignedStaff.getText() != null
                    ? actionBinding.etAssignedStaff.getText().toString().trim() : "";
            if (staff.isEmpty()) {
                actionBinding.tilAssignedStaff.setError("Please enter staff / mechanic name");
                return;
            }
            actionBinding.tilAssignedStaff.setError(null);
            String selectedPriority = (String) actionBinding.spinnerPriority.getSelectedItem();

            maintenanceViewModel.assignStaffAndPriority(record.getRecordId(), staff, selectedPriority)
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.isSuccess()) {
                            Toast.makeText(requireContext(), "Staff assigned successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadData();
                        } else if (res.isError()) {
                            Toast.makeText(requireContext(), "Error: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 2. Save Diagnosis
        actionBinding.btnSaveDiagnosis.setOnClickListener(v -> {
            String diagnosis = actionBinding.etDiagnosis.getText() != null
                    ? actionBinding.etDiagnosis.getText().toString().trim() : "";
            if (diagnosis.isEmpty()) {
                actionBinding.tilDiagnosis.setError("Please enter diagnosis notes");
                return;
            }
            actionBinding.tilDiagnosis.setError(null);

            maintenanceViewModel.startDiagnosisAndRepair(record.getRecordId(), diagnosis)
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.isSuccess()) {
                            Toast.makeText(requireContext(), "Diagnosis recorded. Status set to IN_PROGRESS", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadData();
                        } else if (res.isError()) {
                            Toast.makeText(requireContext(), "Error: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 3. Complete Repair (Parts & INR cost)
        actionBinding.btnCompleteRepair.setOnClickListener(v -> {
            String parts = actionBinding.etPartsReplaced.getText() != null
                    ? actionBinding.etPartsReplaced.getText().toString().trim() : "";
            String notes = actionBinding.etRepairNotes.getText() != null
                    ? actionBinding.etRepairNotes.getText().toString().trim() : "";
            String costStr = actionBinding.etCostInr.getText() != null
                    ? actionBinding.etCostInr.getText().toString().trim() : "0";

            double costInr = 0.0;
            try {
                if (!costStr.isEmpty()) costInr = Double.parseDouble(costStr);
            } catch (NumberFormatException ignored) {}

            long costPaise = Math.round(costInr * 100.0);

            maintenanceViewModel.completeRepair(record.getRecordId(), notes, parts, costPaise, null, null)
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.isSuccess()) {
                            Toast.makeText(requireContext(), "Repair details saved. Ready for quality verification.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadData();
                        } else if (res.isError()) {
                            Toast.makeText(requireContext(), "Error: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 4. Verify & Restore to Service
        actionBinding.btnVerifyAndRestore.setOnClickListener(v -> {
            String verifyNotes = actionBinding.etVerificationNotes.getText() != null
                    ? actionBinding.etVerificationNotes.getText().toString().trim() : "Condition checked and approved for passenger service.";

            new AlertDialog.Builder(requireContext())
                    .setTitle("Restore Bicycle to Service")
                    .setMessage("Are you sure you want to verify bicycle " + record.getBicycleId()
                            + " and restore its status to AVAILABLE in inventory?\n\nThis will allow customers to book it immediately.")
                    .setPositiveButton("Verify & Restore", (d, w) -> {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        String adminId = user != null ? user.getUid() : "admin";
                        String adminName = user != null && user.getDisplayName() != null ? user.getDisplayName() : "Administrator";

                        maintenanceViewModel.verifyAndRestoreBicycle(
                                record.getRecordId(), record.getBicycleId(), adminId, adminName, verifyNotes)
                                .observe(getViewLifecycleOwner(), res -> {
                                    if (res.isSuccess()) {
                                        Toast.makeText(requireContext(), "Bicycle successfully restored to AVAILABLE status!", Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                        loadData();
                                    } else if (res.isError()) {
                                        Toast.makeText(requireContext(), "Error: " + res.message, Toast.LENGTH_LONG).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // 5. Reject Report
        actionBinding.btnRejectReport.setOnClickListener(v -> {
            EditText etReason = new EditText(requireContext());
            etReason.setHint("Reason for rejection (e.g. False alarm)");

            new AlertDialog.Builder(requireContext())
                    .setTitle("Reject Maintenance Report")
                    .setView(etReason)
                    .setPositiveButton("Reject", (d, w) -> {
                        String reason = etReason.getText().toString().trim();
                        maintenanceViewModel.rejectReport(record.getRecordId(), record.getBicycleId(), reason)
                                .observe(getViewLifecycleOwner(), res -> {
                                    if (res.isSuccess()) {
                                        Toast.makeText(requireContext(), "Report rejected", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        loadData();
                                    } else if (res.isError()) {
                                        Toast.makeText(requireContext(), "Error: " + res.message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    @Override
    public void onViewBikeHistory(String bicycleId) {
        Dialog historyDialog = new Dialog(requireContext());
        DialogBicycleMaintenanceHistoryBinding historyBinding =
                DialogBicycleMaintenanceHistoryBinding.inflate(getLayoutInflater());
        historyDialog.setContentView(historyBinding.getRoot());
        historyDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        historyBinding.tvHistoryTitle.setText("Maintenance History: " + bicycleId);
        historyBinding.rvBicycleHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        AdminMaintenanceAdapter historyAdapter = new AdminMaintenanceAdapter(null);
        historyBinding.rvBicycleHistory.setAdapter(historyAdapter);

        historyBinding.historyProgressBar.setVisibility(View.VISIBLE);
        maintenanceViewModel.getMaintenanceHistoryForBicycle(bicycleId).observe(getViewLifecycleOwner(), res -> {
            historyBinding.historyProgressBar.setVisibility(View.GONE);
            if (res.isSuccess() && res.data != null) {
                historyAdapter.submitList(res.data);
                historyBinding.tvHistoryEmpty.setVisibility(res.data.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                historyBinding.tvHistoryEmpty.setVisibility(View.VISIBLE);
                historyBinding.tvHistoryEmpty.setText("No history records.");
            }
        });

        historyBinding.btnCloseHistory.setOnClickListener(v -> historyDialog.dismiss());
        historyDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
