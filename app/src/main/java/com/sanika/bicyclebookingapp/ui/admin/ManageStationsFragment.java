package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.GeoPoint;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentManageStationsBinding;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.ui.admin.adapter.AdminStationAdapter;
import com.sanika.bicyclebookingapp.viewmodel.StationViewModel;

/**
 * Admin screen to add, edit, and delete stations.
 */
public class ManageStationsFragment extends Fragment {

    private FragmentManageStationsBinding binding;
    private StationViewModel stationViewModel;
    private AdminStationAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentManageStationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stationViewModel = new ViewModelProvider(this).get(StationViewModel.class);

        adapter = new AdminStationAdapter(
                this::showEditDialog,
                this::confirmDelete
        );

        binding.rvStations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStations.setAdapter(adapter);

        binding.fabAddStation.setOnClickListener(v -> showAddDialog());

        observeStations();
    }

    private void observeStations() {
        stationViewModel.getAllStations().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                adapter.submitList(resource.data);
                binding.tvEmpty.setVisibility(resource.data.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_station, null);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Station")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    try {
                        EditText etName    = dialogView.findViewById(R.id.etStationName);
                        EditText etAddress = dialogView.findViewById(R.id.etAddress);
                        EditText etLat     = dialogView.findViewById(R.id.etLatitude);
                        EditText etLng     = dialogView.findViewById(R.id.etLongitude);
                        EditText etTotal   = dialogView.findViewById(R.id.etTotalBicycles);

                        String name    = etName.getText().toString().trim();
                        String address = etAddress.getText().toString().trim();
                        double lat     = Double.parseDouble(etLat.getText().toString().trim());
                        double lng     = Double.parseDouble(etLng.getText().toString().trim());
                        int total      = Integer.parseInt(etTotal.getText().toString().trim());

                        if (name.isEmpty()) {
                            Toast.makeText(requireContext(), "Station name is required", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Station station = new Station(null, name, address, lat, lng, total);
                        stationViewModel.addStation(station).observe(getViewLifecycleOwner(), resource -> {
                            if (resource.isError()) {
                                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Invalid latitude/longitude/total", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(Station station) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_station, null);

        EditText etName    = dialogView.findViewById(R.id.etStationName);
        EditText etAddress = dialogView.findViewById(R.id.etAddress);
        EditText etLat     = dialogView.findViewById(R.id.etLatitude);
        EditText etLng     = dialogView.findViewById(R.id.etLongitude);
        EditText etTotal   = dialogView.findViewById(R.id.etTotalBicycles);

        etName.setText(station.getName());
        etAddress.setText(station.getAddress());
        etLat.setText(String.valueOf(station.getLatitude()));
        etLng.setText(String.valueOf(station.getLongitude()));
        etTotal.setText(String.valueOf(station.getTotalBicycles()));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Station")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        station.setName(etName.getText().toString().trim());
                        station.setAddress(etAddress.getText().toString().trim());
                        double lat = Double.parseDouble(etLat.getText().toString().trim());
                        double lng = Double.parseDouble(etLng.getText().toString().trim());
                        station.setLocation(new GeoPoint(lat, lng));
                        station.setTotalBicycles(Integer.parseInt(etTotal.getText().toString().trim()));
                        stationViewModel.updateStation(station);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Invalid latitude/longitude/total", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(Station station) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Station")
                .setMessage("Delete \"" + station.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        stationViewModel.deleteStation(station.getStationId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
