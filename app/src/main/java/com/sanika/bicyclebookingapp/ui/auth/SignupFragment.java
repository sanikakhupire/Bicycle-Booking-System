package com.sanika.bicyclebookingapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentSignupBinding;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.ValidationUtil;
import com.sanika.bicyclebookingapp.viewmodel.AuthViewModel;

/**
 * Signup screen — creates Firebase Auth account and saves user profile to Firestore.
 */
public class SignupFragment extends Fragment {

    private FragmentSignupBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSignupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnSignup.setOnClickListener(v -> attemptSignup());

        binding.tvLogin.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
    }

    private void attemptSignup() {
        String name     = binding.etName.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String phone    = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirm  = binding.etConfirmPassword.getText().toString().trim();

        // Validate
        if (!ValidationUtil.isNotEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_empty_fields));
            return;
        }
        binding.tilName.setError(null);

        if (!ValidationUtil.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        binding.tilEmail.setError(null);

        if (!ValidationUtil.isValidPhone(phone)) {
            binding.tilPhone.setError("Enter a valid phone number");
            return;
        }
        binding.tilPhone.setError(null);

        if (!ValidationUtil.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_short_password));
            return;
        }
        binding.tilPassword.setError(null);

        if (!password.equals(confirm)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }
        binding.tilConfirmPassword.setError(null);

        setLoading(true);

        authViewModel.register(name, email, password, phone).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    navigateToMain();
                    break;
                case ERROR:
                    setLoading(false);
                    binding.tvError.setVisibility(View.VISIBLE);
                    binding.tvError.setText(resource.message);
                    break;
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSignup.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
