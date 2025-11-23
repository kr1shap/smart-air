package com.example.smart_air;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.modelClasses.Child;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditChildDialogFragment extends DialogFragment {

    private static final String TAG = "EditChildDialog";
    private static final String ARG_CHILD = "child";
    private Child child;
    private ChildRepository childRepo; // ✅ FIXED: Changed from childRepository to childRepo
    private EditText etChildName, etChildDob, etChildNotes, etPersonalBest;
    private androidx.appcompat.widget.SwitchCompat switchRescue, switchController, switchSymptoms,
            switchTriggers, switchPef, switchTriage, switchCharts;
    private Date selectedDate;
    private OnChildUpdatedListener listener;

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
        }
        childRepo = new ChildRepository(); // ✅ FIXED: Now matches declaration
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_child, null);

        // Set max height for scrollable dialog
        view.setMinimumHeight(600);

        etChildName = view.findViewById(R.id.et_child_name);
        etChildDob = view.findViewById(R.id.et_child_dob);
        etChildNotes = view.findViewById(R.id.et_child_notes);
        etPersonalBest = view.findViewById(R.id.et_personal_best);

        // Sharing toggle switches
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
            // Display child name
            if (child.getName() != null && !child.getName().isEmpty()) {
                etChildName.setText(child.getName());
            }

            // Display DOB
            if (child.getDob() != null) {
                selectedDate = child.getDob();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                etChildDob.setText(sdf.format(child.getDob()));
            }

            // Display notes
            if (child.getExtraNotes() != null) {
                etChildNotes.setText(child.getExtraNotes());
            }

            // Display personal best
            if (child.getPersonalBest() > 0) {
                etPersonalBest.setText(String.valueOf(child.getPersonalBest()));
            }

            // Load sharing toggles
            if (child.getSharing() != null) {
                Map<String, Boolean> sharing = child.getSharing();
                switchRescue.setChecked(sharing.getOrDefault("rescue", true));
                switchController.setChecked(sharing.getOrDefault("controller", true));
                switchSymptoms.setChecked(sharing.getOrDefault("symptoms", true));
                switchTriggers.setChecked(sharing.getOrDefault("triggers", true));
                switchPef.setChecked(sharing.getOrDefault("pef", true));
                switchTriage.setChecked(sharing.getOrDefault("triage", true));
                switchCharts.setChecked(sharing.getOrDefault("charts", true));
            }
        }

        // Date picker for DOB
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

        // Set max date to today (can't be born in the future)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveChild() {
        if (child == null) return;

        // Get and validate name
        String name = etChildName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a child name", Toast.LENGTH_SHORT).show();
            return;
        }
        child.setName(name);

        // Validate and update DOB
        if (selectedDate != null) {
            // Validate age (must be between 6-16 years old)
            int age = calculateAge(selectedDate);
            if (age < 6 || age > 16) {
                Toast.makeText(getContext(), "Child must be between 6 and 16 years old", Toast.LENGTH_SHORT).show();
                return;
            }
            child.setDob(selectedDate);
        }

        // Update child notes
        String notes = etChildNotes.getText().toString().trim();
        child.setExtraNotes(notes.isEmpty() ? null : notes);

        // Update personal best
        String personalBestStr = etPersonalBest.getText().toString().trim();
        if (!personalBestStr.isEmpty()) {
            try {
                int personalBest = Integer.parseInt(personalBestStr);
                if (personalBest < 0) {
                    Toast.makeText(getContext(), "Personal best must be a positive number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (personalBest > 1000) {
                    Toast.makeText(getContext(), "Personal best seems too high. Please verify.", Toast.LENGTH_SHORT).show();
                    return;
                }
                child.setPersonalBest(personalBest);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number for personal best", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            child.setPersonalBest(0); // Reset to 0 if empty
        }

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

        Log.d(TAG, "Attempting to update child: " + child.getName());
        Log.d(TAG, "Personal Best: " + child.getPersonalBest());

        // Save to Firestore
        childRepo.updateChild(child,
                aVoid -> {
                    Log.d(TAG, "Child updated successfully");
                    Toast.makeText(getContext(), "Child updated successfully", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onChildUpdated();
                    }
                    dismiss();
                },
                e -> {
                    Log.e(TAG, "Error updating child", e);
                    Toast.makeText(getContext(), "Error updating child: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void setOnChildUpdatedListener(OnChildUpdatedListener listener) {
        this.listener = listener;
    }

    // Helper method to calculate age from date of birth
    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);

        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);

        // Adjust if birthday hasn't occurred yet this year
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }
}