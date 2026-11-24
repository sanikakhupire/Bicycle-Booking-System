package com.sanika.bicyclebookingapp.ui.legal;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.databinding.ActivityLegalBinding;
import com.sanika.bicyclebookingapp.model.AccountDeletionRequest;
import com.sanika.bicyclebookingapp.util.LegalContentProvider;
import com.sanika.bicyclebookingapp.util.SecureLog;

/**
 * Activity presenting verified Indian legal compliance, DPDP Act 2023 rights,
 * terms of service, and user-initiated account deletion requests.
 */
public class LegalActivity extends AppCompatActivity {

    private ActivityLegalBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLegalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbarLegal.setNavigationOnClickListener(v -> finish());

        // Default to Privacy Policy
        showPrivacyPolicy();

        binding.tabLayoutLegal.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    showPrivacyPolicy();
                } else if (position == 1) {
                    showTermsOfService();
                } else if (position == 2) {
                    showDataErasure();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.btnSubmitDeletion.setOnClickListener(v -> confirmAndSubmitDeletion());
    }

    private void showPrivacyPolicy() {
        binding.tvLegalContent.setText(LegalContentProvider.getPrivacyPolicy());
        binding.layoutDataDeletion.setVisibility(View.GONE);
        binding.scrollViewLegal.scrollTo(0, 0);
    }

    private void showTermsOfService() {
        binding.tvLegalContent.setText(LegalContentProvider.getTermsAndConditions());
        binding.layoutDataDeletion.setVisibility(View.GONE);
        binding.scrollViewLegal.scrollTo(0, 0);
    }

    private void showDataErasure() {
        binding.tvLegalContent.setText(LegalContentProvider.getDataDeletionNotice());
        binding.layoutDataDeletion.setVisibility(View.VISIBLE);
        binding.scrollViewLegal.scrollTo(0, 0);
    }

    private void confirmAndSubmitDeletion() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be signed in to request account deletion.", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = binding.etDeletionReason.getText() != null
                ? binding.etDeletionReason.getText().toString().trim()
                : "";

        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion Request")
                .setMessage("Are you sure you want to request permanent erasure of your account and personal data? This action cannot be undone.")
                .setPositiveButton("Submit Request", (dialog, which) -> {
                    AccountDeletionRequest req = new AccountDeletionRequest(
                            currentUser.getUid(),
                            currentUser.getEmail(),
                            reason
                    );

                    FirebaseFirestore.getInstance().collection("account_deletion_requests")
                            .add(req)
                            .addOnSuccessListener(doc -> {
                                Toast.makeText(LegalActivity.this, "Deletion request submitted. Our data officer will process it within 7 days.", Toast.LENGTH_LONG).show();
                                binding.etDeletionReason.setText("");
                            })
                            .addOnFailureListener(e -> {
                                SecureLog.e("LegalActivity", "Failed to submit deletion request: " + e.getMessage());
                                Toast.makeText(LegalActivity.this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
