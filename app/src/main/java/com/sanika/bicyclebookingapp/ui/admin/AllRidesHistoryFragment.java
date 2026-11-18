package com.sanika.bicyclebookingapp.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sanika.bicyclebookingapp.databinding.FragmentAllRidesHistoryBinding;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.ui.history.adapter.BookingHistoryAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen — shows ALL rides from ALL users in real-time.
 * Includes filter buttons: All / Active / Completed / Cancelled
 */
public class AllRidesHistoryFragment extends Fragment {

    private FragmentAllRidesHistoryBinding binding;
    private BookingHistoryAdapter adapter;
    private String currentFilter = "ALL";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAllRidesHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // No cancel/delete actions for admin view
        adapter = new BookingHistoryAdapter();
        binding.rvAllRides.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAllRides.setAdapter(adapter);

        // Filter buttons
        binding.btnFilterAll.setOnClickListener(v -> loadRides("ALL"));
        binding.btnFilterActive.setOnClickListener(v -> loadRides("ACTIVE"));
        binding.btnFilterCompleted.setOnClickListener(v -> loadRides("COMPLETED"));
        binding.btnFilterCancelled.setOnClickListener(v -> loadRides("CANCELLED"));

        // Load all rides by default
        loadRides("ALL");
    }

    private void loadRides(String filter) {
        currentFilter = filter;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        // Update button styles
        int active   = requireContext().getColor(com.sanika.bicyclebookingapp.R.color.colorPrimary);
        int inactive = requireContext().getColor(com.sanika.bicyclebookingapp.R.color.colorTextSecondary);
        binding.btnFilterAll.setTextColor("ALL".equals(filter) ? active : inactive);
        binding.btnFilterActive.setTextColor("ACTIVE".equals(filter) ? active : inactive);
        binding.btnFilterCompleted.setTextColor("COMPLETED".equals(filter) ? active : inactive);
        binding.btnFilterCancelled.setTextColor("CANCELLED".equals(filter) ? active : inactive);

        Query query = FirebaseFirestore.getInstance()
                .collection("bookings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100);

        if (!"ALL".equals(filter)) {
            query = query.whereEqualTo("status", filter);
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);

            if (error != null) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.tvEmpty.setText("Error: " + error.getMessage());
                return;
            }

            List<Booking> bookings = new ArrayList<>();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Booking booking = doc.toObject(Booking.class);
                    bookings.add(booking);
                }
            }

            adapter.submitList(bookings);
            binding.tvCount.setText(bookings.size() + " rides");
            binding.tvEmpty.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
