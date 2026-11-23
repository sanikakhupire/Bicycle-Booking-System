package com.sanika.bicyclebookingapp.ui.maintenance;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.databinding.FragmentReportDamageBinding;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.viewmodel.MaintenanceViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer screen to report bicycle damage, unsafe mechanical defects, or issues
 * during or after a ride.
 */
public class ReportDamageFragment extends Fragment {

    private FragmentReportDamageBinding binding;
    private MaintenanceViewModel maintenanceViewModel;

    private String bicycleId;
    private String bookingId;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && binding != null) {
                    selectedImageUri = uri;
                    binding.ivPhotoPreview.setVisibility(View.VISIBLE);
                    binding.ivPhotoPreview.setImageURI(uri);
                    binding.tvPhotoStatus.setText("Photo attached");
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReportDamageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        maintenanceViewModel = new ViewModelProvider(this).get(MaintenanceViewModel.class);

        if (getArguments() != null) {
            bicycleId = getArguments().getString("bicycleId", "");
            bookingId = getArguments().getString("bookingId", "");
        }

        setupUI();
        setupCategorySpinner();
    }

    private void setupUI() {
        if (bicycleId != null && !bicycleId.trim().isEmpty()) {
            binding.tvBicycleInfo.setText("Bicycle: " + bicycleId);
            binding.tilBicycleId.setVisibility(View.GONE);
        } else {
            binding.tvBicycleInfo.setText("Enter Bicycle Identifier below:");
            binding.tilBicycleId.setVisibility(View.VISIBLE);
        }

        if (bookingId != null && !bookingId.trim().isEmpty()) {
            String refStr = "Booking Ref: " + (bookingId.length() >= 8
                    ? "BK-" + bookingId.substring(0, 8).toUpperCase() : "BK-" + bookingId);
            binding.tvBookingRef.setText(refStr);
            binding.tvBookingRef.setVisibility(View.VISIBLE);
        } else {
            binding.tvBookingRef.setVisibility(View.GONE);
        }

        binding.btnAttachPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add(MaintenanceRecord.CAT_BRAKES);
        categories.add(MaintenanceRecord.CAT_TIRES_WHEELS);
        categories.add(MaintenanceRecord.CAT_CHAIN_PEDALS);
        categories.add(MaintenanceRecord.CAT_GEARS);
        categories.add(MaintenanceRecord.CAT_FRAME_FORK);
        categories.add(MaintenanceRecord.CAT_LIGHTS_BELL);
        categories.add(MaintenanceRecord.CAT_HANDLEBAR);
        categories.add(MaintenanceRecord.CAT_UNSAFE_GENERAL);
        categories.add(MaintenanceRecord.CAT_OTHER);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        binding.spinnerCategory.setAdapter(adapter);
    }

    private void submitReport() {
        String targetBikeId = (bicycleId != null && !bicycleId.trim().isEmpty())
                ? bicycleId : (binding.etBicycleId.getText() != null ? binding.etBicycleId.getText().toString().trim() : "");

        if (targetBikeId.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter or select a bicycle ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = binding.etDescription.getText() != null
                ? binding.etDescription.getText().toString().trim() : "";

        if (description.isEmpty()) {
            binding.tilDescription.setError("Please describe the damage or issue");
            return;
        }
        binding.tilDescription.setError(null);

        boolean isUnsafe = binding.switchUnsafe.isChecked();
        String category = (String) binding.spinnerCategory.getSelectedItem();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "anonymous";
        String userName = user != null && user.getDisplayName() != null ? user.getDisplayName() : "Customer";
        String userPhone = user != null && user.getPhoneNumber() != null ? user.getPhoneNumber() : "";

        MaintenanceRecord record = new MaintenanceRecord(
                targetBikeId, userId, userName, category, description, isUnsafe);
        record.setBookingId(bookingId);
        record.setReporterPhone(userPhone);
        record.setReporterRole("CUSTOMER");
        if (selectedImageUri != null) {
            record.setReportedImageUrl(selectedImageUri.toString());
        }

        binding.btnSubmitReport.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        maintenanceViewModel.reportDamage(record).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;

            if (resource.isLoading()) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else if (resource.isSuccess()) {
                binding.progressBar.setVisibility(View.GONE);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Report Submitted")
                        .setMessage("Thank you for your report. Bicycle " + targetBikeId
                                + " has been marked for inspection and taken out of public service to ensure rider safety.")
                        .setPositiveButton("Done", (d, w) -> {
                            Navigation.findNavController(requireView()).navigateUp();
                        })
                        .setCancelable(false)
                        .show();

            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmitReport.setEnabled(true);
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
