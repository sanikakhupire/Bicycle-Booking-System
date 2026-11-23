package com.sanika.bicyclebookingapp.ui.loyalty;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.databinding.FragmentReferralBinding;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.LoyaltyViewModel;
import com.sanika.bicyclebookingapp.viewmodel.ReferralViewModel;

/**
 * Customer Refer & Earn Invitation screen.
 */
public class ReferralFragment extends Fragment {

    private FragmentReferralBinding binding;
    private LoyaltyViewModel loyaltyViewModel;
    private ReferralViewModel referralViewModel;
    private ReferralAdapter adapter;

    private String myReferralCode = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReferralBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loyaltyViewModel = new ViewModelProvider(this).get(LoyaltyViewModel.class);
        referralViewModel = new ViewModelProvider(this).get(ReferralViewModel.class);

        binding.toolbarReferral.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        setupRecyclerView();
        loadReferralData();

        binding.btnCopyCode.setOnClickListener(v -> copyCodeToClipboard());
        binding.btnShareInvite.setOnClickListener(v -> shareInviteCode());
        binding.btnApplyReferralCode.setOnClickListener(v -> applyFriendCode());
    }

    private void setupRecyclerView() {
        adapter = new ReferralAdapter();
        binding.rvReferrals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReferrals.setAdapter(adapter);
    }

    private void loadReferralData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to view referral rewards", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBarReferrals.setVisibility(View.VISIBLE);

        // Fetch referral code from loyalty account
        loyaltyViewModel.getLoyaltyAccount(user.getUid(), user.getDisplayName())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (binding == null) return;

                    if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                        myReferralCode = resource.data.getReferralCode();
                        binding.tvMyReferralCode.setText(myReferralCode != null ? myReferralCode : "RIDER100");
                    }
                });

        // Fetch referred friends list
        referralViewModel.getCustomerReferrals(user.getUid())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (binding == null) return;
                    binding.progressBarReferrals.setVisibility(View.GONE);

                    if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                        adapter.setReferrals(resource.data);
                        binding.tvEmptyReferrals.setVisibility(resource.data.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void copyCodeToClipboard() {
        if (TextUtils.isEmpty(myReferralCode)) return;

        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Bicycle Referral Code", myReferralCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), "Referral code " + myReferralCode + " copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void shareInviteCode() {
        if (TextUtils.isEmpty(myReferralCode)) return;

        String shareText = "Hey! Join me on India's smart bicycle rental app. Use my invite code "
                + myReferralCode + " to get 20% off on your first ride! Download now.";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Share Referral Code");
        startActivity(shareIntent);
    }

    private void applyFriendCode() {
        String enteredCode = binding.etInputReferralCode.getText() != null
                ? binding.etInputReferralCode.getText().toString().trim() : "";

        if (TextUtils.isEmpty(enteredCode)) {
            binding.etInputReferralCode.setError("Please enter a referral code");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to apply referral code", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnApplyReferralCode.setEnabled(false);

        referralViewModel.registerReferral(
                enteredCode,
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Friend",
                user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
        ).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.btnApplyReferralCode.setEnabled(true);

            if (resource.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), resource.data, Toast.LENGTH_LONG).show();
                binding.cardApplyReferral.setVisibility(View.GONE);
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Failed: " + resource.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
