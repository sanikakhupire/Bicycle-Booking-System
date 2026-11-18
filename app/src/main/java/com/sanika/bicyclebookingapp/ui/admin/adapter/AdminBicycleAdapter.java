package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ItemAdminBicycleBinding;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.util.QrCodeUtil;

/**
 * Admin adapter for bicycle list with QR display, toggle availability, and delete.
 */
public class AdminBicycleAdapter extends ListAdapter<Bicycle, AdminBicycleAdapter.ViewHolder> {

    public interface OnToggleListener { void onToggle(Bicycle bicycle); }
    public interface OnDeleteListener { void onDelete(Bicycle bicycle); }

    private final OnToggleListener toggleListener;
    private final OnDeleteListener deleteListener;

    public AdminBicycleAdapter(OnToggleListener toggleListener, OnDeleteListener deleteListener) {
        super(DIFF_CALLBACK);
        this.toggleListener = toggleListener;
        this.deleteListener = deleteListener;
    }

    private static final DiffUtil.ItemCallback<Bicycle> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Bicycle>() {
                @Override
                public boolean areItemsTheSame(@NonNull Bicycle a, @NonNull Bicycle b) {
                    return a.getBicycleId().equals(b.getBicycleId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Bicycle a, @NonNull Bicycle b) {
                    return a.isAvailable() == b.isAvailable();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminBicycleBinding binding = ItemAdminBicycleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), toggleListener, deleteListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminBicycleBinding binding;

        ViewHolder(ItemAdminBicycleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Bicycle bicycle, OnToggleListener toggleListener, OnDeleteListener deleteListener) {
            binding.tvBicycleId.setText("ID: " + bicycle.getBicycleId().substring(0, 8).toUpperCase());
            binding.tvType.setText(bicycle.getType() != null ? bicycle.getType() : "Standard");
            binding.tvStation.setText(bicycle.getStationName() != null ? bicycle.getStationName() : "—");

            boolean available = bicycle.isAvailable();
            binding.tvStatus.setText(available ? "Available" : "In Use");
            int color = available
                    ? binding.getRoot().getContext().getColor(R.color.colorAvailable)
                    : binding.getRoot().getContext().getColor(R.color.colorUnavailable);
            binding.tvStatus.setTextColor(color);

            binding.btnToggle.setText(available ? "Mark In Use" : "Mark Available");
            binding.btnToggle.setOnClickListener(v -> toggleListener.onToggle(bicycle));
            binding.btnDelete.setOnClickListener(v -> deleteListener.onDelete(bicycle));

            // Show QR code dialog
            binding.btnShowQr.setOnClickListener(v -> showQrDialog(bicycle));
        }

        private void showQrDialog(Bicycle bicycle) {
            // Generate QR bitmap from the bicycle's qrCode field (= bicycleId)
            String qrContent = bicycle.getQrCode() != null
                    ? bicycle.getQrCode()
                    : bicycle.getBicycleId();

            Bitmap qrBitmap = QrCodeUtil.generateQrCode(qrContent, 600);
            if (qrBitmap == null) return;

            // Build a simple dialog showing the QR code
            Dialog dialog = new Dialog(binding.getRoot().getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_show_qr);

            ImageView ivQr = dialog.findViewById(R.id.ivQrCode);
            TextView tvBicycleId = dialog.findViewById(R.id.tvQrBicycleId);
            TextView tvQrValue = dialog.findViewById(R.id.tvQrValue);

            ivQr.setImageBitmap(qrBitmap);
            tvBicycleId.setText("Bicycle: " + bicycle.getBicycleId().substring(0, 8).toUpperCase()
                    + " (" + (bicycle.getType() != null ? bicycle.getType() : "Standard") + ")");
            tvQrValue.setText("ID: " + qrContent);

            dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }
}
