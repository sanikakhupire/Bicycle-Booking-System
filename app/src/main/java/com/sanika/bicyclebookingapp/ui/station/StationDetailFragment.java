package com.sanika.bicyclebookingapp.ui.station;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sanika.bicyclebookingapp.databinding.FragmentStationDetailBinding;
import com.sanika.bicyclebookingapp.ui.station.adapter.BicycleAdapter;
import com.sanika.bicyclebookingapp.viewmodel.BicycleViewModel;
import com.sanika.bicyclebookingapp.viewmodel.StationViewModel;

/**
 * Shows station info and list of available bicycles.
 * User can select a bicycle to book.
 */
public class StationDetailFragment extends Fragment {

    private FragmentStationDetailBinding binding;
    private StationViewModel stationViewModel;
    private BicycleViewModel bicycleViewModel;
    private String stationId;
    private String stationName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStationDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stationId = StationDetailFragmentArgs.fromBundle(getArguments()).getStationId();

        stationViewModel = new ViewModelProvider(this).get(StationViewModel.class);
        bicycleViewModel = new ViewModelProvider(this).get(BicycleViewModel.class);

        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());

        setupBicycleList();
        loadStation();
    }

    private void loadStation() {
        stationViewModel.getStation(stationId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                stationName = resource.data.getName();
                binding.tvStationName.setText(stationName);
                binding.tvAddress.setText(resource.data.getAddress());
                binding.tvAvailableCount.setText(
                        resource.data.getAvailableBicycles() + " / "
                        + resource.data.getTotalBicycles() + " available");
            }
        });
    }

    private void setupBicycleList() {
        BicycleAdapter adapter = new BicycleAdapter(bicycle -> {
            // Navigate to booking confirmation
            StationDetailFragmentDirections.ActionStationDetailToBookingConfirm action =
                    StationDetailFragmentDirections.actionStationDetailToBookingConfirm(
                            bicycle.getBicycleId(), stationId, stationName != null ? stationName : "");
            Navigation.findNavController(requireView()).navigate(action);
        });

        binding.rvBicycles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBicycles.setAdapter(adapter);

        bicycleViewModel.getBicyclesByStation(stationId).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        adapter.submitList(resource.data);
                        binding.tvNoBicycles.setVisibility(View.GONE);
                    } else {
                        binding.tvNoBicycles.setVisibility(View.VISIBLE);
                    }
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvNoBicycles.setVisibility(View.VISIBLE);
                    binding.tvNoBicycles.setText(resource.message);
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
