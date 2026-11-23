package com.sanika.bicyclebookingapp.ui.support.adapter;

import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sanika.bicyclebookingapp.R;
import com.sanika.bicyclebookingapp.databinding.ItemTicketMessageBinding;
import com.sanika.bicyclebookingapp.model.TicketMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for threaded conversation messages in a Support Ticket.
 */
public class TicketMessageAdapter extends RecyclerView.Adapter<TicketMessageAdapter.ViewHolder> {

    private final List<TicketMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a, dd MMM", Locale.US);

    public TicketMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<TicketMessage> newMessages) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTicketMessageBinding binding = ItemTicketMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTicketMessageBinding binding;

        ViewHolder(ItemTicketMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TicketMessage msg) {
            boolean isFromMe = (currentUserId != null && currentUserId.equals(msg.getSenderId()))
                    || TicketMessage.SENDER_CUSTOMER.equalsIgnoreCase(msg.getSenderRole());

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.layoutMessageBubble.getLayoutParams();

            if (isFromMe) {
                // Outgoing (Customer)
                params.gravity = Gravity.END;
                binding.layoutMessageBubble.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_chat_outgoing));
                binding.tvSenderName.setText(msg.getSenderName() != null ? msg.getSenderName() : "You");
                binding.tvSenderName.setTextColor(Color.parseColor("#1B5E20"));
            } else {
                // Incoming (Support / Admin)
                params.gravity = Gravity.START;
                binding.layoutMessageBubble.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_chat_incoming));
                String roleTag = TicketMessage.SENDER_ADMIN.equalsIgnoreCase(msg.getSenderRole()) ? "Admin Operations" : "Customer Support";
                binding.tvSenderName.setText((msg.getSenderName() != null ? msg.getSenderName() : "Support") + " (" + roleTag + ")");
                binding.tvSenderName.setTextColor(Color.parseColor("#E65100"));
            }
            binding.layoutMessageBubble.setLayoutParams(params);

            binding.tvMessageBody.setText(msg.getMessage() != null ? msg.getMessage() : "");

            if (msg.getCreatedAt() != null) {
                binding.tvMessageTime.setText(timeFormat.format(msg.getCreatedAt()));
            } else {
                binding.tvMessageTime.setText("");
            }

            if (msg.getAttachmentUrl() != null && !msg.getAttachmentUrl().isEmpty()) {
                binding.ivMessageAttachment.setVisibility(View.VISIBLE);
                try {
                    binding.ivMessageAttachment.setImageURI(Uri.parse(msg.getAttachmentUrl()));
                } catch (Exception ignored) {
                    binding.ivMessageAttachment.setImageResource(R.drawable.ic_bicycle);
                }
            } else {
                binding.ivMessageAttachment.setVisibility(View.GONE);
            }
        }
    }
}
