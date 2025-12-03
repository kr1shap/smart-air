package com.example.smart_air.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.Notification;

import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    List<Notification> list;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationsAdapter(List<Notification> list,  OnNotificationClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()) //inflate
                .inflate(R.layout.notification_card, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int i) {
        Notification n = list.get(i); //bind the notification
        holder.cardView.setAlpha(1f); //reset fade
        holder.title.setText(Notification.getTitle(n.getNotifType()));
        holder.description.setText(Notification.getDescription(n.getNotifType()));
        holder.childName.setText(n.getChildName());
        setNotifSymbol(holder, n);
        holder.date.setText(Notification.convertToDate(n.getTimestamp()));
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                holder.cardView.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> listener.onNotificationClick(n));
            }
        });

    }

    //changes symbol
    private void setNotifSymbol(NotificationViewHolder holder, Notification n) {
        switch(n.getNotifType()) {
            // INVENTORY, TRIAGE, WORSE_DOSE, RAPID_RESCUE, RED_ZONE
            case INVENTORY:
                holder.symbol.setImageResource(R.drawable.baseline_inventory_24);
                break;
            case TRIAGE:
            case RED_ZONE:
                holder.symbol.setImageResource(R.drawable.outline_e911_emergency_24);
                break;
            case WORSE_DOSE:
                holder.symbol.setImageResource(R.drawable.outline_exclamation_24);
                break;
            case RAPID_RESCUE:
                holder.symbol.setImageResource(R.drawable.outline_medication_24);
                break;
            default:
                holder.symbol.setImageResource(R.drawable.baseline_inventory_24);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView childName, title, description, date;
        ImageView symbol;
        CardView cardView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            //instantiate all items in onboarding layout
            title = itemView.findViewById(R.id.notifTitle);
            description = itemView.findViewById(R.id.notifDescription);
            childName = itemView.findViewById(R.id.childName);
            cardView = itemView.findViewById(R.id.notificationCard);
            symbol = itemView.findViewById(R.id.notifIcon);
            date = itemView.findViewById(R.id.date);
        }
    }
}
