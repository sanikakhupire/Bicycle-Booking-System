package com.sanika.bicyclebookingapp.ui.support;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentCreateTicketBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.SupportViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment allowing customers to report grievances, ride issues, payment disputes,
 * and refund requests with image attachments.
 */
public class CreateTicketFragment extends Fragment {

    private FragmentCreateTicketBinding binding;
    private SupportViewModel supportViewModel;

    private Uri selectedImageUri = null;
    private final List<String> categories = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.tvAttachmentName.setText(uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "photo_attachment.jpg");
                    binding.ivAttachmentPreview.setVisibility(View.VISIBLE);
                    binding.ivAttachmentPreview.setImageURI(uri);
                    binding.btnRemoveAttachment.setVisibility(View.VISIBLE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateTicketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        supportViewModel = new ViewModelProvider(this).get(SupportViewModel.class);

        binding.toolbarCreateTicket.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        setupCategorySpinner();
        prefillFromArguments();
        setupAttachmentPickers();

        binding.btnSubmitTicket.setOnClickListener(v -> submitTicket());
    }

    private void setupCategorySpinner() {
        categories.clear();
        categories.add(SupportTicket.CATEGORY_BILLING_DISPUTE);
        categories.add(SupportTicket.CATEGORY_RIDE_PROBLEM);
        categories.add(SupportTicket.CATEGORY_PAYMENT_PROBLEM);
        categories.add(SupportTicket.CATEGORY_REFUND_REQUEST);
        categories.add(SupportTicket.CATEGORY_UNSAFE_BICYCLE);
        categories.add(SupportTicket.CATEGORY_DAMAGED_BICYCLE);
        categories.add(SupportTicket.CATEGORY_STATION_PROBLEM);
        categories.add(SupportTicket.CATEGORY_ACCOUNT_LOGIN);
        categories.add(SupportTicket.CATEGORY_GENERAL);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );
        binding.spinnerTicketCategory.setAdapter(adapter);
    }

    private void prefillFromArguments() {
        if (getArguments() == null) return;

        String passedCategory = getArguments().getString("category");
        if (passedCategory != null) {
            int index = categories.indexOf(passedCategory);
            if (index >= 0) {
                binding.spinnerTicketCategory.setSelection(index);
            }
        }

        String bookingId = getArguments().getString("bookingId");
        if (bookingId != null && !bookingId.isEmpty()) {
            binding.etTicketBookingId.setText(bookingId);
        }

        String bicycleId = getArguments().getString("bicycleId");
        if (bicycleId != null && !bicycleId.isEmpty()) {
            binding.etTicketBicycleId.setText(bicycleId);
        }

        double disputedAmt = getArguments().getDouble("disputedAmount", 0.0);
        if (disputedAmt > 0) {
            binding.etTicketDisputedAmount.setText(String.valueOf(disputedAmt));
        }
    }

    private void setupAttachmentPickers() {
        binding.btnAttachImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.btnRemoveAttachment.setOnClickListener(v -> {
            selectedImageUri = null;
            binding.tvAttachmentName.setText("No file chosen");
            binding.ivAttachmentPreview.setImageURI(null);
            binding.ivAttachmentPreview.setVisibility(View.GONE);
            binding.btnRemoveAttachment.setVisibility(View.GONE);
        });
    }

    private void submitTicket() {
        String subject = binding.etTicketSubject.getText() != null ? binding.etTicketSubject.getText().toString().trim() : "";
        String description = binding.etTicketDescription.getText() != null ? binding.etTicketDescription.getText().toString().trim() : "";

        if (TextUtils.isEmpty(subject)) {
            binding.etTicketSubject.setError("Please enter a subject summary");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            binding.etTicketDescription.setError("Please describe your problem");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to submit a grievance.", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = binding.spinnerTicketCategory.getSelectedItem() != null
                ? binding.spinnerTicketCategory.getSelectedItem().toString()
                : SupportTicket.CATEGORY_GENERAL;

        String priority = SupportTicket.PRIORITY_MEDIUM;
        int checkedChipId = binding.chipGroupTicketPriority.getCheckedChipId();
        if (checkedChipId == R.id.chipPriorityLow) {
            priority = SupportTicket.PRIORITY_LOW;
        } else if (checkedChipId == R.id.chipPriorityHigh) {
            priority = SupportTicket.PRIORITY_HIGH;
        } else if (checkedChipId == R.id.chipPriorityUrgent) {
            priority = SupportTicket.PRIORITY_URGENT;
        }

        String bookingId = binding.etTicketBookingId.getText() != null ? binding.etTicketBookingId.getText().toString().trim() : "";
        String bicycleId = binding.etTicketBicycleId.getText() != null ? binding.etTicketBicycleId.getText().toString().trim() : "";
        String disputedAmtStr = binding.etTicketDisputedAmount.getText() != null ? binding.etTicketDisputedAmount.getText().toString().trim() : "";

        long disputedPaise = 0;
        if (!TextUtils.isEmpty(disputedAmtStr)) {
            try {
                disputedPaise = Math.round(Double.parseDouble(disputedAmtStr) * 100);
            } catch (NumberFormatException ignored) {}
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(user.getUid());
        ticket.setUserName(user.getDisplayName() != null ? user.getDisplayName() : "Customer");
        ticket.setUserPhone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus(SupportTicket.STATUS_OPEN);
        ticket.setBookingId(bookingId);
        ticket.setBicycleId(bicycleId);
        ticket.setDisputedAmountPaise(disputedPaise);

        if (selectedImageUri != null) {
            ticket.setAttachmentUrl(selectedImageUri.toString());
        }

        binding.progressBarCreateTicket.setVisibility(View.VISIBLE);
        binding.btnSubmitTicket.setEnabled(false);

        supportViewModel.createTicket(ticket).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarCreateTicket.setVisibility(View.GONE);
            binding.btnSubmitTicket.setEnabled(true);

            if (resource.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), "Support ticket logged successfully! (#" + resource.data + ")", Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).navigate(R.id.customerTicketsFragment);
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error submitting grievance: " + resource.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
