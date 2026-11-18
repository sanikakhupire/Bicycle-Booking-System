package com.sanika.bicyclebookingapp.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentHomeBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.ui.home.adapter.StationAdapter;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;
import com.sanika.bicyclebookingapp.viewmodel.StationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Home screen showing Google Map with station markers and a bottom sheet list.
 */
public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private FragmentHomeBinding binding;
    private StationViewModel stationViewModel;
    private BookingViewModel bookingViewModel;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private StationAdapter stationAdapter;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (Boolean.TRUE.equals(fine)) {
                    enableMyLocation();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stationViewModel = new ViewModelProvider(this).get(StationViewModel.class);
        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupMap();
        setupRecyclerView();
        observeStations();
        checkActiveRide();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupRecyclerView() {
        stationAdapter = new StationAdapter(station -> {
            // Navigate to station detail
            HomeFragmentDirections.ActionHomeToStationDetail action =
                    HomeFragmentDirections.actionHomeToStationDetail(station.getStationId());
            Navigation.findNavController(requireView()).navigate(action);
        });
        binding.rvStations.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvStations.setAdapter(stationAdapter);
    }

    private void observeStations() {
        stationViewModel.getAllStations().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        stationAdapter.submitList(resource.data);
                        addStationMarkers(resource.data);
                    }
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    break;
            }
        });
    }

    private void checkActiveRide() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        bookingViewModel.getActiveBooking(uid).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                Booking booking = resource.data;
                binding.cardActiveRide.setVisibility(View.VISIBLE);
                binding.tvActiveRideStatus.setText("Active Ride: " + booking.getStationName());
                binding.cardActiveRide.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("bookingId", booking.getBookingId());
                    args.putString("bicycleId", booking.getBicycleId());
                    args.putString("stationId", booking.getStationId() != null ? booking.getStationId() : "");
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_home_to_activeRide, args);
                });
            } else {
                binding.cardActiveRide.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && googleMap != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f));
                }
            });
        }
    }

    private void addStationMarkers(List<Station> stations) {
        if (googleMap == null) return;
        googleMap.clear();
        for (Station station : stations) {
            LatLng latLng = new LatLng(station.getLatitude(), station.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(station.getName())
                    .snippet(station.getAvailableBicycles() + " bicycles available")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            station.getAvailableBicycles() > 0
                                    ? BitmapDescriptorFactory.HUE_GREEN
                                    : BitmapDescriptorFactory.HUE_RED));
            googleMap.addMarker(markerOptions);
        }

        // Click on marker → navigate to station detail
        googleMap.setOnInfoWindowClickListener(marker -> {
            // Find station by title
            for (Station station : stations) {
                if (station.getName().equals(marker.getTitle())) {
                    HomeFragmentDirections.ActionHomeToStationDetail action =
                            HomeFragmentDirections.actionHomeToStationDetail(station.getStationId());
                    Navigation.findNavController(requireView()).navigate(action);
                    break;
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
