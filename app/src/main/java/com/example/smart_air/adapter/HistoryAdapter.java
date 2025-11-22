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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_TRIAGE = 1;
    private static final int VIEW_TYPE_DAILY = 2;
    private List<HistoryItem> historyItems;

    public HistoryAdapter(List<HistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    @Override
    public int getItemViewType(int position) {
        HistoryItem item = historyItems.get(position);
        if (item.cardType == HistoryItem.typeOfCard.triage) {
            return VIEW_TYPE_TRIAGE;
        } else {
            return VIEW_TYPE_DAILY;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_DAILY){
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_dailycheckin, parent, false);
            return new DailyViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_triage, parent, false);
            return new TriageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        HistoryItem item = historyItems.get(position);

        if (holder instanceof DailyViewHolder) {
            ((DailyViewHolder) holder).bind(item);
        } else if (holder instanceof TriageViewHolder) {
            ((TriageViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    static class DailyViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, childText, parentText, nightTerrorsStatus, activityLimitsStatus, coughingStatus, zoneStatus;
        ProgressBar childActivityLimitsBar, parentActivityLimitsBar, childCoughingBar, parentCoughingBar;
        ChipGroup triggersContainer;
        View colourBox;

        DailyViewHolder(@NonNull View itemView) {
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

        void bind(HistoryItem card) {
            // DATE
            dateText.setText(card.date);

            // CHILD / PARENT BAR LOGIC
            switch (card.cardType) {
                case childOnly:
                    childText.setVisibility(View.VISIBLE);
                    parentText.setVisibility(View.INVISIBLE);

                    childActivityLimitsBar.setProgress(card.activityChild);
                    parentActivityLimitsBar.setVisibility(View.INVISIBLE);
                    childActivityLimitsBar.setVisibility(View.VISIBLE);

                    childCoughingBar.setProgress(card.coughingChild);
                    parentCoughingBar.setVisibility(View.INVISIBLE);
                    childCoughingBar.setVisibility(View.VISIBLE);
                    break;

                case parentOnly:
                    childText.setVisibility(View.INVISIBLE);
                    parentText.setVisibility(View.VISIBLE);

                    childActivityLimitsBar.setProgress(card.activityParent);
                    parentActivityLimitsBar.setVisibility(View.INVISIBLE);
                    childActivityLimitsBar.setVisibility(View.VISIBLE);

                    childCoughingBar.setProgress(card.coughingParent);
                    parentCoughingBar.setVisibility(View.INVISIBLE);
                    childCoughingBar.setVisibility(View.VISIBLE);
                    break;

                case both:
                    childText.setVisibility(View.VISIBLE);
                    parentText.setVisibility(View.VISIBLE);

                    childActivityLimitsBar.setProgress(card.activityChild);
                    parentActivityLimitsBar.setProgress(card.activityParent);

                    childActivityLimitsBar.setVisibility(View.VISIBLE);
                    parentActivityLimitsBar.setVisibility(View.VISIBLE);

                    childCoughingBar.setProgress(card.coughingChild);
                    parentCoughingBar.setProgress(card.coughingParent);

                    childCoughingBar.setVisibility(View.VISIBLE);
                    parentCoughingBar.setVisibility(View.VISIBLE);
                    break;
            }

            // statuses
            nightTerrorsStatus.setText(card.nightStatus);
            activityLimitsStatus.setText(card.activityStatus);
            coughingStatus.setText(card.coughingStatus);

            // zone indicator
            if (card.zone.isEmpty()) {
                colourBox.setVisibility(View.GONE);
                zoneStatus.setVisibility(View.VISIBLE);
                zoneStatus.setText("NOT ENTERED");
            } else {
                zoneStatus.setVisibility(View.GONE);
                colourBox.setVisibility(View.VISIBLE);
                colourBox.setBackgroundColor(getColour(card.zone));
            }

            // triggers
            setChips(card.triggers);
        }

        private int getColour(String zone) {
            switch (zone) {
                case "green": return Color.parseColor("#4CAF50");
                case "yellow": return Color.parseColor("#FFEB3B");
                case "red": return Color.parseColor("#F44336");
                default: return Color.GRAY;
            }
        }

        private void setChips(List<String> triggers) {
            triggersContainer.removeAllViews();
            for (String trigger : triggers) {
                Chip chip = new Chip(itemView.getContext());
                chip.setText(trigger);
                triggersContainer.addView(chip);
            }
        }
    }

    static class TriageViewHolder extends RecyclerView.ViewHolder {
        ChipGroup flagsChipGroup;
        TextView pefValue, rescueAttemptsValue, emergencyButtonText, userResponseList, dateText, title;

        TriageViewHolder(@NonNull View itemView) {
            super(itemView);
            flagsChipGroup = itemView.findViewById(R.id.flagsChipGroup);
            pefValue = itemView.findViewById(R.id.pefValue);
            rescueAttemptsValue = itemView.findViewById(R.id.rescueAttemptsValue);
            emergencyButtonText = itemView.findViewById(R.id.emergencyButtonText);
            userResponseList = itemView.findViewById(R.id.userResponseList);
            dateText = itemView.findViewById(R.id.dateText);
            title = itemView.findViewById(R.id.title);
        }

        void bind(HistoryItem item) {
            dateText.setText(item.time);
            title.setText(item.date + " INCIDENT LOG");
            userResponseList.setText(item.userBullets);
            if(item.pef == -5){
                pefValue.setText("not entered");
            }
            else{
                pefValue.setText(String.valueOf(item.pef));
            }
            if(item.rescueAttempts == -5){
                rescueAttemptsValue.setText("0");
            }
            else{
                rescueAttemptsValue.setText(String.valueOf(item.rescueAttempts));
            }
            emergencyButtonText.setText(item.emergencyCall);
            setChips(item.flaglist);
        }

        private void setChips(List<String> flags) {
            flagsChipGroup.removeAllViews();
            for (String flag : flags) {
                Chip chip = new Chip(itemView.getContext());
                chip.setText(flag);

                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#0277BD")));
                chip.setTextColor(Color.WHITE);

                flagsChipGroup.addView(chip);
            }
        }
    }
}
