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
                            && a.getType().equals(b.getType());
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
            binding.tvBicycleId.setText("ID: " + bicycle.getBicycleId().substring(0, 8).toUpperCase());
            binding.tvBicycleType.setText(bicycle.getType() != null ? bicycle.getType() : "Standard");
            binding.tvStatus.setText(bicycle.isAvailable() ? "Available" : "In Use");

            int statusColor = bicycle.isAvailable()
                    ? binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorAvailable)
                    : binding.getRoot().getContext().getColor(com.sanika.bicyclebookingapp.R.color.colorUnavailable);
            binding.tvStatus.setTextColor(statusColor);

            binding.btnBook.setEnabled(bicycle.isAvailable());
            binding.btnBook.setOnClickListener(v -> listener.onBicycleClick(bicycle));
        }
    }
}
