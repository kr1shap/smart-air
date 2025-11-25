package com.example.smart_air;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.modelClasses.Child;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditChildDialogFragment extends DialogFragment {

    private static final String TAG = "EditChildDialog";
    private static final String ARG_CHILD = "child";
    private Child child;
    private ChildRepository childRepo;
    private EditText etChildName, etChildDob, etChildNotes, etPersonalBest, etTresholdTechnique, etTresholdRescue;
    private SwitchCompat switchRescue, switchController, switchSymptoms,
            switchTriggers, switchPef, switchTriage, switchCharts;
    private Date selectedDate;
    private OnChildUpdatedListener listener;

    //Toggle buttons
    private Chip chipMon, chipTues, chipWed, chipThurs, chipFri, chipSat, chipSun;

    public interface OnChildUpdatedListener {
        void onChildUpdated();
    }

    public static EditChildDialogFragment newInstance(Child child) {
        EditChildDialogFragment fragment = new EditChildDialogFragment();
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
            Log.d(TAG, "Child loaded: " + child.getName());
            if (child.getSharing() != null) {
                Log.d(TAG, "Sharing settings: " + child.getSharing().toString());
            }
        }
        childRepo = new ChildRepository();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_child, null);

        view.setMinimumHeight(600);

        etChildName = view.findViewById(R.id.et_child_name);
        etChildDob = view.findViewById(R.id.et_child_dob);
        etChildNotes = view.findViewById(R.id.et_child_notes);
        etPersonalBest = view.findViewById(R.id.et_personal_best);

        etTresholdTechnique = view.findViewById(R.id.et_badge_threshold_tech);
        etTresholdRescue = view.findViewById(R.id.et_badge_threshold_rescue);

        //toggle buttons
        chipMon = view.findViewById(R.id.chipMon);
        chipTues = view.findViewById(R.id.chipTues);
        chipWed = view.findViewById(R.id.chipWed);
        chipThurs = view.findViewById(R.id.chipThurs);
        chipFri = view.findViewById(R.id.chipFri);
        chipSat = view.findViewById(R.id.chipSat);
        chipSun = view.findViewById(R.id.chipSun);


        switchRescue = view.findViewById(R.id.switch_rescue);
        switchController = view.findViewById(R.id.switch_controller);
        switchSymptoms = view.findViewById(R.id.switch_symptoms);
        switchTriggers = view.findViewById(R.id.switch_triggers);
        switchPef = view.findViewById(R.id.switch_pef);
        switchTriage = view.findViewById(R.id.switch_triage);
        switchCharts = view.findViewById(R.id.switch_charts);

        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        // Populate fields with existing data
        if (child != null) {
            if (child.getName() != null && !child.getName().isEmpty()) {
                etChildName.setText(child.getName());
            }

            if (child.getDob() != null) {
                selectedDate = child.getDob();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                etChildDob.setText(sdf.format(child.getDob()));
            }

            if (child.getExtraNotes() != null) {
                etChildNotes.setText(child.getExtraNotes());
            }

            if (child.getPersonalBest() > 0) {
                etPersonalBest.setText(String.valueOf(child.getPersonalBest()));
            }

            if(child.getThresholds() != null) {
                if(child.getThresholds().containsKey("quality_thresh")) {
                    etTresholdTechnique.setText(String.valueOf(child.getThresholds().get("quality_thresh")));
                }
                if(child.getThresholds().containsKey("rescue_thresh")) {
                    etTresholdRescue.setText(String.valueOf(child.getThresholds().get("rescue_thresh")));
                }
            }

            //Load the badge thresholds
            Map<String, Boolean> weeklySchedule = child.getWeeklySchedule();        // weekly schedule
            // Load sharing toggles - FIXED
            if (weeklySchedule != null) {
                chipMon.setChecked(weeklySchedule.getOrDefault("Monday", false));
                chipTues.setChecked(weeklySchedule.getOrDefault("Tuesday", false));
                chipWed.setChecked(weeklySchedule.getOrDefault("Wednesday", false));
                chipThurs.setChecked(weeklySchedule.getOrDefault("Thursday", false));
                chipFri.setChecked(weeklySchedule.getOrDefault("Friday", false));
                chipSat.setChecked(weeklySchedule.getOrDefault("Saturday", false));
                chipSun.setChecked(weeklySchedule.getOrDefault("Sunday", false));

            } else {
                Log.w(TAG, "Weekly map is null, defaulting to false");
            }

            // Load sharing toggles - FIXED
            Map<String, Boolean> sharing = child.getSharing();
            if (sharing != null) {
                Log.d(TAG, "Loading toggles from sharing map:");

                Boolean rescue = sharing.get("rescue");
                Boolean controller = sharing.get("controller");
                Boolean symptoms = sharing.get("symptoms");
                Boolean triggers = sharing.get("triggers");
                Boolean pef = sharing.get("pef");
                Boolean triage = sharing.get("triage");
                Boolean charts = sharing.get("charts");

                switchRescue.setChecked(rescue != null && rescue);
                switchController.setChecked(controller != null && controller);
                switchSymptoms.setChecked(symptoms != null && symptoms);
                switchTriggers.setChecked(triggers != null && triggers);
                switchPef.setChecked(pef != null && pef);
                switchTriage.setChecked(triage != null && triage);
                switchCharts.setChecked(charts != null && charts);

                Log.d(TAG, "Rescue: " + rescue + " -> " + switchRescue.isChecked());
                Log.d(TAG, "Controller: " + controller + " -> " + switchController.isChecked());
                Log.d(TAG, "Symptoms: " + symptoms + " -> " + switchSymptoms.isChecked());
            } else {
                Log.w(TAG, "Sharing map is null, defaulting to false");
            }
        }

        etChildDob.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveChild());
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    selectedDate = selected.getTime();

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    etChildDob.setText(sdf.format(selectedDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveChild() {
        if (child == null) return;

        String name = etChildName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a child name", Toast.LENGTH_SHORT).show();
            return;
        }
        child.setName(name);

        if (selectedDate != null) {
            int age = calculateAge(selectedDate);
            if (age < 6 || age > 16) {
                Toast.makeText(getContext(), "Child must be between 6 and 16 years old", Toast.LENGTH_SHORT).show();
                return;
            }
            child.setDob(selectedDate);
        }

        String notes = etChildNotes.getText().toString().trim();
        child.setExtraNotes(notes.isEmpty() ? null : notes);

        String personalBestStr = etPersonalBest.getText().toString().trim();
        if (!personalBestStr.isEmpty()) {
            try {
                int personalBest = Integer.parseInt(personalBestStr);
                if (personalBest < 0) {
                    Toast.makeText(getContext(), "Personal best must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (personalBest > 1000) {
                    Toast.makeText(getContext(), "Personal best seems too high", Toast.LENGTH_SHORT).show();
                    return;
                }
                child.setPersonalBest(personalBest);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            child.setPersonalBest(0);
        }

        //UPDATE THRESHOLDS
        String tresholdTechStr = etTresholdTechnique.getText().toString().trim();
        String rescueStr = etTresholdRescue.getText().toString().trim();
        Map<String, Integer> thresholds = new HashMap<>();
        //for technique thresh
        if (!tresholdTechStr.isEmpty()) {
            try {
                int number = Integer.parseInt(tresholdTechStr);
                if (number < 0) {
                    Toast.makeText(getContext(), "Threshold for technique must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }
                thresholds.put("quality_thresh", number);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            thresholds.put("quality_thresh", 10); //default 10
        }
        //for rescue thresh
        if (!rescueStr.isEmpty()) {
            try {
                int number = Integer.parseInt(rescueStr);
                if (number < 0) {
                    Toast.makeText(getContext(), "Threshold for rescue must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }
                thresholds.put("rescue_thresh", number);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            thresholds.put("rescue_thresh", 4); //default 4
        }
        child.setThresholds(thresholds);

        // Update weekly toggles
        Map<String, Boolean> weeklySchedule = new HashMap<>();
        weeklySchedule.put("Monday", chipMon.isChecked());
        weeklySchedule.put("Tuesday", chipTues.isChecked());
        weeklySchedule.put("Wednesday", chipWed.isChecked());
        weeklySchedule.put("Thursday", chipThurs.isChecked());
        weeklySchedule.put("Friday", chipFri.isChecked());
        weeklySchedule.put("Saturday", chipSat.isChecked());
        weeklySchedule.put("Sunday", chipSun.isChecked());
        child.setWeeklySchedule(weeklySchedule);

        // Update sharing toggles
        Map<String, Boolean> sharing = new HashMap<>();
        sharing.put("rescue", switchRescue.isChecked());
        sharing.put("controller", switchController.isChecked());
        sharing.put("symptoms", switchSymptoms.isChecked());
        sharing.put("triggers", switchTriggers.isChecked());
        sharing.put("pef", switchPef.isChecked());
        sharing.put("triage", switchTriage.isChecked());
        sharing.put("charts", switchCharts.isChecked());
        child.setSharing(sharing);

        Log.d(TAG, "Saving child with sharing: " + sharing.toString());

        childRepo.updateChild(child,
                aVoid -> {
                    Log.d(TAG, "Child updated successfully");
                    Toast.makeText(getContext(), "Child updated", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onChildUpdated();
                    }
                    dismiss();
                },
                e -> {
                    Log.e(TAG, "Error updating child", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void setOnChildUpdatedListener(OnChildUpdatedListener listener) {
        this.listener = listener;
    }

    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
}