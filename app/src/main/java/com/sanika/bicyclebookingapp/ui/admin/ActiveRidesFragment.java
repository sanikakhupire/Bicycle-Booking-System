package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sanika.bicyclebookingapp.databinding.FragmentActiveRidesBinding;
import com.sanika.bicyclebookingapp.ui.history.adapter.BookingHistoryAdapter;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;

/**
 * Admin screen showing all currently active rides in real-time.
 */
public class ActiveRidesFragment extends Fragment {

    private FragmentActiveRidesBinding binding;
    private BookingViewModel bookingViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentActiveRidesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);

        BookingHistoryAdapter adapter = new BookingHistoryAdapter();
        binding.rvActiveRides.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvActiveRides.setAdapter(adapter);

        bookingViewModel.getAllActiveRides().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        adapter.submitList(resource.data);
                        binding.tvCount.setText(resource.data.size() + " active rides");
                        binding.tvEmpty.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.tvCount.setText("0 active rides");
                    }
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    break;
            }
        });

        // Theft detection button
        binding.btnCheckTheft.setOnClickListener(v -> {
            binding.tvTheftStatus.setText("Checking for theft...");
            binding.tvTheftStatus.setVisibility(View.VISIBLE);

            com.sanika.bicyclebookingapp.util.TheftDetector.checkForTheft(
                new com.sanika.bicyclebookingapp.util.TheftDetector.TheftCallback() {
                    @Override
                    public void onTheftDetected(String bicycleId, String stationName, double distanceKm) {
                        String msg = "⚠️ THEFT ALERT: Bicycle " + bicycleId.substring(0, 6).toUpperCase()
                                + " is " + String.format("%.1f", distanceKm) + "km from " + stationName;
                        binding.tvTheftStatus.setText(msg);
                        binding.tvTheftStatus.setTextColor(
                                requireContext().getColor(com.sanika.bicyclebookingapp.R.color.colorUnavailable));
                    }

                    @Override
                    public void onCheckComplete(int checked, int thefts) {
                        if (thefts == 0) {
                            binding.tvTheftStatus.setText("✓ All " + checked + " bicycles are safe");
                            binding.tvTheftStatus.setTextColor(
                                    requireContext().getColor(com.sanika.bicyclebookingapp.R.color.colorAvailable));
                        }
                    }
                }
            );
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
