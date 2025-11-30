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
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.viewmodel.SharedChildViewModel;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LogDoseFragment extends Fragment {

    private FirebaseFirestore db;
    /** This will always hold the *current child’s* UID (or the child user’s own UID). */
    private String uid;

    private SharedChildViewModel sharedModel;
    private String userRole = "";

    // Keep references so we can reload logs when the child changes
    private LinearLayout controllerLogsContainer;
    private LinearLayout rescueLogsContainer;
    private TextView dateView;

    public LogDoseFragment() {}

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

        // --- hook into the shared child viewmodel ---
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);

        // Observe role (might be useful if you need different behaviour later)
        sharedModel.getCurrentRole().observe(
                getViewLifecycleOwner(),
                role -> {
                    if (role != null) {
                        userRole = role;
                    }
                }
        );

        // When the list of children is available, try to set the current uid from it
        sharedModel.getAllChildren().observe(
                getViewLifecycleOwner(),
                children -> {
                    if (children != null && !children.isEmpty()) {
                        Integer idx = sharedModel.getCurrentChild().getValue();
                        int safeIndex = (idx != null && idx >= 0 && idx < children.size()) ? idx : 0;

                        Child currentChild = children.get(safeIndex);
                        if (currentChild != null) {
                            uid = currentChild.getChildUid(); // <-- per-child UID
                            if (controllerLogsContainer != null && rescueLogsContainer != null) {
                                // just reload logs if UI already set up
                                loadLogsFor("controller", controllerLogsContainer);
                                loadLogsFor("rescue", rescueLogsContainer);
                            } else {
                                // first time we have a uid, set up the UI
                                setupLogDoseUI(view);
                            }
                        }
                    }
                }
        );

        // When the *index* changes (user switches child), update uid & reload logs
        sharedModel.getCurrentChild().observe(
                getViewLifecycleOwner(),
                idx -> {
                    List<Child> children = sharedModel.getAllChildren().getValue();
                    if (children != null && !children.isEmpty() && idx != null) {
                        if (idx >= 0 && idx < children.size()) {
                            Child currentChild = children.get(idx);
                            if (currentChild != null) {
                                uid = currentChild.getChildUid();
                                if (controllerLogsContainer != null && rescueLogsContainer != null) {
                                    loadLogsFor("controller", controllerLogsContainer);
                                    loadLogsFor("rescue", rescueLogsContainer);
                                }
                            }
                        }
                    }
                }
        );

        // Immediate attempt: if SharedChildViewModel already has children, use them.
        List<Child> childrenNow = sharedModel.getAllChildren().getValue();
        if (childrenNow != null && !childrenNow.isEmpty()) {
            Integer idx = sharedModel.getCurrentChild().getValue();
            int safeIndex = (idx != null && idx >= 0 && idx < childrenNow.size()) ? idx : 0;
            Child currentChild = childrenNow.get(safeIndex);
            if (currentChild != null) {
                uid = currentChild.getChildUid();
                setupLogDoseUI(view);
                return;
            }
        }

        // Fallback: old behaviour (in case ViewModel is not populated for some reason)
        String currentUid = user.getUid();
        db.collection("children")
                .whereEqualTo("parentUid", currentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // use first child as before
                        uid = querySnapshot.getDocuments().get(0).getId();
                    } else {
                        // treat user as child
                        uid = currentUid;
                    }
                    setupLogDoseUI(view);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to identify user / child", Toast.LENGTH_SHORT).show());
    }

    private void setupLogDoseUI(View view) {
        dateView = view.findViewById(R.id.text_today_date);
        controllerLogsContainer = view.findViewById(R.id.controllerLogsContainer);
        rescueLogsContainer = view.findViewById(R.id.rescueLogsContainer);

        Button backButton = view.findViewById(R.id.btn_back_log_dose);
        if (backButton != null) {
            backButton.setOnClickListener(v ->
                    requireActivity()
                            .getSupportFragmentManager()
                            .popBackStack());
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateView.setText(today);

        // Load logs for the currently selected child
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

        titleText.setText("controller".equals(logType) ? "Log Controller Dose" : "Log Rescue Dose");

        shortBreathSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                shortBreathValue.setText("Current: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        techniqueHelperBtn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Technique helper UI coming soon 🙂", Toast.LENGTH_SHORT).show());

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

            if (preCheck == null || postCheck == null || TextUtils.isEmpty(puffsStr)) {
                Toast.makeText(getContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            int puffs;
            try {
                puffs = Integer.parseInt(puffsStr);
            } catch (NumberFormatException e) {
                puffsInput.setError("Enter a valid number");
                return;
            }

            if (uid == null || uid.isEmpty()) {
                Toast.makeText(getContext(), "Child not selected yet, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            String medCollection = "controller".equals(logType) ? "controllerLog" : "rescueLog";

            Map<String, Object> data = new HashMap<>();
            data.put("preCheck", preCheck);
            data.put("postCheck", postCheck);
            data.put("puffs", puffs);
            data.put("shortBreathRating", shortBreathRating);
            data.put("timeTaken", FieldValue.serverTimestamp());

            db.collection("children")
                    .document(uid)
                    .collection(medCollection)
                    .add(data)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), "Dose logged!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadLogsFor(logType, logsContainer);
                        getAndUpdateInventory(logType, puffs);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to log dose: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private void loadLogsFor(String logType, LinearLayout container) {
        if (container == null || uid == null || uid.isEmpty()) return;

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

                        com.google.firebase.Timestamp timestamp = doc.getTimestamp("timeTaken");
                        String formattedDate = "";
                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
                        }

                        String summary = (formattedDate.isEmpty() ? "" : formattedDate + " - ") +
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

    private void getAndUpdateInventory(String medType, int puffs) {
        if (uid == null || uid.isEmpty()) return;

        // Firestore structure: children/{uid}/inventory/{controller|rescue}
        String docName = "controller".equals(medType) ? "controller" : "rescue";

        db.collection("children")
                .document(uid)
                .collection("inventory")
                .document(docName)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("amount")) {
                        Long currentAmountLong = doc.getLong("amount");

                        if (currentAmountLong == null) {
                            Toast.makeText(getContext(),
                                    "Inventory data is corrupted or missing.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        long currentAmount = currentAmountLong;

                        if (currentAmount < puffs) {
                            Toast.makeText(getContext(),
                                    "Not enough doses left in inventory.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        long updatedAmount = currentAmount - puffs;

                        // update the *same doc* we just read
                        doc.getReference()
                                .update("amount", updatedAmount)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(getContext(),
                                                "Inventory updated.",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to update inventory.",
                                                Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(getContext(),
                                "Inventory not set up yet.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Error accessing inventory.",
                                Toast.LENGTH_SHORT).show());
    }


    /*
    private void getAndUpdateInventory(String medType, int puffs) {
        if (uid == null || uid.isEmpty()) return;

        String collection = "controller".equals(medType) ? "controllerInventory" : "rescueInventory";

        db.collection("children")
                .document(uid)
                .collection(collection)
                .document("main")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("amount")) {
                        Long currentAmountLong = doc.getLong("amount");

                        if (currentAmountLong == null) {
                            Toast.makeText(getContext(), "Inventory data is corrupted or missing.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        long currentAmount = currentAmountLong;

                        if (currentAmount < puffs) {
                            Toast.makeText(getContext(), "Not enough doses left in inventory.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        long updatedAmount = currentAmount - puffs;

                        db.collection("children")
                                .document(uid)
                                .collection(collection)
                                .document("main")
                                .update("amount", updatedAmount)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(getContext(), "Inventory updated.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed to update inventory.", Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(getContext(), "Inventory not set up yet.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error accessing inventory.", Toast.LENGTH_SHORT).show());
    }
     */
}
