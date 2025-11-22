package com.example.smart_air;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
import java.util.Locale;

/**
 * EditChildDialogFragment
 * This dialog allows the parent to:
 * - update the child’s date of birth
 * - add or edit child notes
 * - update the child's personal best PEF value
 * The dialog loads existing child information,
 * allows editing, & then saves updates to Firestore through ChildRepository.
 */
public class EditChildDialogFragment extends DialogFragment {

    private static final String ARG_CHILD = "child";
    private Child child;
    private ChildRepository childRepo;
    private EditText etChildDob;
    private EditText etChildNotes;
    private EditText etPersonalBest;
    private Date selectedDate;
    private OnChildUpdatedListener listener;
    private EditText etChildName;

    public interface OnChildUpdatedListener{
        void onChildUpdated();
    }


    /**
     * Factory method for creating a new EditChildDialogFragment
     * Serializes Child object into JSON so it can be passed via fragment arguments.
     */
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

        childRepo = new ChildRepository();
    }


    /// creates the dialog UI & populates it with the child's existing data
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState){

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_child, null);

        etChildName = view.findViewById(R.id.et_child_name);
        etChildDob = view.findViewById(R.id.et_child_dob);
        etChildNotes = view.findViewById(R.id.et_child_notes);
        etPersonalBest = view.findViewById(R.id.et_personal_best);
        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        // populate fields with existing data
        if (child != null) {
            // child model has name field now
            etChildName.setText(child.getName());

            if (child.getDob() != null) {
                selectedDate = child.getDob();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy",
                        Locale.getDefault());
                etChildDob.setText(sdf.format(child.getDob()));
            }

            if (child.getExtraNotes() != null) {
                etChildNotes.setText(child.getExtraNotes());
            }

            etPersonalBest.setText(String.valueOf(child.getPersonalBest()));
        }

        // date picker for DOB
        etChildDob.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> saveChild());
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    /// shows a DatePickerDialog allowing the parent to select the child's date of birth
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

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy",
                            Locale.getDefault());
                    etChildDob.setText(sdf.format(selectedDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void saveChild(){
        if (child == null) return;

        // Update child fields
        String notes = etChildNotes.getText().toString().trim();
        String personalBestStr = etPersonalBest.getText().toString().trim();

        child.setExtraNotes(notes);

        String name = etChildName.getText().toString().trim();
        if (!name.isEmpty()) {
            child.setName(name);
        }

        if (!personalBestStr.isEmpty()){
            try {
                int personalBest = Integer.parseInt(personalBestStr);
                child.setPersonalBest(personalBest);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid personal best value",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (selectedDate != null) {
            child.setDob(selectedDate);
        }

        // save to firebase
        childRepo.updateChild(child,
                aVoid -> {
                    Toast.makeText(getContext(), "Child updated successfully",
                            Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onChildUpdated();
                    }
                    dismiss();
                },
                e -> {
                    Toast.makeText(getContext(), "Error updating child: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // public void setOnChildUpdatedListener(OnChildUpdatedListener listener) {
        //this.listener = listener;
    //}

}