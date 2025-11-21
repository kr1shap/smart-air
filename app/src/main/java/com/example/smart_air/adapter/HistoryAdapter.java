package com.example.smart_air.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.HistoryItem;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<HistoryItem> historyItems;

    public HistoryAdapter(List<HistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_dailycheckin, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem card = historyItems.get(position);

        // date
        holder.dateText.setText(card.date);

        // child/parent text & bars
        switch (card.cardType) {
            case childOnly:
                holder.childText.setVisibility(View.VISIBLE);
                holder.parentText.setVisibility(View.INVISIBLE);

                holder.childActivityLimitsBar.setProgress(card.activityChild);
                holder.childActivityLimitsBar.setVisibility(View.VISIBLE);
                holder.parentActivityLimitsBar.setVisibility(View.INVISIBLE);
                holder.childActivityLimitsBar.setProgressTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.role_default_bg))
                );

                holder.childCoughingBar.setProgress(card.coughingChild);
                holder.childCoughingBar.setVisibility(View.VISIBLE);
                holder.parentCoughingBar.setVisibility(View.INVISIBLE);
                holder.childCoughingBar.setProgressTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.role_default_bg))
                );
                break;

            case parentOnly:
                holder.childText.setVisibility(View.INVISIBLE);
                holder.parentText.setVisibility(View.VISIBLE);

                holder.childActivityLimitsBar.setProgress(card.activityParent);
                holder.parentActivityLimitsBar.setVisibility(View.INVISIBLE);
                holder.childActivityLimitsBar.setVisibility(View.VISIBLE);
                holder.childActivityLimitsBar.setProgressTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.role_selected_bg))
                );

                holder.childCoughingBar.setProgress(card.coughingParent);
                holder.parentCoughingBar.setVisibility(View.INVISIBLE);
                holder.childCoughingBar.setVisibility(View.VISIBLE);
                holder.childCoughingBar.setProgressTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.role_selected_bg))
                );
                break;

            case both:
                holder.childText.setVisibility(View.VISIBLE);
                holder.parentText.setVisibility(View.VISIBLE);

                holder.childActivityLimitsBar.setProgress(card.activityChild);
                holder.parentActivityLimitsBar.setProgress(card.activityParent);
                holder.childActivityLimitsBar.setVisibility(View.VISIBLE);
                holder.parentActivityLimitsBar.setVisibility(View.VISIBLE);
                holder.childActivityLimitsBar.setProgressTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.role_default_bg))
                );

                holder.childCoughingBar.setProgress(card.coughingChild);
                holder.parentCoughingBar.setProgress(card.coughingParent);
                holder.childCoughingBar.setVisibility(View.VISIBLE);
                holder.parentCoughingBar.setVisibility(View.VISIBLE);
                holder.childCoughingBar.setProgressTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.role_default_bg))
                );
                break;
        }

        // other fields
        holder.nightTerrorsStatus.setText(card.nightStatus);
        holder.activityLimitsStatus.setText(card.activityStatus);
        holder.coughingStatus.setText(card.coughingStatus);
        if(card.zone.isEmpty()){
            holder.colourBox.setVisibility(View.GONE);
            holder.zoneStatus.setVisibility(View.VISIBLE);
            holder.zoneStatus.setText("NOT ENTERED");
        }
        else{
            holder.zoneStatus.setVisibility(View.GONE);
            holder.colourBox.setVisibility(View.VISIBLE);
            holder.colourBox.setBackgroundColor(getColour(card.zone));
        }

        // triggers
        holder.setChips(card.triggers);
    }

    private int getColour(String zone) {
        switch (zone) {
            case "green":
                return Color.parseColor("#4CAF50"); // green

            case "yellow":
                return Color.parseColor("#FFEB3B"); // yellow

            case "red":
                return Color.parseColor("#F44336"); // red

            default:
                return Color.GRAY; // safety, won't be called though
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    // method to update filtered list
    public void updateList(List<HistoryItem> newList) {
        historyItems = newList;
        notifyDataSetChanged();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, childText, parentText, nightTerrorsStatus, activityLimitsStatus, coughingStatus, zoneStatus;
        ProgressBar childActivityLimitsBar, parentActivityLimitsBar, childCoughingBar, parentCoughingBar;
        ChipGroup triggersContainer;
        View colourBox;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            dateText = itemView.findViewById(R.id.dateText);
            childText = itemView.findViewById(R.id.childText);
            parentText = itemView.findViewById(R.id.parentText);
            nightTerrorsStatus = itemView.findViewById(R.id.nightTerrorsStatus);
            activityLimitsStatus = itemView.findViewById(R.id.activityLimitsStatus);
            coughingStatus = itemView.findViewById(R.id.coughingStatus);
            colourBox = itemView.findViewById(R.id.colourBox);
            zoneStatus = itemView.findViewById(R.id.zoneStatus);

            childActivityLimitsBar = itemView.findViewById(R.id.childActivityLimitsBar);
            parentActivityLimitsBar = itemView.findViewById(R.id.parentActivityLimitsBar);
            childCoughingBar = itemView.findViewById(R.id.childCoughingBar);
            parentCoughingBar = itemView.findViewById(R.id.parentCoughingBar);

            triggersContainer = itemView.findViewById(R.id.triggersContainer);
        }

        public void setChips(List<String> triggers) {
            triggersContainer.removeAllViews();
            for (String trigger : triggers) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(itemView.getContext());
                chip.setText(trigger);
                triggersContainer.addView(chip);
            }
        }
    }
}
