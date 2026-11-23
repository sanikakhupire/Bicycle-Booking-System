package com.sanika.bicyclebookingapp.ui.support;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.FragmentCustomerTicketsBinding;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.ui.support.adapter.CustomerTicketAdapter;
import com.sanika.bicyclebookingapp.util.Resource;
import com.sanika.bicyclebookingapp.viewmodel.SupportViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer Grievances & Support Tickets List screen.
 */
public class CustomerTicketsFragment extends Fragment {

    private FragmentCustomerTicketsBinding binding;
    private SupportViewModel supportViewModel;
    private CustomerTicketAdapter adapter;

    private List<SupportTicket> allTickets = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerTicketsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        supportViewModel = new ViewModelProvider(this).get(SupportViewModel.class);

        binding.toolbarCustomerTickets.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        setupRecyclerView();
        setupFilters();
        loadTickets();

        binding.fabNewTicket.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.createTicketFragment)
        );
    }

    private void setupRecyclerView() {
        adapter = new CustomerTicketAdapter(this::openTicketDetail);
        binding.rvCustomerTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomerTickets.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupCustomerTicketStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCustTicketOpen) {
                currentFilter = "ACTIVE";
            } else if (checkedId == R.id.chipCustTicketWaiting) {
                currentFilter = SupportTicket.STATUS_WAITING_FOR_CUSTOMER;
            } else if (checkedId == R.id.chipCustTicketResolved) {
                currentFilter = SupportTicket.STATUS_RESOLVED;
            } else if (checkedId == R.id.chipCustTicketClosed) {
                currentFilter = SupportTicket.STATUS_CLOSED;
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void loadTickets() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(requireContext(), "Please log in to view tickets", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBarCustTickets.setVisibility(View.VISIBLE);

        supportViewModel.getCustomerTickets(uid).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            binding.progressBarCustTickets.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                allTickets = resource.data;
                applyFilter();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        List<SupportTicket> filtered = new ArrayList<>();

        for (SupportTicket t : allTickets) {
            String status = t.getStatus() != null ? t.getStatus().toUpperCase() : SupportTicket.STATUS_OPEN;
            boolean matches;

            if ("ALL".equals(currentFilter)) {
                matches = true;
            } else if ("ACTIVE".equals(currentFilter)) {
                matches = SupportTicket.STATUS_OPEN.equalsIgnoreCase(status)
                        || SupportTicket.STATUS_ASSIGNED.equalsIgnoreCase(status)
                        || SupportTicket.STATUS_IN_PROGRESS.equalsIgnoreCase(status)
                        || SupportTicket.STATUS_REOPENED.equalsIgnoreCase(status);
            } else {
                matches = currentFilter.equalsIgnoreCase(status);
            }

            if (matches) {
                filtered.add(t);
            }
        }

        adapter.setTickets(filtered);
        binding.tvEmptyCustTickets.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openTicketDetail(SupportTicket ticket) {
        Bundle bundle = new Bundle();
        bundle.putString("ticketId", ticket.getTicketId());
        bundle.putString("subject", ticket.getSubject());
        bundle.putString("category", ticket.getCategory());
        bundle.putString("priority", ticket.getPriority());
        bundle.putString("status", ticket.getStatus());
        bundle.putString("assignedStaff", ticket.getAssignedStaff());
        bundle.putString("bookingId", ticket.getBookingId());
        bundle.putString("bicycleId", ticket.getBicycleId());
        bundle.putLong("disputedPaise", ticket.getDisputedAmountPaise());
        bundle.putString("resolutionSummary", ticket.getResolutionSummary());

        Navigation.findNavController(requireView()).navigate(R.id.ticketDetailFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
