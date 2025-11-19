package com.example.smart_air.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.Invite;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class InvitesAdapter extends RecyclerView.Adapter<InvitesAdapter.InviteViewHolder> {

    private List<Invite> invitesList;
    private OnInviteActionListener listener;

    public interface OnInviteActionListener {
        void onCopyCode(Invite invite);
        void onRevokeInvite(Invite invite);
        void onRegenerateInvite(Invite invite);
    }

    public InvitesAdapter(List<Invite> invitesList, OnInviteActionListener listener) {
        this.invitesList = invitesList;
        this.listener = listener;
    }

    // Constructor with individual listeners for backward compatibility
    public InvitesAdapter(List<Invite> invitesList,
                          OnCopyCodeListener copyListener,
                          OnRevokeListener revokeListener,
                          OnRegenerateListener regenerateListener) {
        this.invitesList = invitesList;
        this.listener = new OnInviteActionListener() {
            @Override
            public void onCopyCode(Invite invite) {
                copyListener.onCopy(invite);
            }

            @Override
            public void onRevokeInvite(Invite invite) {
                revokeListener.onRevoke(invite);
            }

            @Override
            public void onRegenerateInvite(Invite invite) {
                regenerateListener.onRegenerate(invite);
            }
        };
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invite, parent, false);
        return new InviteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        Invite invite = invitesList.get(position);
        holder.bind(invite, listener);
    }

    @Override
    public int getItemCount() {
        return invitesList.size();
    }

    static class InviteViewHolder extends RecyclerView.ViewHolder {
        private TextView tvInviteCode;
        private TextView tvExpiresAt;
        private TextView tvStatus;
        private Button btnCopy;
        private Button btnRevoke;
        private Button btnRegenerate;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInviteCode = itemView.findViewById(R.id.tvInviteCode);
            tvExpiresAt = itemView.findViewById(R.id.tvExpiresAt);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnRevoke = itemView.findViewById(R.id.btnRevoke);
            btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
        }

        public void bind(Invite invite, OnInviteActionListener listener) {
            tvInviteCode.setText(invite.getCode());

            // Format expiration date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            String expiryText = "Expires: " + sdf.format(invite.getExpiresAt());
            tvExpiresAt.setText(expiryText);

            // Show status
            boolean isExpired = invite.getExpiresAt() < System.currentTimeMillis();

            if (invite.isUsed()) {
                tvStatus.setText("Used");
                tvStatus.setVisibility(View.VISIBLE);
            } else if (isExpired) {
                tvStatus.setText("Expired");
                tvStatus.setVisibility(View.VISIBLE);
            } else {
                tvStatus.setText("Active");
                tvStatus.setVisibility(View.VISIBLE);
            }

            // Set button actions
            btnCopy.setOnClickListener(v -> listener.onCopyCode(invite));
            btnRevoke.setOnClickListener(v -> listener.onRevokeInvite(invite));
            btnRegenerate.setOnClickListener(v -> listener.onRegenerateInvite(invite));
        }
    }

    // Individual listener interfaces for backward compatibility
    public interface OnCopyCodeListener {
        void onCopy(Invite invite);
    }

    public interface OnRevokeListener {
        void onRevoke(Invite invite);
    }

    public interface OnRegenerateListener {
        void onRegenerate(Invite invite);
    }
}