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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.R;
import com.example.smart_air.Repository.CheckInRepository;
import com.example.smart_air.Repository.HistoryRepository;
import com.example.smart_air.adapter.HistoryAdapter;
import com.example.smart_air.modelClasses.HistoryItem;
import com.google.android.material.button.MaterialButton;
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
    public String [] filters = {"ALL","ALL","ALL","ALL","ALL","ALL"};
    String childUid;

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
        GridLayout filterContainerInitial = view.findViewById(R.id.filterGrid);
        filterContainerInitial.setVisibility(View.GONE);

        MaterialButton filter = view.findViewById(R.id.buttonFilters);
        filter.setOnClickListener(v -> {
            GridLayout filterContainer = view.findViewById(R.id.filterGrid);
            if(filterContainer.getVisibility() == View.GONE){
                filterContainer.setVisibility(View.VISIBLE);
                filter.setText("FILTERS ▲");
            }
            else{
                filterContainer.setVisibility(View.GONE);
                filter.setText("FILTERS ▼");
            }
        });

        setUpFilterUI();
        repo.getChildUid(this);

        // night filter
        AutoCompleteTextView nightDropdown = view.findViewById(R.id.selectNightWaking);
        nightDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            if(selected.equals("YES")){
                filters[0] = "true";
            }
            else if(selected.equals("NO")){
                filters[0] = "false";
            }
            else{
                filters[0] = selected;
            }
            repo.getDailyCheckIns(childUid,this);
        });

        // activity filter
        AutoCompleteTextView activityDropdown = view.findViewById(R.id.selectActivityLimits);
        activityDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            filters[1] = selected;
            repo.getDailyCheckIns(childUid,this);
        });

        // coughing filter
        AutoCompleteTextView coughingDropdown = view.findViewById(R.id.selectCoughingLevel);
        coughingDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            filters[2] = selected;
            repo.getDailyCheckIns(childUid,this);
        });

        // triggers filter
        AutoCompleteTextView triggerDropdown = view.findViewById(R.id.selectTriggers);
        triggerDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            filters[3] = selected;
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
        String [] dateOptions = {"ALL", "Past 3 months", "Past month", "Past 2 weeks"};
        setUpOneFilterUI(R.id.selectNightWaking,nightWakingOptions);
        setUpOneFilterUI(R.id.selectActivityLimits,activityLimitsOptions);
        setUpOneFilterUI(R.id.selectCoughingLevel,coughingLevelOptions);
        setUpOneFilterUI(R.id.selectTriggers,triggersOptions);
        setUpOneFilterUI(R.id.selectTriage,triageOptions);
        setUpOneFilterUI(R.id.selectDate,dateOptions);
    }

    public void createRecycleView(List<HistoryItem> results) {
        RecyclerView recyclerView = view.findViewById(R.id.historyRecyclerView);
        HistoryAdapter adapter = new HistoryAdapter(results); // your list of HistoryItem
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
}
