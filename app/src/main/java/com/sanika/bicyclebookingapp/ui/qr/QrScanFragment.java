package com.sanika.bicyclebookingapp.ui.qr;

import android.Manifest;
import android.content.Intent;
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

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.sanika.bicyclebookingapp.databinding.FragmentQrScanBinding;
import com.sanika.bicyclebookingapp.service.RideTrackingService;
import com.sanika.bicyclebookingapp.viewmodel.BookingViewModel;

import java.util.List;

/**
 * QR Scan Fragment — handles TWO scan modes:
 *
 * MODE 1: UNLOCK (scan bicycle QR to START ride)
 *   - Scanned code must match the booked bicycleId
 *   - On match → starts RideTrackingService → navigates to ActiveRide
 *
 * MODE 2: RETURN (scan station QR to END ride)
 *   - Scanned code must match a valid stationId in Firestore
 *   - On match → stops RideTrackingService → finalizes fare → navigates to RideComplete
 *
 * The mode is determined by the "mode" argument passed via Safe Args.
 */
public class QrScanFragment extends Fragment {

    public static final String MODE_UNLOCK = "UNLOCK";
    public static final String MODE_RETURN = "RETURN";

    private FragmentQrScanBinding binding;
    private BookingViewModel bookingViewModel;

    private String bookingId;
    private String expectedBicycleId;
    private String stationId;
    private String scanMode;
    private boolean scanned = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startScanning();
                else showError("Camera permission required to scan QR code");
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentQrScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        QrScanFragmentArgs args = QrScanFragmentArgs.fromBundle(getArguments());
        bookingId          = args.getBookingId();
        expectedBicycleId  = args.getBicycleId();
        stationId          = args.getStationId();
        scanMode           = args.getMode() != null ? args.getMode() : MODE_UNLOCK;

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);

        // Update instruction text based on mode
        if (MODE_RETURN.equals(scanMode)) {
            binding.tvScanInstruction.setText(
                "Scan the QR code at the station to return your bicycle and end the ride");
            binding.tvScanTitle.setText("Return Bicycle");
        } else {
            binding.tvScanInstruction.setText(
                "Scan the QR code on the bicycle to unlock and start your ride");
            binding.tvScanTitle.setText("Unlock Bicycle");
        }

        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnManualEntry.setOnClickListener(v -> showManualEntry());
        binding.btnCancelBooking.setOnClickListener(v -> cancelBooking());

        // Hide cancel button in RETURN mode
        binding.btnCancelBooking.setVisibility(
            MODE_RETURN.equals(scanMode) ? View.GONE : View.VISIBLE);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanning() {
        binding.barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (scanned || result.getText() == null) return;
                scanned = true;
                binding.barcodeScanner.pause();
                handleScannedCode(result.getText().trim());
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> points) {}
        });
    }

    private void handleScannedCode(String code) {
        if (MODE_UNLOCK.equals(scanMode)) {
            handleUnlockScan(code);
        } else {
            handleReturnScan(code);
        }
    }

    /**
     * UNLOCK MODE: code must match the booked bicycleId.
     */
    private void handleUnlockScan(String code) {
        if (code.equals(expectedBicycleId)) {
            setLoading(true);
            bookingViewModel.startRide(bookingId).observe(getViewLifecycleOwner(), resource -> {
                switch (resource.status) {
                    case LOADING:
                        setLoading(true);
                        break;
                    case SUCCESS:
                        setLoading(false);
                        // Start the foreground tracking service
                        startTrackingService();
                        // Navigate to active ride
                        QrScanFragmentDirections.ActionQrScanToActiveRide action =
                                QrScanFragmentDirections.actionQrScanToActiveRide(
                                        bookingId, expectedBicycleId,
                                        stationId != null ? stationId : "");
                        Navigation.findNavController(requireView()).navigate(action);
                        break;
                    case ERROR:
                        setLoading(false);
                        showError(resource.message);
                        resetScan();
                        break;
                }
            });
        } else {
            showError("Wrong bicycle QR code. Please scan the correct bicycle.");
            resetScan();
        }
    }

    /**
     * RETURN MODE: code must match a valid stationId in Firestore.
     * Validates the station QR, then ends the ride.
     */
    private void handleReturnScan(String code) {
        setLoading(true);
        // Validate that scanned code is a real station ID
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("stations").document(code)
            .get()
            .addOnSuccessListener(snap -> {
                if (snap.exists()) {
                    String returnStationName = snap.getString("name");
                    // Valid station — end the ride
                    endRideAtStation(code, returnStationName);
                } else {
                    setLoading(false);
                    showError("Invalid station QR. Please scan a valid station QR code.");
                    resetScan();
                }
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                showError("Could not verify station. Please try again.");
                resetScan();
            });
    }

    /**
     * Ends the ride when user scans a valid station QR.
     */
    private void endRideAtStation(String returnStationId, String returnStationName) {
        // Stop the tracking service
        Intent stopService = new Intent(requireContext(), RideTrackingService.class);
        stopService.setAction(RideTrackingService.ACTION_STOP);
        requireContext().startService(stopService);

        // End ride in Firestore via ViewModel
        bookingViewModel.endRide(bookingId, expectedBicycleId, returnStationId)
            .observe(getViewLifecycleOwner(), resource -> {
                switch (resource.status) {
                    case LOADING:
                        setLoading(true);
                        break;
                    case SUCCESS:
                        setLoading(false);
                        if (resource.data != null) {
                            // Navigate to ride complete with fare breakdown
                            QrScanFragmentDirections.ActionQrScanToActiveRide nav =
                                    QrScanFragmentDirections.actionQrScanToActiveRide(
                                            bookingId, expectedBicycleId, returnStationId);
                            // Actually navigate to ride complete directly
                            Bundle args = new Bundle();
                            args.putString("bookingId", bookingId);
                            args.putFloat("fare", (float) resource.data.getFare());
                            args.putLong("durationMinutes", resource.data.getDurationMinutes());
                            Navigation.findNavController(requireView())
                                .navigate(com.sanika.bicyclebookingapp.R.id.rideCompleteFragment,
                                        args);
                        }
                        break;
                    case ERROR:
                        setLoading(false);
                        showError(resource.message);
                        resetScan();
                        break;
                }
            });
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(requireContext(), RideTrackingService.class);
        serviceIntent.setAction(RideTrackingService.ACTION_START);
        serviceIntent.putExtra(RideTrackingService.EXTRA_BOOKING_ID, bookingId);
        serviceIntent.putExtra(RideTrackingService.EXTRA_BICYCLE_ID, expectedBicycleId);
        serviceIntent.putExtra(RideTrackingService.EXTRA_STATION_ID,
                stationId != null ? stationId : "");
        serviceIntent.putExtra(RideTrackingService.EXTRA_START_TIME,
                System.currentTimeMillis());
        requireContext().startForegroundService(serviceIntent);
    }

    private void cancelBooking() {
        bookingViewModel.cancelBooking(bookingId, expectedBicycleId,
                stationId != null ? stationId : "")
            .observe(getViewLifecycleOwner(), resource -> {
                if (resource.isSuccess()) {
                    requireActivity().onBackPressed();
                }
            });
    }

    private void showManualEntry() {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(MODE_RETURN.equals(scanMode)
                ? "Enter Station ID manually"
                : "Enter Bicycle ID manually");

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Paste ID here");
        input.setPadding(48, 24, 48, 24);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                scanned = true;
                binding.barcodeScanner.pause();
                handleScannedCode(code);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showError(String message) {
        if (binding == null) return;
        binding.tvError.setVisibility(View.VISIBLE);
        binding.tvError.setText(message);
    }

    private void resetScan() {
        scanned = false;
        if (binding != null) binding.barcodeScanner.resume();
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!scanned && binding != null) binding.barcodeScanner.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.barcodeScanner.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
