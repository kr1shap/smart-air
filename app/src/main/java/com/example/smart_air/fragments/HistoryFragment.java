package com.example.smart_air.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.smart_air.R;
import com.example.smart_air.Repository.CheckInRepository;
import com.example.smart_air.Repository.HistoryRepository;
import com.example.smart_air.modelClasses.HistoryItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class HistoryFragment extends Fragment {

    private View view;
    private HistoryRepository repo;
    public String [] filters = {"ALL","ALL","ALL","ALL","ALL"};
    String childUid;
    LinearLayout container;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.history_page, container, false);
    }

    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        repo = new HistoryRepository();
        container = view.findViewById(R.id.historyContainer);

        setUpFilterUI();
        repo.getChildUid(this);

        AutoCompleteTextView nightDropdown = view.findViewById(R.id.selectNightWaking);

        nightDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            if(selected.equals("YES")){
                filters[0] = "true";
            }
            else if(selected.equals("NO")){
                filters[0] = "False";
            }
            else{
                filters[0] = selected;
            }
            container.removeAllViews();
            repo.getDailyCheckIns(childUid,this);
        });


    }

    public void setChildUid(String childUid) {
        this.childUid = childUid;
        repo.getDailyCheckIns(childUid,this);
    }
    public void exitScreen(){
        //TODO: fix it
    }

    public void createDailyCard(HistoryItem card){
        LinearLayout container = view.findViewById(R.id.historyContainer);
        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_parent_child, container, false);

        container.addView(cardView);

        // date
        TextView dateText = cardView.findViewById(R.id.dateText);
        dateText.setText(card.date);

        TextView childText = cardView.findViewById(R.id.childText);
        TextView parentText = cardView.findViewById(R.id.parentText);
        ProgressBar childActivityLimitsBar = cardView.findViewById(R.id.childActivityLimitsBar);
        ProgressBar parentActivityLimitsBar = cardView.findViewById(R.id.parentActivityLimitsBar);
        ProgressBar childCoughingBar = cardView.findViewById(R.id.childCoughingBar);
        ProgressBar parentCoughingBar = cardView.findViewById(R.id.parentCoughingBar);

        if(card.cardType == HistoryItem.typeOfCard.childOnly){
            // setting child parent text
            childText.setVisibility(View.VISIBLE);
            parentText.setVisibility(View.INVISIBLE);

            // activity bar
            childActivityLimitsBar.setProgress(card.activityChild);
            childActivityLimitsBar.setVisibility(View.VISIBLE);
            parentActivityLimitsBar.setVisibility(View.INVISIBLE);

            // coughing bar
            childCoughingBar.setVisibility(View.VISIBLE);
            parentCoughingBar.setVisibility(View.INVISIBLE);
            childCoughingBar.setProgress(card.coughingChild);
            parentCoughingBar.setProgress(card.coughingParent);
        }
        else if(card.cardType == HistoryItem.typeOfCard.parentOnly){
            // setting child parent text
            childText.setVisibility(View.INVISIBLE);
            parentText.setVisibility(View.VISIBLE);

            // activity bar
            parentActivityLimitsBar.setProgress(card.activityParent);
            parentActivityLimitsBar.setVisibility(View.VISIBLE);
            childActivityLimitsBar.setVisibility(View.INVISIBLE);

            // coughing bar
            childCoughingBar.setVisibility(View.INVISIBLE);
            parentCoughingBar.setVisibility(View.VISIBLE);
            childCoughingBar.setProgress(card.coughingChild);
            parentCoughingBar.setProgress(card.coughingParent);
        }
        else{
            // setting child parent text
            childText.setVisibility(View.VISIBLE);
            parentText.setVisibility(View.VISIBLE);

            // activity bar
            parentActivityLimitsBar.setProgress(card.activityParent);
            parentActivityLimitsBar.setVisibility(View.VISIBLE);
            childActivityLimitsBar.setVisibility(View.VISIBLE);
            childActivityLimitsBar.setProgress(card.activityChild);

            // coughing bar
            childCoughingBar.setVisibility(View.VISIBLE);
            childCoughingBar.setVisibility(View.VISIBLE);
            childCoughingBar.setProgress(card.coughingChild);
            parentCoughingBar.setProgress(card.coughingParent);
        }

        // set night trigger text
        TextView nightTerrorsStatus = cardView.findViewById(R.id.nightTerrorsStatus);
        nightTerrorsStatus.setText(card.nightStatus);
        
        // activity limit
        TextView activityLimitsStatus = cardView.findViewById(R.id.activityLimitsStatus);
        activityLimitsStatus.setText(card.activityStatus);

        // coughing
        TextView coughingStatus = cardView.findViewById(R.id.coughingStatus);
        coughingStatus.setText(card.coughingStatus);

        // triggers
        ChipGroup chipGroup = cardView.findViewById(R.id.triggersContainer);
        setChips(chipGroup,card.triggers);

        // adding pef
        TextView pef = cardView.findViewById(R.id.pefText);
        pef.setText(card.pefText);
        
    }

    private void setUpOneFilterUI(int type, String [] items){
        AutoCompleteTextView dropdown = view.findViewById(type);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
                items
        );

        dropdown.clearFocus();
        dropdown.setAdapter(adapter);
    }

    private void setUpFilterUI(){
        String [] nightWakingOptions = {"ALL","YES","NO"};
        String [] activityLimitsOptions = {"ALL","0-1","2-3","4-5","6-7","8-9","10"};
        String [] coughingLevelOptions = {"ALL","No Coughing", "Wheezing", "Coughing", "Extreme Coughing"};
        String [] triggersOptions = {"ALL","Allergies", "Smoke","Flu","Strong smells", "Running", "Exercise", "Cold Air", "Dust/Pets", "Illness"};
        String [] triageOptions = {"ALL","Days with Triage","Days without Triage"};
        setUpOneFilterUI(R.id.selectNightWaking,nightWakingOptions);
        setUpOneFilterUI(R.id.selectActivityLimits,activityLimitsOptions);
        setUpOneFilterUI(R.id.selectCoughingLevel,coughingLevelOptions);
        setUpOneFilterUI(R.id.selectTriggers,triggersOptions);
        setUpOneFilterUI(R.id.selectTriage,triageOptions);
    }

    private void setChips(ChipGroup chipGroup, List<String> chipTexts) {
        // remove all existing chips
        chipGroup.removeAllViews();

        for (String text : chipTexts) {
            Chip chip = new Chip(requireContext());
            chip.setText(text);
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(12f);
            chip.setChipBackgroundColorResource(R.color.colour_blue);
            float radius = 12 * getResources().getDisplayMetrics().density; // 8dp
            chip.setShapeAppearanceModel(
                    chip.getShapeAppearanceModel()
                            .toBuilder()
                            .setAllCornerSizes(radius)
                            .build()
            );
            chip.setChipMinHeight(0);
            chip.setChipStartPadding(6f);
            chip.setChipEndPadding(6f);
            chip.setCloseIconEnabled(false);

            int verticalPaddingPx = (int) (4 * getResources().getDisplayMetrics().density); // 2dp top & bottom
            int horizontalPaddingPx = chip.getPaddingLeft(); // keep existing left/right padding
            chip.setPadding(horizontalPaddingPx, verticalPaddingPx, horizontalPaddingPx, verticalPaddingPx);

            chip.setEnsureMinTouchTargetSize(false);
            chipGroup.addView(chip);
        }
    }

}
