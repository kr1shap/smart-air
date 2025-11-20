package com.example.smart_air;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.modelClasses.Child;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChildDetailFragment extends Fragment {

    private static final String ARG_CHILD = "child";
    private Child child;
    private ChildRepository childRepo;

    private TextView tvChildName, tvChildDob, tvChildNotes;

    private SwitchCompat switchRescue, switchController, switchSymptoms, switchTriggers, switchPef,
            switchTriage, switchCharts;


    /**
     * Factory method for creating an instance of this fragment.
     * serializes the Child object into JSON & places it into fragments arguments.
     */
    public static ChildDetailFragment newInstance(Child child){
        ChildDetailFragment fragment = new ChildDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD, new Gson().toJson(child));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            String childJson = getArguments().getString(ARG_CHILD);
            child = new Gson().fromJson(childJson, Child.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_child_detail, container, false);

        childRepo = new ChildRepository();

        initViews(view);
        populateChildInfo();
        setupSharingToggles();

        return view;
    }

    // finds and assigns all views from the layout
    private void initViews(View view) {
        tvChildName   = view.findViewById(R.id.tv_child_name);
        tvChildDob    = view.findViewById(R.id.tv_child_dob);
        tvChildNotes  = view.findViewById(R.id.tv_child_notes);

        // toggles
        switchRescue    = view.findViewById(R.id.switch_rescue);
        switchController = view.findViewById(R.id.switch_controller);
        switchSymptoms   = view.findViewById(R.id.switch_symptoms);
        switchTriggers   = view.findViewById(R.id.switch_triggers);
        switchPef        = view.findViewById(R.id.switch_pef);
        switchTriage     = view.findViewById(R.id.switch_triage);
        switchCharts     = view.findViewById(R.id.switch_charts);
    }

    /**
     * Populates UI fields with the Child's information
     * & loads toggle states from Firestore (or defaults if missing).
     */
    private void populateChildInfo() {
        if (child == null) return;

        // child label
        tvChildName.setText(
                getString(R.string.child_label, child.getChildUid())
        );

        // dob
        if (child.getDob() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvChildDob.setText(
                    getString(R.string.child_dob_label, sdf.format(child.getDob()))
            );
        }

        // notes
        if (child.getExtraNotes() != null && !child.getExtraNotes().isEmpty()) {
            tvChildNotes.setText(
                    getString(R.string.child_notes_label, child.getExtraNotes())
            );
            tvChildNotes.setVisibility(View.VISIBLE);
        } else {
            tvChildNotes.setVisibility(View.GONE);
        }

        // sharing toggles (load defaults if missing)
        Map<String, Boolean> sharing = child.getSharing();
        if (sharing == null) {
            sharing = getDefaultSharing();
            child.setSharing(sharing);
        }

        switchRescue.setChecked(Boolean.TRUE.equals(sharing.get("rescue")));
        switchController.setChecked(Boolean.TRUE.equals(sharing.get("controller")));
        switchSymptoms.setChecked(Boolean.TRUE.equals(sharing.get("symptoms")));
        switchTriggers.setChecked(Boolean.TRUE.equals(sharing.get("triggers")));
        switchPef.setChecked(Boolean.TRUE.equals(sharing.get("pef")));
        switchTriage.setChecked(Boolean.TRUE.equals(sharing.get("triage")));
        switchCharts.setChecked(Boolean.TRUE.equals(sharing.get("charts")));
    }

    /**
     * Attaches a single listener reused by all switches.
     * Whenever ANY switch is toggled, the entire sharing map is updated in Firebase.
     */
    private void setupSharingToggles(){
        View.OnClickListener toggleListener = v -> {

            // build updated permissions map
            Map<String, Boolean> sharing = new HashMap<>();

            sharing.put("rescue", switchRescue.isChecked());
            sharing.put("controller", switchController.isChecked());
            sharing.put("symptoms", switchSymptoms.isChecked());
            sharing.put("triggers", switchTriggers.isChecked());
            sharing.put("pef", switchPef.isChecked());
            sharing.put("triage", switchTriage.isChecked());
            sharing.put("charts", switchCharts.isChecked());

            updateSharingSettings(sharing);
        };

        // apply same listener to all switches
        switchRescue.setOnClickListener(toggleListener);
        switchController.setOnClickListener(toggleListener);
        switchSymptoms.setOnClickListener(toggleListener);
        switchTriggers.setOnClickListener(toggleListener);
        switchPef.setOnClickListener(toggleListener);
        switchTriage.setOnClickListener(toggleListener);
        switchCharts.setOnClickListener(toggleListener);
    }

    /**
     * Saves updated sharing preferences to Firebase.
     *
     * @param sharing updated map of sharing toggles.
     */
    private void updateSharingSettings(Map<String, Boolean> sharing){
        if (child == null) return;

        childRepo.updateSharingToggles(
                child.getChildUid(),
                sharing,
                aVoid -> {
                    child.setSharing(sharing);
                    Toast.makeText(getContext(),
                            R.string.sharing_updated,
                            Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Toast.makeText(getContext(),
                            getString(R.string.error_updating_sharing, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                    populateChildInfo(); // revert switches
                }
        );
    }

    private Map<String, Boolean> getDefaultSharing(){

        Map<String, Boolean> sharing = new HashMap<>();
        sharing.put("rescue", true);
        sharing.put("controller", true);
        sharing.put("symptoms", true);
        sharing.put("triggers", true);
        sharing.put("pef", true);
        sharing.put("triage", true);
        sharing.put("charts", true);
        return sharing;

    }
}
