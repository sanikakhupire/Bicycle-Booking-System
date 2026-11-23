package com.sanika.bicyclebookingapp.ui.admin.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.databinding.ItemAdminCustomerBinding;
import com.sanika.bicyclebookingapp.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Customer Management list in the admin panel.
 */
public class AdminCustomerAdapter extends RecyclerView.Adapter<AdminCustomerAdapter.ViewHolder> {

    public interface OnRoleChangeListener {
        void onRoleChange(User user);
    }

    private final List<User> users = new ArrayList<>();
    private final OnRoleChangeListener listener;

    public AdminCustomerAdapter(OnRoleChangeListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> newUsers) {
        this.users.clear();
        if (newUsers != null) {
            this.users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminCustomerBinding binding = ItemAdminCustomerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminCustomerBinding binding;

        ViewHolder(ItemAdminCustomerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.tvCustomerName.setText(user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Anonymous User");
            binding.tvCustomerEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
            binding.tvCustomerPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "No phone registered");
            binding.tvCustomerUid.setText("UID: " + (user.getUid() != null ? user.getUid() : ""));

            String role = user.getRole() != null ? user.getRole().toUpperCase() : (user.isAdmin() ? "ADMIN" : "USER");
            binding.tvCustomerRoleBadge.setText(role);

            if ("ADMIN".equals(role)) {
                binding.tvCustomerRoleBadge.setBackgroundColor(Color.parseColor("#EDE7F6"));
                binding.tvCustomerRoleBadge.setTextColor(Color.parseColor("#512DA8"));
            } else if ("HANDLER".equals(role)) {
                binding.tvCustomerRoleBadge.setBackgroundColor(Color.parseColor("#E0F7FA"));
                binding.tvCustomerRoleBadge.setTextColor(Color.parseColor("#006064"));
            } else {
                binding.tvCustomerRoleBadge.setBackgroundColor(Color.parseColor("#E8F5E9"));
                binding.tvCustomerRoleBadge.setTextColor(Color.parseColor("#2E7D32"));
            }

            binding.btnChangeRole.setOnClickListener(v -> {
                if (listener != null) listener.onRoleChange(user);
            });
        }
    }
}
