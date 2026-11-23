package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAdminMaintenanceBinding;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Adapter for Admin Bicycle Maintenance requests with priority tags, status badges,
 * and quick workflow actions.
 */
public class AdminMaintenanceAdapter extends ListAdapter<MaintenanceRecord, AdminMaintenanceAdapter.ViewHolder> {

    public interface OnMaintenanceActionListener {
        void onManageAction(MaintenanceRecord record);
        void onViewBikeHistory(String bicycleId);
    }

    private final OnMaintenanceActionListener actionListener;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    public AdminMaintenanceAdapter(OnMaintenanceActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    private static final DiffUtil.ItemCallback<MaintenanceRecord> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MaintenanceRecord>() {
                @Override
                public boolean areItemsTheSame(@NonNull MaintenanceRecord a, @NonNull MaintenanceRecord b) {
                    return a.getRecordId() != null && a.getRecordId().equals(b.getRecordId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MaintenanceRecord a, @NonNull MaintenanceRecord b) {
                    String statusA = a.getStatus() != null ? a.getStatus() : "";
                    String statusB = b.getStatus() != null ? b.getStatus() : "";
                    long costA = a.getCostPaise();
                    long costB = b.getCostPaise();
                    return statusA.equals(statusB) && costA == costB;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminMaintenanceBinding binding = ItemAdminMaintenanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), actionListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminMaintenanceBinding binding;

        ViewHolder(ItemAdminMaintenanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MaintenanceRecord record, OnMaintenanceActionListener actionListener) {
            String bikeIdStr = "Bike: " + (record.getBicycleId() != null ? record.getBicycleId() : "—");
            binding.tvBicycleId.setText(bikeIdStr);

            // Priority Badge
            String priority = record.getPriority() != null ? record.getPriority().toUpperCase() : MaintenanceRecord.PRIORITY_MEDIUM;
            binding.tvPriorityBadge.setText(priority);
            switch (priority) {
                case MaintenanceRecord.PRIORITY_CRITICAL:
                    binding.tvPriorityBadge.setBackgroundColor(Color.parseColor("#FFCDD2"));
                    binding.tvPriorityBadge.setTextColor(Color.parseColor("#B71C1C"));
                    break;
                case MaintenanceRecord.PRIORITY_HIGH:
                    binding.tvPriorityBadge.setBackgroundColor(Color.parseColor("#FFE0B2"));
                    binding.tvPriorityBadge.setTextColor(Color.parseColor("#E65100"));
                    break;
                case MaintenanceRecord.PRIORITY_LOW:
                    binding.tvPriorityBadge.setBackgroundColor(Color.parseColor("#E0E0E0"));
                    binding.tvPriorityBadge.setTextColor(Color.parseColor("#616161"));
                    break;
                case MaintenanceRecord.PRIORITY_MEDIUM:
                default:
                    binding.tvPriorityBadge.setBackgroundColor(Color.parseColor("#BBDEFB"));
                    binding.tvPriorityBadge.setTextColor(Color.parseColor("#0D47A1"));
                    break;
            }

            // Status Badge
            String status = record.getStatus() != null ? record.getStatus().toUpperCase() : MaintenanceRecord.STATUS_REPORTED;
            binding.tvStatusBadge.setText(status);
            switch (status) {
                case MaintenanceRecord.STATUS_REPORTED:
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FFF9C4"));
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#F57F17"));
                    break;
                case MaintenanceRecord.STATUS_ASSIGNED:
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#E1BEE7"));
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#6A1B9A"));
                    break;
                case MaintenanceRecord.STATUS_IN_PROGRESS:
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FFE0B2"));
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#E65100"));
                    break;
                case MaintenanceRecord.STATUS_COMPLETED:
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#C8E6C9"));
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#1B5E20"));
                    break;
                case MaintenanceRecord.STATUS_VERIFIED:
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#Dcedc8"));
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#33691E"));
                    break;
                case MaintenanceRecord.STATUS_REJECTED:
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FFCDD2"));
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#B71C1C"));
                    break;
                default:
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#EEEEEE"));
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#424242"));
                    break;
            }

            // Unsafe Banner
            binding.tvUnsafeBanner.setVisibility(record.isUnsafe() ? View.VISIBLE : View.GONE);

            // Category & Date
            binding.tvCategory.setText(record.getDamageCategory() != null ? record.getDamageCategory() : "Issue");
            if (record.getReportedAt() != null) {
                binding.tvReportedDate.setText(DATE_FORMAT.format(record.getReportedAt()));
            } else {
                binding.tvReportedDate.setText("Recently");
            }

            // Description
            binding.tvDescription.setText(record.getDescription() != null && !record.getDescription().isEmpty()
                    ? record.getDescription() : "No detailed problem description provided.");

            // Reporter
            String reporter = "Reported by: " + (record.getReporterName() != null ? record.getReporterName() : "Customer");
            if (record.getReporterPhone() != null && !record.getReporterPhone().isEmpty()) {
                reporter += " (" + record.getReporterPhone() + ")";
            }
            binding.tvReporterInfo.setText(reporter);

            // Staff
            if (record.getAssignedStaff() != null && !record.getAssignedStaff().isEmpty()) {
                binding.tvStaffInfo.setText("Assigned: " + record.getAssignedStaff());
                binding.tvStaffInfo.setVisibility(View.VISIBLE);
            } else {
                binding.tvStaffInfo.setText("Assigned: Unassigned");
                binding.tvStaffInfo.setVisibility(View.VISIBLE);
            }

            // Cost
            if (record.getCostPaise() > 0) {
                binding.layoutCost.setVisibility(View.VISIBLE);
                binding.tvRepairCost.setText(record.getFormattedCost());
            } else {
                binding.layoutCost.setVisibility(View.GONE);
            }

            // Action listeners
            binding.btnManage.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onManageAction(record);
            });

            binding.btnBikeHistory.setOnClickListener(v -> {
                if (actionListener != null && record.getBicycleId() != null) {
                    actionListener.onViewBikeHistory(record.getBicycleId());
                }
            });
        }
    }
}
