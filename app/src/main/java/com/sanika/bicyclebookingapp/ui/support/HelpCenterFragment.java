package com.sanika.bicyclebookingapp.ui.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentHelpCenterBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;

/**
 * Help Center & Customer Support Portal.
 * Displays FAQs, 24x7 India hotline, email support, and quick grievance redressal channels.
 */
public class HelpCenterFragment extends Fragment {

    private FragmentHelpCenterBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHelpCenterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbarHelpCenter.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        // 24x7 Hotline Call
        binding.btnCallHotline.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:18008892925"));
            try {
                startActivity(callIntent);
            } catch (Exception ignored) {}
        });

        // Email Support
        binding.btnEmailSupport.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@bicyclebooking.in"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Customer Grievance / Inquiry");
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Support Email"));
            } catch (Exception ignored) {}
        });

        // General Ticket Actions
        binding.btnCreateTicket.setOnClickListener(v -> openCreateTicket(null));
        binding.btnViewTickets.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.customerTicketsFragment)
        );

        // Quick Category Cards
        binding.cardQuickRefund.setOnClickListener(v -> openCreateTicket(SupportTicket.CATEGORY_REFUND_REQUEST));
        binding.cardQuickBilling.setOnClickListener(v -> openCreateTicket(SupportTicket.CATEGORY_BILLING_DISPUTE));
        binding.cardQuickRideProblem.setOnClickListener(v -> openCreateTicket(SupportTicket.CATEGORY_RIDE_PROBLEM));
        binding.cardQuickUnsafe.setOnClickListener(v -> openCreateTicket(SupportTicket.CATEGORY_UNSAFE_BICYCLE));
    }

    private void openCreateTicket(String preselectedCategory) {
        Bundle bundle = new Bundle();
        if (preselectedCategory != null) {
            bundle.putString("category", preselectedCategory);
        }
        Navigation.findNavController(requireView()).navigate(R.id.createTicketFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
