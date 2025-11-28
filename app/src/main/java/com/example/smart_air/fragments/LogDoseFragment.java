package com.example.smart_air.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smart_air.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogDoseFragment extends Fragment {

    private FirebaseFirestore db;
    private String uid;

    public LogDoseFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log_dose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        uid = user.getUid();

        TextView dateView = view.findViewById(R.id.text_today_date);
        LinearLayout controllerLogsContainer = view.findViewById(R.id.controllerLogsContainer);
        LinearLayout rescueLogsContainer = view.findViewById(R.id.rescueLogsContainer);

        Button backButton = view.findViewById(R.id.btn_back_log_dose);
        if (backButton != null) {
            backButton.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateView.setText(today);

        // Load existing logs
        loadLogsFor("controller", controllerLogsContainer);
        loadLogsFor("rescue", rescueLogsContainer);

        Button addControllerBtn = view.findViewById(R.id.btn_add_controller_log);
        Button addRescueBtn = view.findViewById(R.id.btn_add_rescue_log);

        if (addControllerBtn != null) {
            addControllerBtn.setOnClickListener(v ->
                    showLogDialog("controller", controllerLogsContainer));
        }

        if (addRescueBtn != null) {
            addRescueBtn.setOnClickListener(v ->
                    showLogDialog("rescue", rescueLogsContainer));
        }
    }

    // ===================== DIALOG =====================

    private void showLogDialog(String logType, LinearLayout logsContainer) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_log_dose, null);

        TextView titleText = dialogView.findViewById(R.id.text_log_title);
        RadioGroup preGroup = dialogView.findViewById(R.id.group_pre_check);
        RadioGroup postGroup = dialogView.findViewById(R.id.group_post_check);
        EditText puffsInput = dialogView.findViewById(R.id.input_puffs);
        SeekBar shortBreathSeek = dialogView.findViewById(R.id.seek_short_breath);
        TextView shortBreathValue = dialogView.findViewById(R.id.text_short_breath_value);
        Button techniqueHelperBtn = dialogView.findViewById(R.id.btn_technique_helper);
        Button saveBtn = dialogView.findViewById(R.id.btn_save_log);
        Button cancelBtn = dialogView.findViewById(R.id.btn_cancel_log);

        // Title
        if ("controller".equals(logType)) {
            titleText.setText("Log Controller Dose");
        } else {
            titleText.setText("Log Rescue Dose");
        }

        // Short breath slider label
        shortBreathSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                shortBreathValue.setText("Current: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        techniqueHelperBtn.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Technique helper UI coming soon 🙂",
                        Toast.LENGTH_SHORT).show());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String preCheck = getSelectedRadioText(preGroup);
            String postCheck = getSelectedRadioText(postGroup);
            String puffsStr = puffsInput.getText().toString().trim();
            int shortBreathRating = shortBreathSeek.getProgress();

            if (preCheck == null) {
                Toast.makeText(getContext(), "Please select how you felt before.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (postCheck == null) {
                Toast.makeText(getContext(), "Please select how you felt after.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(puffsStr)) {
                puffsInput.setError("Required");
                puffsInput.requestFocus();
                return;
            }

            int puffs;
            try {
                puffs = Integer.parseInt(puffsStr);
            } catch (NumberFormatException e) {
                puffsInput.setError("Enter a valid number");
                puffsInput.requestFocus();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("preCheck", preCheck);
            data.put("postCheck", postCheck);
            data.put("puffs", puffs);
            data.put("shortBreathRating", shortBreathRating);
            data.put("timeTaken", FieldValue.serverTimestamp());

            String collectionName = "controller".equals(logType) ? "controllerLog" : "rescueLog";

            CollectionReference logsRef = db.collection("children")
                    .document(uid)
                    .collection(collectionName);

            logsRef.add(data)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), "Dose logged!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadLogsFor(logType, logsContainer);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(),
                                "Failed to log dose: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    @Nullable
    private String getSelectedRadioText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return null;
        RadioButton rb = group.findViewById(id);
        return rb != null ? rb.getText().toString() : null;
    }

    // ===================== LOAD LOGS =====================

    private void loadLogsFor(String logType, LinearLayout container) {
        if (container == null) return;

        String collectionName = "controller".equals(logType) ? "controllerLog" : "rescueLog";

        db.collection("children")
                .document(uid)
                .collection(collectionName)
                .orderBy("timeTaken", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    container.removeAllViews();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String preCheck = doc.getString("preCheck");
                        String postCheck = doc.getString("postCheck");
                        Long puffs = doc.getLong("puffs");
                        Long shortBreathRating = doc.getLong("shortBreathRating");

                        String summary =
                                (puffs == null ? "" : (puffs + " puffs")) +
                                        (shortBreathRating == null ? "" : ", SOB: " + shortBreathRating) +
                                        (preCheck == null ? "" : ", Pre: " + preCheck) +
                                        (postCheck == null ? "" : ", Post: " + postCheck);

                        TextView logView = new TextView(getContext());
                        logView.setText(summary);
                        logView.setPadding(16, 8, 16, 8);
                        container.addView(logView);
                    }
                });
    }
}
