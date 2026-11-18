package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAdminStationBinding;
import com.sanika.bicyclebookingapp.model.Station;

/**
 * Admin adapter for station list with edit/delete actions.
 */
public class AdminStationAdapter extends ListAdapter<Station, AdminStationAdapter.ViewHolder> {

    public interface OnEditListener  { void onEdit(Station station); }
    public interface OnDeleteListener { void onDelete(Station station); }

    private final OnEditListener editListener;
    private final OnDeleteListener deleteListener;

    public AdminStationAdapter(OnEditListener editListener, OnDeleteListener deleteListener) {
        super(DIFF_CALLBACK);
        this.editListener   = editListener;
        this.deleteListener = deleteListener;
    }

    private static final DiffUtil.ItemCallback<Station> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Station>() {
                @Override
                public boolean areItemsTheSame(@NonNull Station a, @NonNull Station b) {
                    return a.getStationId().equals(b.getStationId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Station a, @NonNull Station b) {
                    return a.getName().equals(b.getName())
                            && a.getAvailableBicycles() == b.getAvailableBicycles();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminStationBinding binding = ItemAdminStationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), editListener, deleteListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminStationBinding binding;

        ViewHolder(ItemAdminStationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Station station, OnEditListener editListener, OnDeleteListener deleteListener) {
            binding.tvStationName.setText(station.getName());
            binding.tvAddress.setText(station.getAddress());
            binding.tvBicycleCount.setText(
                    station.getAvailableBicycles() + "/" + station.getTotalBicycles() + " available");
            binding.btnEdit.setOnClickListener(v -> editListener.onEdit(station));
            binding.btnDelete.setOnClickListener(v -> deleteListener.onDelete(station));
        }
    }
}
