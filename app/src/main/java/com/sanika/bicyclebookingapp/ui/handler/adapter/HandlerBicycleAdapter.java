package com.sanika.bicyclebookingapp.ui.handler.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ItemHandlerBicycleBinding;
import com.sanika.bicyclebookingapp.model.Bicycle;

public class HandlerBicycleAdapter extends ListAdapter<Bicycle, HandlerBicycleAdapter.ViewHolder> {

    public interface OnMaintenanceToggleListener {
        void onToggle(Bicycle bicycle);
    }

    private final OnMaintenanceToggleListener toggleListener;

    public HandlerBicycleAdapter(OnMaintenanceToggleListener toggleListener) {
        super(DIFF_CALLBACK);
        this.toggleListener = toggleListener;
    }

    private static final DiffUtil.ItemCallback<Bicycle> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Bicycle oldItem, @NonNull Bicycle newItem) {
                    return oldItem.getBicycleId() != null &&
                            oldItem.getBicycleId().equals(newItem.getBicycleId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Bicycle oldItem, @NonNull Bicycle newItem) {
                    return oldItem.isAvailable() == newItem.isAvailable() &&
                            oldItem.isUnderMaintenance() == newItem.isUnderMaintenance();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHandlerBicycleBinding binding = ItemHandlerBicycleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHandlerBicycleBinding binding;

        ViewHolder(ItemHandlerBicycleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Bicycle bicycle) {
            binding.tvBicycleType.setText(bicycle.getType() != null ? bicycle.getType() : "Bicycle");
            String shortId = bicycle.getBicycleId() != null && bicycle.getBicycleId().length() > 6
                    ? bicycle.getBicycleId().substring(0, 6).toUpperCase()
                    : (bicycle.getBicycleId() != null ? bicycle.getBicycleId() : "—");
            binding.tvBicycleId.setText("ID: #" + shortId);

            if (bicycle.isUnderMaintenance()) {
                binding.tvStatus.setText("Under Maintenance");
                binding.tvStatus.setTextColor(itemView.getContext().getColor(R.color.colorUnavailable));
                binding.btnToggleMaintenance.setText("Set Active");
            } else if (bicycle.isAvailable()) {
                binding.tvStatus.setText("Available");
                binding.tvStatus.setTextColor(itemView.getContext().getColor(R.color.colorAvailable));
                binding.btnToggleMaintenance.setText("Mark Repair");
            } else {
                binding.tvStatus.setText("In Active Ride");
                binding.tvStatus.setTextColor(itemView.getContext().getColor(R.color.colorAccent));
                binding.btnToggleMaintenance.setText("Flag Issue");
            }

            binding.btnToggleMaintenance.setOnClickListener(v -> {
                if (toggleListener != null) {
                    toggleListener.onToggle(bicycle);
                }
            });
        }
    }
}
