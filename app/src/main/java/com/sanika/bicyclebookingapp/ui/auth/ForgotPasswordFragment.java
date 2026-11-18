package com.sanika.bicyclebookingapp.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sanika.bicyclebookingapp.databinding.FragmentForgotPasswordBinding;
import com.sanika.bicyclebookingapp.util.ValidationUtil;
import com.sanika.bicyclebookingapp.viewmodel.AuthViewModel;

/**
 * Forgot password screen — sends a reset link via Firebase Auth.
 */
public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnSendReset.setOnClickListener(v -> sendReset());
        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void sendReset() {
        String email = binding.etEmail.getText().toString().trim();

        if (!ValidationUtil.isValidEmail(email)) {
            binding.tilEmail.setError("Enter a valid email");
            return;
        }
        binding.tilEmail.setError(null);

        setLoading(true);

        authViewModel.sendPasswordReset(email).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    binding.tvSuccess.setVisibility(View.VISIBLE);
                    binding.tvSuccess.setText("Reset link sent to " + email);
                    binding.tvError.setVisibility(View.GONE);
                    break;
                case ERROR:
                    setLoading(false);
                    binding.tvError.setVisibility(View.VISIBLE);
                    binding.tvError.setText(resource.message);
                    binding.tvSuccess.setVisibility(View.GONE);
                    break;
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSendReset.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
