package com.sanika.bicyclebookingapp.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemStationCardBinding;
import com.sanika.bicyclebookingapp.model.Station;

/**
 * RecyclerView adapter for the horizontal station cards on the Home screen.
 */
public class StationAdapter extends ListAdapter<Station, StationAdapter.StationViewHolder> {

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    private final OnStationClickListener listener;

    public StationAdapter(OnStationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Station> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Station>() {
                @Override
                public boolean areItemsTheSame(@NonNull Station a, @NonNull Station b) {
                    return a.getStationId().equals(b.getStationId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Station a, @NonNull Station b) {
                    return a.getAvailableBicycles() == b.getAvailableBicycles()
                            && a.getName().equals(b.getName());
                }
            };

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStationCardBinding binding = ItemStationCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class StationViewHolder extends RecyclerView.ViewHolder {

        private final ItemStationCardBinding binding;

        StationViewHolder(ItemStationCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Station station, OnStationClickListener listener) {
            binding.tvStationName.setText(station.getName());
            binding.tvAvailableCount.setText(station.getAvailableBicycles() + " available");
            binding.tvAddress.setText(station.getAddress());

            // Color indicator
            int color = station.getAvailableBicycles() > 0
                    ? binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorAvailable)
                    : binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorUnavailable);
            binding.viewAvailabilityDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color));

            binding.getRoot().setOnClickListener(v -> listener.onStationClick(station));
        }
    }
}
