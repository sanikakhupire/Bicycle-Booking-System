package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.text.Editable;
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

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminCustomersBinding;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminCustomerAdapter;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer Management screen for Administrators.
 * Search, filter, and role assignment.
 */
public class AdminCustomersFragment extends Fragment {

    private FragmentAdminCustomersBinding binding;
    private AdminViewModel adminViewModel;
    private AdminCustomerAdapter adapter;

    private List<User> allUsers = new ArrayList<>();
    private String currentRoleFilter = "ALL";
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminCustomersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupRecyclerView();
        setupSearchAndFilters();
        loadCustomers();
    }

    private void setupRecyclerView() {
        adapter = new AdminCustomerAdapter(this::showChangeRoleDialog);
        binding.rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomers.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
        binding.etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.chipGroupCustomerRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRoleCustomer) {
                currentRoleFilter = "USER";
            } else if (checkedId == R.id.chipRoleHandler) {
                currentRoleFilter = "HANDLER";
            } else if (checkedId == R.id.chipRoleAdmin) {
                currentRoleFilter = "ADMIN";
            } else {
                currentRoleFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void loadCustomers() {
        binding.progressBarCustomers.setVisibility(View.VISIBLE);

        adminViewModel.getAllCustomers().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarCustomers.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                allUsers = resource.data;
                applyFilters();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        List<User> filtered = new ArrayList<>();

        for (User u : allUsers) {
            String role = u.getRole() != null ? u.getRole().toUpperCase() : (u.isAdmin() ? "ADMIN" : "USER");

            boolean matchesRole = "ALL".equals(currentRoleFilter) || currentRoleFilter.equalsIgnoreCase(role);

            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || (u.getName() != null && u.getName().toLowerCase().contains(currentSearchQuery))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(currentSearchQuery))
                    || (u.getPhone() != null && u.getPhone().contains(currentSearchQuery));

            if (matchesRole && matchesSearch) {
                filtered.add(u);
            }
        }

        adapter.setUsers(filtered);
        binding.tvEmptyCustomers.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showChangeRoleDialog(User user) {
        String[] roles = {"CUSTOMER (USER)", "CYCLE HANDLER (HANDLER)", "ADMINISTRATOR (ADMIN)"};
        String[] roleCodes = {"USER", "HANDLER", "ADMIN"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Role for " + (user.getName() != null ? user.getName() : user.getEmail()))
                .setItems(roles, (dialog, which) -> {
                    String selectedRole = roleCodes[which];
                    confirmRoleChange(user, selectedRole);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRoleChange(User user, String newRole) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Privilege Change")
                .setMessage("Are you sure you want to change " + (user.getName() != null ? user.getName() : user.getEmail())
                        + "'s role to " + newRole + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    adminViewModel.updateUserRole(user.getUid(), newRole,
                            () -> Toast.makeText(requireContext(), "Role updated successfully!", Toast.LENGTH_SHORT).show(),
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
