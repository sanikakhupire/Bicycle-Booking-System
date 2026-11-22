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
    private android.os.CountDownTimer countDownTimer;

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
            binding.cardTimer.setVisibility(View.GONE);
            binding.tvBicycleInfo.setVisibility(View.GONE);
        } else {
            binding.tvScanInstruction.setText(
                "Scan the QR code on the bicycle to unlock and start your ride");
            binding.tvScanTitle.setText("Unlock Bicycle");
            String shortId = expectedBicycleId != null && expectedBicycleId.length() >= 8
                    ? expectedBicycleId.substring(0, 8).toUpperCase()
                    : (expectedBicycleId != null ? expectedBicycleId : "—");
            binding.tvBicycleInfo.setText("Reserved Bicycle: #" + shortId);
            setupReservationTimer();
        }

        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnManualEntry.setOnClickListener(v -> showManualEntry());
        binding.btnCancelBooking.setOnClickListener(v -> confirmCancelBooking());

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

    private void setupReservationTimer() {
        if (bookingId == null) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener(snap -> {
                if (!isAdded() || binding == null || snap == null || !snap.exists()) return;
                java.util.Date expiresAt = snap.getDate("expiresAt");
                java.util.Date createdAt = snap.getDate("createdAt");

                long expiryMs;
                if (expiresAt != null) {
                    expiryMs = expiresAt.getTime();
                } else if (createdAt != null) {
                    expiryMs = createdAt.getTime() + 10 * 60 * 1000;
                } else {
                    expiryMs = System.currentTimeMillis() + 10 * 60 * 1000;
                }

                long remainingMs = expiryMs - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    handleReservationExpired();
                    return;
                }

                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }

                countDownTimer = new android.os.CountDownTimer(remainingMs, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (binding == null) return;
                        long minutes = millisUntilFinished / 60000;
                        long seconds = (millisUntilFinished % 60000) / 1000;
                        binding.tvTimer.setText(String.format("Hold expires in: %02d:%02d", minutes, seconds));
                    }

                    @Override
                    public void onFinish() {
                        if (binding != null) {
                            binding.tvTimer.setText("Reservation Expired");
                        }
                        handleReservationExpired();
                    }
                }.start();
            });
    }

    private void handleReservationExpired() {
        if (!isAdded()) return;
        bookingViewModel.expireBooking(bookingId, expectedBicycleId, stationId != null ? stationId : "");
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reservation Expired")
            .setMessage("Your 10-minute hold has elapsed. The bicycle has been released back to station inventory.")
            .setCancelable(false)
            .setPositiveButton("OK", (dialog, which) -> {
                if (isAdded()) {
                    requireActivity().onBackPressed();
                }
            })
            .show();
    }

    private void confirmCancelBooking() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel Reservation")
            .setMessage("Are you sure you want to cancel this booking?\n\nCancellation Policy:\n• Free cancellation before unlocking the bicycle.\n• The bicycle will immediately be returned to station inventory.")
            .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelBooking())
            .setNegativeButton("Keep Reservation", null)
            .show();
    }

    private void cancelBooking() {
        setLoading(true);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        bookingViewModel.cancelBooking(bookingId, expectedBicycleId,
                stationId != null ? stationId : "")
            .observe(getViewLifecycleOwner(), resource -> {
                setLoading(false);
                if (resource.isSuccess()) {
                    android.widget.Toast.makeText(requireContext(),
                        "Booking cancelled. Bicycle returned to station.", android.widget.Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                } else if (resource.isError()) {
                    showError("Cancellation failed: " + resource.message);
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
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
