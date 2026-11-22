package com.sanika.bicyclebookingapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.databinding.FragmentProfileBinding;
import com.sanika.bicyclebookingapp.ui.admin.AdminActivity;
import com.sanika.bicyclebookingapp.ui.auth.AuthActivity;
import com.sanika.bicyclebookingapp.viewmodel.AuthViewModel;

/**
 * Profile screen — shows user info, edit profile, logout, and admin panel access.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private AuthViewModel authViewModel;
    private String uid;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        uid = FirebaseAuth.getInstance().getUid();

        if (uid != null) {
            loadProfile();
        }

        binding.btnEditProfile.setOnClickListener(v -> toggleEditMode(true));
        binding.btnSave.setOnClickListener(v -> saveProfile());
        binding.btnCancel.setOnClickListener(v -> toggleEditMode(false));
        binding.btnLogout.setOnClickListener(v -> confirmLogout());

        // Admin panel button — hidden by default, shown only for admins
        binding.btnAdminPanel.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AdminActivity.class);
            startActivity(intent);
        });

        // Handler portal button — hidden by default, shown for handlers and admins
        binding.btnHandlerPortal.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.sanika.bicyclebookingapp.ui.handler.CycleHandlerActivity.class);
            startActivity(intent);
        });
    }

    private void loadProfile() {
        authViewModel.getUserProfile(uid).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                binding.tvName.setText(resource.data.getName());
                binding.tvEmail.setText(resource.data.getEmail());
                binding.tvPhone.setText(resource.data.getPhone());
                binding.etName.setText(resource.data.getName());
                binding.etPhone.setText(resource.data.getPhone());

                // Show admin button only if user is admin
                isAdmin = resource.data.isAdmin();
                binding.btnAdminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

                // Show handler portal button for handlers or admins
                boolean isHandler = resource.data.isHandler();
                binding.btnHandlerPortal.setVisibility((isAdmin || isHandler) ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void saveProfile() {
        String name  = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            binding.tilName.setError("Name cannot be empty");
            return;
        }
        binding.tilName.setError(null);

        if (!com.sanika.bicyclebookingapp.util.ValidationUtil.isValidPhone(phone)) {
            binding.tilPhone.setError("Enter a valid 10-digit Indian mobile number");
            return;
        }
        binding.tilPhone.setError(null);
        phone = com.sanika.bicyclebookingapp.util.ValidationUtil.normalizeIndianPhone(phone);

        setLoading(true);
        authViewModel.updateProfile(uid, name, phone).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    toggleEditMode(false);
                    loadProfile();
                    break;
                case ERROR:
                    setLoading(false);
                    binding.tvError.setVisibility(View.VISIBLE);
                    binding.tvError.setText(resource.message);
                    break;
            }
        });
    }

    private void toggleEditMode(boolean editing) {
        binding.layoutView.setVisibility(editing ? View.GONE : View.VISIBLE);
        binding.layoutEdit.setVisibility(editing ? View.VISIBLE : View.GONE);
        binding.tvError.setVisibility(View.GONE);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    authViewModel.logout();
                    Intent intent = new Intent(requireActivity(), AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
