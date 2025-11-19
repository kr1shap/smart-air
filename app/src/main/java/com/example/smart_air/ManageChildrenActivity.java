package com.example.smart_air;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.adapter.ChildrenAdapter;
import com.example.smart_air.modelClasses.Child;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ManageChildrenActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChildren;
    private FloatingActionButton fabAddChild;
    private ChildrenAdapter childrenAdapter;
    private ChildRepository childRepository;
    private String parentUid;
    private List<Child> childrenList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        childRepository = new ChildRepository();
        childrenList = new ArrayList<>();

        initViews();
        setupRecyclerView();
        loadChildren();

        fabAddChild.setOnClickListener(v -> showAddChildDialog());
    }

    private void initViews() {
        recyclerViewChildren = findViewById(R.id.recyclerViewChildren);
        fabAddChild = findViewById(R.id.fabAddChild);
    }

    private void setupRecyclerView() {
        childrenAdapter = new ChildrenAdapter(childrenList, this::onChildClick);
        recyclerViewChildren.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChildren.setAdapter(childrenAdapter);
    }

    private void loadChildren() {
        childRepository.getChildrenForParent(parentUid).observe(this, children -> {
            if (children != null) {
                childrenList.clear();
                childrenList.addAll(children);
                childrenAdapter.notifyDataSetChanged();
            }
        });
    }

    private void onChildClick(Child child) {
        // Open sharing settings for this child
        showSharingSettingsDialog(child);
    }

    private void showAddChildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_child, null);
        builder.setView(dialogView);

        EditText etChildName = dialogView.findViewById(R.id.etChildName);
        Button btnSelectDob = dialogView.findViewById(R.id.btnSelectDob);
        EditText etNotes = dialogView.findViewById(R.id.etNotes);

        final Date[] selectedDob = {null};

        btnSelectDob.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDob[0] = calendar.getTime();
                        btnSelectDob.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setTitle("Add Child")
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etChildName.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter child's name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (selectedDob[0] == null) {
                        Toast.makeText(this, "Please select date of birth", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Child newChild = new Child(
                            null, // Will be generated
                            parentUid,
                            name,
                            selectedDob[0],
                            notes,
                            0, // Default personal best
                            Child.getDefaultSharingSettings()
                    );

                    childRepository.addChild(newChild, parentUid, new ChildRepository.OnChildOperationListener() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(ManageChildrenActivity.this, message, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(ManageChildrenActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void showSharingSettingsDialog(Child child) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sharing_settings, null);
        builder.setView(dialogView);

        // Initialize all toggle views
        com.google.android.material.switchmaterial.SwitchMaterial switchRescue = dialogView.findViewById(R.id.switchRescueLogs);
        com.google.android.material.switchmaterial.SwitchMaterial switchController = dialogView.findViewById(R.id.switchControllerAdherence);
        com.google.android.material.switchmaterial.SwitchMaterial switchSymptoms = dialogView.findViewById(R.id.switchSymptoms);
        com.google.android.material.switchmaterial.SwitchMaterial switchTriggers = dialogView.findViewById(R.id.switchTriggers);
        com.google.android.material.switchmaterial.SwitchMaterial switchPeakFlow = dialogView.findViewById(R.id.switchPeakFlow);
        com.google.android.material.switchmaterial.SwitchMaterial switchTriage = dialogView.findViewById(R.id.switchTriageIncidents);
        com.google.android.material.switchmaterial.SwitchMaterial switchCharts = dialogView.findViewById(R.id.switchSummaryCharts);

        // Set current values
        switchRescue.setChecked(child.getSharing().getOrDefault(Child.SHARE_RESCUE_LOGS, false));
        switchController.setChecked(child.getSharing().getOrDefault(Child.SHARE_CONTROLLER_ADHERENCE, false));
        switchSymptoms.setChecked(child.getSharing().getOrDefault(Child.SHARE_SYMPTOMS, false));
        switchTriggers.setChecked(child.getSharing().getOrDefault(Child.SHARE_TRIGGERS, false));
        switchPeakFlow.setChecked(child.getSharing().getOrDefault(Child.SHARE_PEAK_FLOW, false));
        switchTriage.setChecked(child.getSharing().getOrDefault(Child.SHARE_TRIAGE_INCIDENTS, false));
        switchCharts.setChecked(child.getSharing().getOrDefault(Child.SHARE_SUMMARY_CHARTS, false));

        // Set listeners for real-time updates
        switchRescue.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateSharing(child.getChildUid(), Child.SHARE_RESCUE_LOGS, isChecked));
        switchController.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateSharing(child.getChildUid(), Child.SHARE_CONTROLLER_ADHERENCE, isChecked));
        switchSymptoms.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateSharing(child.getChildUid(), Child.SHARE_SYMPTOMS, isChecked));
        switchTriggers.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateSharing(child.getChildUid(), Child.SHARE_TRIGGERS, isChecked));
        switchPeakFlow.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateSharing(child.getChildUid(), Child.SHARE_PEAK_FLOW, isChecked));
        switchTriage.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateSharing(child.getChildUid(), Child.SHARE_TRIAGE_INCIDENTS, isChecked));
        switchCharts.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateSharing(child.getChildUid(), Child.SHARE_SUMMARY_CHARTS, isChecked));

        builder.setTitle("Share with Provider - " + child.getName())
                .setPositiveButton("Done", null)
                .create()
                .show();
    }

    private void updateSharing(String childUid, String sharingKey, boolean enabled) {
        childRepository.updateSingleSharingToggle(childUid, sharingKey, enabled,
                new ChildRepository.OnChildOperationListener() {
                    @Override
                    public void onSuccess(String message) {
                        // Silently update - no toast needed for each toggle
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(ManageChildrenActivity.this,
                                "Failed to update: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}