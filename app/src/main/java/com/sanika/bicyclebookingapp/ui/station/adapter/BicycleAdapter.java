package com.sanika.bicyclebookingapp.ui.station.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemBicycleBinding;
import com.sanika.bicyclebookingapp.model.Bicycle;

/**
 * RecyclerView adapter for the list of bicycles at a station.
 */
public class BicycleAdapter extends ListAdapter<Bicycle, BicycleAdapter.BicycleViewHolder> {

    public interface OnBicycleClickListener {
        void onBicycleClick(Bicycle bicycle);
        void onBicycleDetails(Bicycle bicycle);
    }

    private final OnBicycleClickListener listener;

    public BicycleAdapter(OnBicycleClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Bicycle> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Bicycle>() {
                @Override
                public boolean areItemsTheSame(@NonNull Bicycle a, @NonNull Bicycle b) {
                    return a.getBicycleId().equals(b.getBicycleId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Bicycle a, @NonNull Bicycle b) {
                    return a.isAvailable() == b.isAvailable()
                            && (a.getStatus() != null ? a.getStatus().equals(b.getStatus()) : true)
                            && (a.getType() != null ? a.getType().equals(b.getType()) : true);
                }
            };

    @NonNull
    @Override
    public BicycleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBicycleBinding binding = ItemBicycleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BicycleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BicycleViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class BicycleViewHolder extends RecyclerView.ViewHolder {

        private final ItemBicycleBinding binding;

        BicycleViewHolder(ItemBicycleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Bicycle bicycle, OnBicycleClickListener listener) {
            String shortId = bicycle.getBicycleId() != null && bicycle.getBicycleId().length() >= 8
                    ? bicycle.getBicycleId().substring(0, 8).toUpperCase()
                    : (bicycle.getBicycleId() != null ? bicycle.getBicycleId() : "—");
            binding.tvBicycleId.setText("Bike #" + shortId);
            
            String bikeType = bicycle.getType() != null ? bicycle.getType() : "Standard";
            binding.tvBicycleType.setText("Type: " + bikeType);

            boolean isAvailable = bicycle.isAvailable();
            String status = bicycle.getStatus();
            int statusColor;
            String statusText;

            if (Bicycle.STATUS_AVAILABLE.equalsIgnoreCase(status)) {
                statusText = "Available for Ride";
                statusColor = binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorAvailable);
            } else if (Bicycle.STATUS_RESERVED.equalsIgnoreCase(status)) {
                statusText = "Reserved (Hold)";
                statusColor = binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorAccent);
            } else if (Bicycle.STATUS_IN_USE.equalsIgnoreCase(status)) {
                statusText = "In Use";
                statusColor = binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorUnavailable);
            } else if (Bicycle.STATUS_MAINTENANCE.equalsIgnoreCase(status)) {
                statusText = "Under Maintenance";
                statusColor = binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorUnavailable);
            } else if (Bicycle.STATUS_DAMAGED.equalsIgnoreCase(status)) {
                statusText = "Reported Damaged";
                statusColor = binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorUnavailable);
            } else {
                statusText = isAvailable ? "Available" : "Unavailable";
                statusColor = isAvailable
                        ? binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorAvailable)
                        : binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorUnavailable);
            }

            binding.tvStatus.setText(statusText);
            binding.tvStatus.setTextColor(statusColor);

            binding.tvRate.setText("₹5 base + ₹8/km + ₹1/min");

            binding.btnBook.setEnabled(isAvailable);
            binding.btnBook.setOnClickListener(v -> {
                if (listener != null) listener.onBicycleClick(bicycle);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onBicycleDetails(bicycle);
            });
        }
    }
}
