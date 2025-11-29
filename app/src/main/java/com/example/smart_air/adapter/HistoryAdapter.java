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

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_TRIAGE = 1;
    private static final int VIEW_TYPE_DAILY = 2;
    private List<HistoryItem> historyItems; // list with cards

    // set array for HistoryAdapter
    public HistoryAdapter(List<HistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    // update array for HistoryAdapter
    public void updateList(List<HistoryItem> newList) {
        this.historyItems.clear();
        this.historyItems.addAll(newList);
        notifyDataSetChanged();
    }

    // get array for HistoryAdapter
    public List<HistoryItem> getCurrentList() {
        return new ArrayList<>(historyItems);
    }

    // get type of card for HistoryItem
    @Override
    public int getItemViewType(int position) {
        HistoryItem item = historyItems.get(position);
        if (item.cardType == HistoryItem.typeOfCard.triage) {
            return VIEW_TYPE_TRIAGE;
        } else {
            return VIEW_TYPE_DAILY;
        }
    }

    // creating the view holder for each card
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

    // call specifci function based on type of viewHolder
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

    public static class DailyViewHolder extends RecyclerView.ViewHolder {
        // setting up ui components
        TextView dateText, childText, parentText, nightTerrorsStatus, activityLimitsStatus, coughingStatus, zoneStatus, nightTerror, activityLimit, coughingWheezing, trigger;
        ProgressBar childActivityLimitsBar, parentActivityLimitsBar, childCoughingBar, parentCoughingBar;
        ChipGroup triggersContainer;
        View colourBox;

        public DailyViewHolder(@NonNull View itemView) {
            super(itemView);

            // declaring ui components
            dateText = itemView.findViewById(R.id.dateText);
            childText = itemView.findViewById(R.id.childText);
            parentText = itemView.findViewById(R.id.parentText);
            nightTerrorsStatus = itemView.findViewById(R.id.nightTerrorsStatus);
            activityLimitsStatus = itemView.findViewById(R.id.activityLimitsStatus);
            coughingStatus = itemView.findViewById(R.id.coughingStatus);
            colourBox = itemView.findViewById(R.id.colourBox);
            zoneStatus = itemView.findViewById(R.id.zoneStatus);
            nightTerror = itemView.findViewById(R.id.nightTerror);
            activityLimit = itemView.findViewById(R.id.activityLimit);
            coughingWheezing = itemView.findViewById(R.id.coughingWheezing);
            trigger = itemView.findViewById(R.id.trigger);

            childActivityLimitsBar = itemView.findViewById(R.id.childActivityLimitsBar);
            parentActivityLimitsBar = itemView.findViewById(R.id.parentActivityLimitsBar);
            childCoughingBar = itemView.findViewById(R.id.childCoughingBar);
            parentCoughingBar = itemView.findViewById(R.id.parentCoughingBar);

            triggersContainer = itemView.findViewById(R.id.triggersContainer);
        }

        public void bind(HistoryItem card) {
            // date
            dateText.setText(card.date);

            // child/parent logic
            switch (card.cardType) {
                case childOnly:
                    childText.setVisibility(View.VISIBLE);
                    parentText.setVisibility(View.INVISIBLE);

                    childActivityLimitsBar.setProgress(card.activityChild);
                    parentActivityLimitsBar.setVisibility(View.INVISIBLE);
                    childActivityLimitsBar.setVisibility(View.VISIBLE);
                    childActivityLimitsBar.setProgressTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.role_default_bg))
                    );

                    childCoughingBar.setProgress(card.coughingChild);
                    parentCoughingBar.setVisibility(View.INVISIBLE);
                    childCoughingBar.setVisibility(View.VISIBLE);
                    childCoughingBar.setProgressTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.role_default_bg))
                    );
                    break;

                case parentOnly:
                    childText.setVisibility(View.INVISIBLE);
                    parentText.setVisibility(View.VISIBLE);

                    childActivityLimitsBar.setProgress(card.activityParent);
                    childActivityLimitsBar.setProgressTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.role_selected_bg))
                    );
                    parentActivityLimitsBar.setVisibility(View.INVISIBLE);
                    childActivityLimitsBar.setVisibility(View.VISIBLE);

                    childCoughingBar.setProgress(card.coughingParent);
                    parentCoughingBar.setVisibility(View.INVISIBLE);
                    childCoughingBar.setVisibility(View.VISIBLE);
                    childCoughingBar.setProgressTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.role_selected_bg))
                    );
                    break;

                case both:
                    childText.setVisibility(View.VISIBLE);
                    parentText.setVisibility(View.VISIBLE);

                    childActivityLimitsBar.setProgress(card.activityChild);
                    parentActivityLimitsBar.setProgress(card.activityParent);
                    childActivityLimitsBar.setProgressTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.role_default_bg))
                    );

                    childActivityLimitsBar.setVisibility(View.VISIBLE);
                    parentActivityLimitsBar.setVisibility(View.VISIBLE);

                    childCoughingBar.setProgress(card.coughingChild);
                    parentCoughingBar.setProgress(card.coughingParent);
                    childCoughingBar.setProgressTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.role_default_bg))
                    );

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

            if(card.removeSymptoms){
                childText.setVisibility(View.GONE);
                parentText.setVisibility(View.GONE);
                childActivityLimitsBar.setVisibility(View.GONE);
                parentActivityLimitsBar.setVisibility(View.GONE);
                childCoughingBar.setVisibility(View.GONE);
                parentCoughingBar.setVisibility(View.GONE);
                nightTerror.setVisibility(View.GONE);
                activityLimit.setVisibility(View.GONE);
                coughingWheezing.setVisibility(View.GONE);
                nightTerrorsStatus.setVisibility(View.GONE);
                activityLimitsStatus.setVisibility(View.GONE);
                coughingStatus.setVisibility(View.GONE);
            }
            else{
                childText.setVisibility(View.VISIBLE);
                parentText.setVisibility(View.VISIBLE);
                childActivityLimitsBar.setVisibility(View.VISIBLE);
                parentActivityLimitsBar.setVisibility(View.VISIBLE);
                childCoughingBar.setVisibility(View.VISIBLE);
                parentCoughingBar.setVisibility(View.VISIBLE);
                nightTerror.setVisibility(View.VISIBLE);
                activityLimit.setVisibility(View.VISIBLE);
                coughingWheezing.setVisibility(View.VISIBLE);
                nightTerrorsStatus.setVisibility(View.VISIBLE);
                activityLimitsStatus.setVisibility(View.VISIBLE);
                coughingStatus.setVisibility(View.VISIBLE);
            }
            if(card.removeTrigger){
                trigger.setVisibility(View.GONE);
                triggersContainer.setVisibility(View.GONE);
            }
            else{
                trigger.setVisibility(View.VISIBLE);
                triggersContainer.setVisibility(View.VISIBLE);
            }
        }

        // return hexcode for zone colour
        private int getColour(String zone) {
            switch (zone) {
                case "green": return Color.parseColor("#4CAF50");
                case "yellow": return Color.parseColor("#FFEB3B");
                case "red": return Color.parseColor("#F44336");
                default: return Color.GRAY;
            }
        }

        // add chips to trigger group
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
        // setting up ui components
        ChipGroup flagsChipGroup;
        TextView pefValue, rescueAttemptsValue, emergencyButtonText, userResponseList, dateText, title;

        TriageViewHolder(@NonNull View itemView) {
            super(itemView);
            // declaring ui components
            flagsChipGroup = itemView.findViewById(R.id.flagsChipGroup);
            pefValue = itemView.findViewById(R.id.pefValue);
            rescueAttemptsValue = itemView.findViewById(R.id.rescueAttemptsValue);
            emergencyButtonText = itemView.findViewById(R.id.emergencyButtonText);
            userResponseList = itemView.findViewById(R.id.userResponseList);
            dateText = itemView.findViewById(R.id.dateText);
            title = itemView.findViewById(R.id.title);
        }

        void bind(HistoryItem item) {
            // setting values based on card
            dateText.setText(item.time);
            title.setText(item.date + " INCIDENT LOG");
            userResponseList.setText(item.userBullets);
            if(item.pef == -5){
                pefValue.setText("not entered");
            }
            else if(item.pef == -10){
                pefValue.setText("not available");
            }
            else{
                pefValue.setText(String.valueOf(item.pef));
            }
            if(item.rescueAttempts == -5){
                rescueAttemptsValue.setText("0");
            }
            else if(item.rescueAttempts == -10){
                rescueAttemptsValue.setText("n/a");
            }
            else{
                rescueAttemptsValue.setText(String.valueOf(item.rescueAttempts));
            }
            emergencyButtonText.setText(item.emergencyCall);
            setChips(item.flaglist);
        }

        // add chips to flag group
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
