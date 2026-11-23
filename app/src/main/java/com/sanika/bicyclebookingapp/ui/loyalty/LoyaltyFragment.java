package com.sanika.bicyclebookingapp.ui.loyalty;

import android.os.Bundle;
import android.text.InputType;
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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentLoyaltyBinding;
import com.sanika.bicyclebookingapp.model.LoyaltyAccount;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.LoyaltyViewModel;

import java.util.Locale;

/**
 * Customer Loyalty Club, Milestones, and Points Wallet screen.
 */
public class LoyaltyFragment extends Fragment {

    private FragmentLoyaltyBinding binding;
    private LoyaltyViewModel loyaltyViewModel;
    private LoyaltyTransactionAdapter adapter;
    private LoyaltyAccount currentAccount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoyaltyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loyaltyViewModel = new ViewModelProvider(this).get(LoyaltyViewModel.class);

        binding.toolbarLoyalty.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        setupRecyclerView();
        loadLoyaltyData();

        binding.cardReferAndEarnPromo.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.referralFragment)
        );

        binding.btnRedeemPoints.setOnClickListener(v -> showRedeemDialog());
    }

    private void setupRecyclerView() {
        adapter = new LoyaltyTransactionAdapter();
        binding.rvLoyaltyTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLoyaltyTransactions.setAdapter(adapter);
    }

    private void loadLoyaltyData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to view rewards", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBarLoyalty.setVisibility(View.VISIBLE);

        // Load account
        loyaltyViewModel.getLoyaltyAccount(user.getUid(), user.getDisplayName())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (binding == null) return;

                    if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                        currentAccount = resource.data;
                        renderAccountDetails(currentAccount);
                    }
                });

        // Load transaction history
        loyaltyViewModel.getLoyaltyHistory(user.getUid())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (binding == null) return;
                    binding.progressBarLoyalty.setVisibility(View.GONE);

                    if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                        adapter.setTransactions(resource.data);
                        binding.tvEmptyLoyaltyHistory.setVisibility(resource.data.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void renderAccountDetails(LoyaltyAccount account) {
        binding.tvLoyaltyPoints.setText(account.getPointsBalance() + " pts");
        binding.tvLoyaltyRupeeValue.setText(String.format(Locale.ENGLISH, "Equivalent to ₹%.2f in ride discounts (1 pt = 10 paise)", account.getPointsValueInr()));
        binding.tvLoyaltyTier.setText(account.getTierLevel() + " RIDER");
        binding.tvCompletedRidesCount.setText("Total Rides: " + account.getTotalCompletedRides() + " completed");

        // Milestones
        int rides = account.getTotalCompletedRides();

        if (account.isMilestoneBronzeClaimed() || rides >= 5) {
            binding.tvMilestoneBronzeStatus.setText("✓ Claimed (+50 pts)");
            binding.tvMilestoneBronzeStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
        } else {
            binding.tvMilestoneBronzeStatus.setText(rides + "/5 rides");
            binding.tvMilestoneBronzeStatus.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        }

        if (account.isMilestoneSilverClaimed() || rides >= 15) {
            binding.tvMilestoneSilverStatus.setText("✓ Claimed (+150 pts)");
            binding.tvMilestoneSilverStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
        } else {
            binding.tvMilestoneSilverStatus.setText(rides + "/15 rides");
            binding.tvMilestoneSilverStatus.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        }

        if (account.isMilestoneGoldClaimed() || rides >= 30) {
            binding.tvMilestoneGoldStatus.setText("✓ Claimed (+350 pts)");
            binding.tvMilestoneGoldStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
        } else {
            binding.tvMilestoneGoldStatus.setText(rides + "/30 rides");
            binding.tvMilestoneGoldStatus.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        }

        if (account.isMilestonePlatinumClaimed() || rides >= 50) {
            binding.tvMilestonePlatinumStatus.setText("✓ Claimed (+1000 pts)");
            binding.tvMilestonePlatinumStatus.setTextColor(requireContext().getColor(R.color.colorAvailable));
        } else {
            binding.tvMilestonePlatinumStatus.setText(rides + "/50 rides");
            binding.tvMilestonePlatinumStatus.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        }
    }

    private void showRedeemDialog() {
        if (currentAccount == null || currentAccount.getPointsBalance() < 100) {
            Toast.makeText(requireContext(), "You need at least 100 points (₹10.00 value) to redeem.", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText etPoints = new EditText(requireContext());
        etPoints.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPoints.setHint("Enter points (multiples of 100, max " + currentAccount.getPointsBalance() + ")");

        new AlertDialog.Builder(requireContext())
                .setTitle("Redeem Loyalty Points")
                .setMessage("Convert your loyalty points into a ride discount code.\nConversion rate: 100 pts = ₹10.00.")
                .setView(etPoints)
                .setPositiveButton("Redeem Now", (d, w) -> {
                    String ptsStr = etPoints.getText().toString().trim();
                    try {
                        long pts = Long.parseLong(ptsStr);
                        if (pts < 100) {
                            Toast.makeText(requireContext(), "Minimum 100 points required", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (pts > currentAccount.getPointsBalance()) {
                            Toast.makeText(requireContext(), "Entered points exceed your balance", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String uid = FirebaseAuth.getInstance().getUid();
                        loyaltyViewModel.redeemPoints(uid, pts, null).observe(getViewLifecycleOwner(), res -> {
                            if (res.status == Resource.Status.SUCCESS) {
                                double discountInr = res.data / 100.0;
                                Toast.makeText(requireContext(), String.format(Locale.ENGLISH, "Redeemed %d points for ₹%.2f discount coupon!", pts, discountInr), Toast.LENGTH_LONG).show();
                            } else if (res.status == Resource.Status.ERROR) {
                                Toast.makeText(requireContext(), "Redemption failed: " + res.message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Please enter a valid number of points", Toast.LENGTH_SHORT).show();
                    }
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
