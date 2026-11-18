package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentManageBicyclesBinding;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminBicycleAdapter;
import com.sanika.bicyclebookingapp.viewmodel.BicycleViewModel;
import com.sanika.bicyclebookingapp.viewmodel.StationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen — shows ALL bicycles across all stations.
 */
public class ManageBicyclesFragment extends Fragment {

    private FragmentManageBicyclesBinding binding;
    private BicycleViewModel bicycleViewModel;
    private StationViewModel stationViewModel;
    private AdminBicycleAdapter adapter;
    private final List<Station> stationList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentManageBicyclesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bicycleViewModel = new ViewModelProvider(this).get(BicycleViewModel.class);
        stationViewModel = new ViewModelProvider(this).get(StationViewModel.class);

        adapter = new AdminBicycleAdapter(
                this::toggleAvailability,
                this::confirmDeleteBicycle
        );

        binding.rvBicycles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBicycles.setAdapter(adapter);

        binding.fabAddBicycle.setOnClickListener(v -> showAddBicycleDialog());

        // One-time fix: set qrCode = documentId and recalculate station counts
        com.sanika.bicyclebookingapp.util.DataFixUtil.fixAll(null);

        // Load all stations (for the add dialog spinner)
        stationViewModel.getAllStations().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                stationList.clear();
                stationList.addAll(resource.data);
            }
        });

        // Load ALL bicycles across all stations
        bicycleViewModel.getAllBicycles().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    break;
                case SUCCESS:
                    List<Bicycle> bikes = resource.data != null ? resource.data : new ArrayList<>();
                    adapter.submitList(bikes);
                    binding.tvEmpty.setVisibility(bikes.isEmpty() ? View.VISIBLE : View.GONE);
                    break;
                case ERROR:
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    binding.tvEmpty.setText("Error loading bicycles: " + resource.message);
                    break;
            }
        });
    }

    private void showAddBicycleDialog() {
        if (stationList.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Please add a station first before adding bicycles.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] stationNames = new String[stationList.size()];
        for (int i = 0; i < stationList.size(); i++) {
            stationNames[i] = stationList.get(i).getName();
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_bicycle, null);
        EditText etType = dialogView.findViewById(R.id.etBicycleType);
        Spinner spinnerStation = dialogView.findViewById(R.id.spinnerStation);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, stationNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStation.setAdapter(spinnerAdapter);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Bicycle")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String type = etType.getText().toString().trim();
                    if (type.isEmpty()) type = "Standard";

                    int idx = spinnerStation.getSelectedItemPosition();
                    Station selectedStation = stationList.get(idx);

                    Bicycle bicycle = new Bicycle(null, selectedStation.getStationId(), type);
                    bicycle.setStationName(selectedStation.getName());
                    bicycleViewModel.addBicycle(bicycle);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleAvailability(Bicycle bicycle) {
        bicycleViewModel.updateAvailability(bicycle.getBicycleId(), !bicycle.isAvailable());
    }

    private void confirmDeleteBicycle(Bicycle bicycle) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Bicycle")
                .setMessage("Delete this bicycle?")
                .setPositiveButton("Delete", (dialog, which) ->
                        bicycleViewModel.deleteBicycle(bicycle.getBicycleId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
