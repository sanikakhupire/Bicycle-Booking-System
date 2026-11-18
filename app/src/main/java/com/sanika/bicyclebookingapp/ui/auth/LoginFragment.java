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
import com.sanika.bicyclebookingapp.databinding.FragmentLoginBinding;
import com.sanika.bicyclebookingapp.ui.main.MainActivity;
import com.sanika.bicyclebookingapp.util.ValidationUtil;
import com.sanika.bicyclebookingapp.viewmodel.AuthViewModel;

/**
 * Login screen — email + password sign-in via Firebase Auth.
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Login button
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Navigate to Signup
        binding.tvSignup.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_signup));

        // Navigate to Forgot Password
        binding.tvForgotPassword.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_forgotPassword));
    }

    private void attemptLogin() {
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validate inputs
        if (!ValidationUtil.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        binding.tilEmail.setError(null);

        if (!ValidationUtil.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_short_password));
            return;
        }
        binding.tilPassword.setError(null);

        setLoading(true);

        authViewModel.login(email, password).observe(getViewLifecycleOwner(), resource -> {
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
        binding.btnLogin.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
