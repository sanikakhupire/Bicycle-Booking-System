package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentAdminReviewsBinding;
import com.sanika.bicyclebookingapp.model.RideReview;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminReviewAdapter;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.ReviewViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for Admin Customer Reviews and Bicycle Condition Moderation.
 */
public class AdminReviewsFragment extends Fragment {

    private FragmentAdminReviewsBinding binding;
    private ReviewViewModel reviewViewModel;
    private AdminReviewAdapter adapter;

    private List<RideReview> allReviews = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminReviewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reviewViewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        setupRecyclerView();
        setupFilters();
        loadReviews();
    }

    private void setupRecyclerView() {
        adapter = new AdminReviewAdapter((review, newStatus, reason) -> {
            String adminUid = FirebaseAuth.getInstance().getUid();
            reviewViewModel.moderateReview(review.getReviewId(), newStatus, reason, adminUid != null ? adminUid : "ADMIN")
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Review status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                        } else if (res.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Moderation failed: " + res.message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviews.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupReviewStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipReviewPublished) {
                currentFilter = RideReview.STATUS_PUBLISHED;
            } else if (checkedId == R.id.chipReviewFlagged) {
                currentFilter = RideReview.STATUS_FLAGGED;
            } else if (checkedId == R.id.chipReviewHidden) {
                currentFilter = RideReview.STATUS_HIDDEN;
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void loadReviews() {
        binding.progressBarReviews.setVisibility(View.VISIBLE);

        reviewViewModel.getAllReviews().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarReviews.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                allReviews = resource.data;
                applyFilter();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        List<RideReview> filtered = new ArrayList<>();

        for (RideReview r : allReviews) {
            String status = r.getStatus() != null ? r.getStatus().toUpperCase() : RideReview.STATUS_PUBLISHED;

            if ("ALL".equalsIgnoreCase(currentFilter) || currentFilter.equalsIgnoreCase(status)) {
                filtered.add(r);
            }
        }

        adapter.setReviews(filtered);
        binding.tvEmptyReviews.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
