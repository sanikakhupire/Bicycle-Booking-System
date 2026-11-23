package com.sanika.bicyclebookingapp.ui.ride;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.DialogRateRideBinding;
import com.sanika.bicyclebookingapp.model.RideReview;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.ReviewViewModel;

/**
 * DialogFragment allowing customers to submit a rating and review for their completed ride.
 */
public class RateRideDialogFragment extends DialogFragment {

    public interface OnReviewSubmittedListener {
        void onReviewSubmitted(RideReview review);
    }

    private DialogRateRideBinding binding;
    private ReviewViewModel reviewViewModel;
    private OnReviewSubmittedListener listener;

    private String bookingId;
    private String bicycleId;
    private String pickupStationId;
    private String pickupStationName;
    private String returnStationId;
    private String returnStationName;

    public static RateRideDialogFragment newInstance(String bookingId, String bicycleId,
                                                     String pickupStationId, String pickupStationName,
                                                     String returnStationId, String returnStationName) {
        RateRideDialogFragment fragment = new RateRideDialogFragment();
        Bundle args = new Bundle();
        args.putString("bookingId", bookingId);
        args.putString("bicycleId", bicycleId);
        args.putString("pickupStationId", pickupStationId);
        args.putString("pickupStationName", pickupStationName);
        args.putString("returnStationId", returnStationId);
        args.putString("returnStationName", returnStationName);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnReviewSubmittedListener(OnReviewSubmittedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogRateRideBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reviewViewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        if (getArguments() != null) {
            bookingId = getArguments().getString("bookingId");
            bicycleId = getArguments().getString("bicycleId");
            pickupStationId = getArguments().getString("pickupStationId");
            pickupStationName = getArguments().getString("pickupStationName");
            returnStationId = getArguments().getString("returnStationId");
            returnStationName = getArguments().getString("returnStationName");
        }

        String bikeRef = bicycleId != null && !bicycleId.isEmpty() ? "Bicycle " + bicycleId : "Ride Experience";
        binding.tvRateSubtitle.setText("How was your trip with " + bikeRef + "?");

        binding.btnCancelReview.setOnClickListener(v -> dismiss());
        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to submit a review", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        float overall = binding.rbOverall.getRating();
        float bicycle = binding.rbBicycle.getRating();
        float station = binding.rbStation.getRating();

        String condition = RideReview.CONDITION_GOOD;
        int checkedChipId = binding.chipGroupCondition.getCheckedChipId();
        if (checkedChipId == R.id.chipCondExcellent) {
            condition = RideReview.CONDITION_EXCELLENT;
        } else if (checkedChipId == R.id.chipCondNeedsService) {
            condition = RideReview.CONDITION_NEEDS_SERVICE;
        } else if (checkedChipId == R.id.chipCondDamaged) {
            condition = RideReview.CONDITION_DAMAGED;
        }

        String comments = binding.etReviewComments.getText() != null ? binding.etReviewComments.getText().toString().trim() : "";

        RideReview review = new RideReview(
                bookingId,
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Customer",
                bicycleId,
                overall > 0 ? overall : 5.0f,
                bicycle > 0 ? bicycle : 5.0f,
                condition,
                comments
        );
        review.setRatingStation(station > 0 ? station : 5.0f);
        review.setPickupStationId(pickupStationId);
        review.setPickupStationName(pickupStationName);
        review.setReturnStationId(returnStationId);
        review.setReturnStationName(returnStationName);

        binding.progressBarReview.setVisibility(View.VISIBLE);
        binding.btnSubmitReview.setEnabled(false);

        reviewViewModel.submitReview(review).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarReview.setVisibility(View.GONE);
            binding.btnSubmitReview.setEnabled(true);

            if (resource.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), "Thank you for your rating & feedback!", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onReviewSubmitted(review);
                dismiss();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
