package com.example.smart_air.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.R;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.modelClasses.enums.NotifType;
import com.example.smart_air.modelClasses.formatters.StringFormatters;
import com.example.smart_air.viewmodel.ChildTogglesViewModel;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.firestore.DocumentReference;
import java.util.Calendar;
import java.util.Set;
import java.util.ArrayList;



public class LogDoseFragment extends Fragment {

    private FirebaseFirestore db;
    private String uid;
    int threshold = 300;
    double lessthan20 = threshold * 0.2;
    //VM For the toggles (parent and provider)
    private ChildTogglesViewModel togglesVM;
    private SharedChildViewModel sharedModel;
    private String userRole = "";
    // Keep references so we can reload logs when the child changes
    private LinearLayout controllerLogsContainer;
    private LinearLayout rescueLogsContainer;
    private TextView dateView, text_rescue_section_title;
    private CardView rescueCard;
    //buttons for disabling for providers
    private Button btn_add_controller_log, btn_add_rescue_log;

    public LogDoseFragment() {
    }

    //toggle cache
    private Map<String, Boolean> toggleCache; //for rescue toggles
    //For shared label tag on top
    View sharedProviderLabel;
    TextView sharedLabelText;
    Button addControllerBtn, addRescueBtn;
    String afterDose = "";

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
        //instantiate buttons
        btn_add_controller_log = view.findViewById(R.id.btn_add_controller_log);
        btn_add_rescue_log = view.findViewById(R.id.btn_add_rescue_log);
        //initalize other containers
        dateView = view.findViewById(R.id.text_today_date);
        controllerLogsContainer = view.findViewById(R.id.controllerLogsContainer);
        rescueLogsContainer = view.findViewById(R.id.rescueLogsContainer);
        rescueCard = view.findViewById(R.id.card_rescue_logs);
        text_rescue_section_title = view.findViewById(R.id.text_rescue_section_title);
        //shared tag label
        sharedProviderLabel = view.findViewById(R.id.sharedProviderLabel);
        sharedLabelText = view.findViewById(R.id.sharedLabelText);
        // hook into the shared child viewmodel
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        togglesVM = new ViewModelProvider(this).get(ChildTogglesViewModel.class);

        //isntantiate toggle map
        toggleCache = new HashMap<>();

        //back button
        Button backButton = view.findViewById(R.id.btn_back_log_dose);
        if (backButton != null) {
            backButton.setOnClickListener(v ->
                    requireActivity()
                            .getSupportFragmentManager()
                            .popBackStack());
        }

        //buttons in containers
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateView.setText(today);

        addControllerBtn = view.findViewById(R.id.btn_add_controller_log);
        addRescueBtn = view.findViewById(R.id.btn_add_rescue_log);

        //onclick for buttons
        if (addControllerBtn != null) {
            addControllerBtn.setOnClickListener(v ->
                    showLogDialog("controller", controllerLogsContainer));
        }

        if (addRescueBtn != null) {
            addRescueBtn.setOnClickListener(v ->
                    showLogDialog("rescue", rescueLogsContainer));
        }

        // observe role
        sharedModel.getCurrentRole().observe(
                getViewLifecycleOwner(),
                role -> {
                    if (role != null) {
                        userRole = role;
                    }
                    //role check for page access
                    if ("provider".equals(userRole)) {
                        //Disable log dose buttons for providers
                        btn_add_rescue_log.setVisibility(View.GONE);
                        btn_add_controller_log.setVisibility(View.GONE);
                    } else {
                        //enable log dose buttons for providers
                        btn_add_rescue_log.setVisibility(View.VISIBLE);
                        btn_add_controller_log.setVisibility(View.VISIBLE);
                    }
                    if ("child".equals(userRole)) {
                        uid = FirebaseAuth.getInstance().getUid();
                        //directly load logs as VM not applicable to them
                        loadLogsFor("controller", controllerLogsContainer);
                        loadLogsFor("rescue", rescueLogsContainer);
                    } //current child
                }
        );

        // when the list of children is available, try to set the current uid from it
        sharedModel.getAllChildren().observe(
                getViewLifecycleOwner(),
                children -> {
                    if (children != null && !children.isEmpty()) {
                        Integer idx = sharedModel.getCurrentChild().getValue();
                        int safeIndex = (idx != null && idx >= 0 && idx < children.size()) ? idx : 0;
                        Child currentChild = children.get(safeIndex);
                        if (currentChild != null) {
                            uid = currentChild.getChildUid();
                            if (userRole.equals("provider"))
                                togglesVM.attachChildListener(uid); //attach listener for provider
                            loadLogsFor("controller", controllerLogsContainer);
                            loadLogsFor("rescue", rescueLogsContainer);
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
                                togglesVM.attachChildListener(uid); //attach listener for provider and parent
                                if (controllerLogsContainer != null && rescueLogsContainer != null) {
                                    loadLogsFor("controller", controllerLogsContainer);
                                    if (userRole.equals("provider") && toggleCache.get(uid) != null && !toggleCache.get(uid)) {
                                    } //do nothing
                                    else loadLogsFor("rescue", rescueLogsContainer);
                                }
                            }
                        }
                    }
                }
        );

        //change the toggles for log dose (provider/parent only - read)
        togglesVM.getSharingToggles().observe(getViewLifecycleOwner(), sharing -> {
            Boolean allowRescue = false;
            //change sharing toggles
            if (sharing != null) {
                allowRescue = sharing.getOrDefault("rescue", false);
                toggleCache.put(uid, allowRescue);
            }
            //check rescue permissions
            if (!allowRescue && userRole.equals("provider")) {
                rescueCard.setVisibility(View.GONE);
                text_rescue_section_title.setVisibility(View.GONE);
            } else {
                if (userRole.equals("parent")) applySharingTogglesParent(allowRescue);
                rescueCard.setVisibility(View.VISIBLE);
                text_rescue_section_title.setVisibility(View.VISIBLE);
            }
        });
    }

    //Setup the 'shared with provider tag' for the parent
    private void applySharingTogglesParent(Boolean allowRescue) {
        //now apply the 'visible to provider' tag on top
        if (allowRescue == null || !allowRescue) {
            sharedLabelText.setText("Shared with Provider: None");
            sharedProviderLabel.setVisibility(View.VISIBLE);
            return;
        }
        sharedLabelText.setText("Shared with Provider: Rescue logs");
        sharedProviderLabel.setVisibility(View.VISIBLE);
    }

    @Nullable
    private String getSelectedRadioText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return null;
        RadioButton rb = group.findViewById(id);
        return rb != null ? rb.getText().toString() : null;
    }

    private void showLogDialog(String logType, LinearLayout logsContainer) {
        if (getContext() == null) return;
        if (userRole == null || userRole.trim().isEmpty()) return;
        if (userRole.equals("provider")) return;

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

        //DO NOT open technique helper for parent
        if (userRole.equals("parent")) techniqueHelperBtn.setVisibility(View.GONE);
        else techniqueHelperBtn.setVisibility(View.VISIBLE);

        titleText.setText("controller".equals(logType) ? "Log Controller Dose" : "Log Rescue Dose");

        shortBreathSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                shortBreathValue.setText("Current: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        techniqueHelperBtn.setOnClickListener(v ->
        {
            dialog.dismiss();
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TechniqueHelperFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String preCheck = getSelectedRadioText(preGroup);
            String postCheck = getSelectedRadioText(postGroup);
            afterDose = postCheck;
            String puffsStr = puffsInput.getText().toString().trim();
            int shortBreathRating = shortBreathSeek.getProgress();

            if (preCheck == null || postCheck == null || TextUtils.isEmpty(puffsStr)) {
                Toast.makeText(getContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            int puffs;
            try {
                puffs = Integer.parseInt(puffsStr);
                if (puffs < 0) {
                    Toast.makeText(getContext(), "Cannot have negative puffs.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                puffsInput.setError("Enter a valid number");
                return;
            }

            if (uid == null || uid.isEmpty()) {
                Toast.makeText(getContext(), "Child not selected yet, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            String medCollection = "controller".equals(logType) ? "controllerLog" : "rescueLog";

            getAndUpdateInventory(logType, puffs, new AuthContract.GeneralCallback() {
                        @Override
                        public void onSuccess() {
                            //send alert
                            if (afterDose.equals("Worse")) sendAlert(uid, 2);
                            afterDose = ""; //reset
                            //only log dose on success
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
                                        docRef.get().addOnSuccessListener(snapshot -> {
                                            Toast.makeText(getContext(), "Dose logged!", Toast.LENGTH_SHORT).show();
                                            // update badge and stat
                                            if ("rescue".equals(logType)) {
                                                rapidrescuealerts();
                                                updateLowRescueBadge(); // low rescue badge
                                            } else if ("controller".equals(logType)) {
                                                updateControllerBadgeAndStreak(); // controller badge & streak
                                            }
                                            // refresh the UI
                                            dialog.dismiss();
                                            loadLogsFor(logType, logsContainer);
                                        }).addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Dose logged but failed to read timestamp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(), "Failed to log dose: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onFailure(String s) {
                            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        dialog.show();
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
                        logView.setTextColor(Color.BLACK);
                        logView.setText(summary);
                        logView.setPadding(16, 8, 16, 8);
                        container.addView(logView);
                    }
                });
    }

    private void getAndUpdateInventory(String medType, int puffs, AuthContract.GeneralCallback callback) {
        if (uid == null || uid.isEmpty()) return;

        //structure: children/{uid}/inventory/{controller|rescue}
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
                            callback.onFailure("Inventory data is corrupted or missing.");
                            return;
                        }

                        long currentAmount = currentAmountLong;

                        if (currentAmount < puffs) {
                            callback.onFailure("Not enough doses left in inventory");
                            return;
                        }
                        long updatedAmount = currentAmount - puffs;
                        // sends alert if medication is less than 20% of threshold
                        if (updatedAmount <= lessthan20) {
                            Toast.makeText(requireContext(), "Sent low inventory alert!", Toast.LENGTH_SHORT).show();
                            sendAlert(uid, 0); //since only parent has access to inventory
                        }
                        // update the *same doc* we just read
                        doc.getReference()
                                .update("amount", updatedAmount)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(getContext(),
                                            "Inventory updated.",
                                            Toast.LENGTH_SHORT).show();
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e ->
                                {
                                    callback.onFailure("Failed to update inventory.");
                                });

                    } else {
                        Toast.makeText(getContext(),
                                "Inventory not set up yet.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error accessing inventory.");
                });
    }

    // rapid rescue alerts section
   /*
   check if 3+ rescue attempts made in 3 hours
    */
    public void rapidrescuealerts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String childUid;
        if ("child".equals(userRole)) {
            childUid = user.getUid();
        } else {
            childUid = uid;
        }
        if (childUid == null || childUid.isEmpty()) {
            Log.e("RescueCheck", "childUid is null");
            return;
        }
        long now = System.currentTimeMillis();
        long threeHours = 3 * 60 * 60 * 1000;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .document(childUid)
                .collection("rescueLog")
                .get()
                .addOnSuccessListener(query -> {
                    int count = 0;
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("timeTaken");
                        if (ts != null) {
                            long rescueTime = ts.toDate().getTime();
                            if (now - rescueTime <= threeHours) {
                                count++;
                            }
                            if (count >= 3) break;
                        }
                    }
                    if (count == 3) {
                        sendAlert(childUid, 1);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RescueCheck", "Failed to read rescueLog: ", e);
                });
    }

    /*
     used to send rapid rescue or inventory notifications to all parents
    */
    public void sendAlert(String cUid, int choice) {
        if (cUid == null) {
            Log.e("Rapid Rescue", "sendAlert called with null childUid");
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(cUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.e("Rapid Rescue", "Child user document missing");
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    List<String> parentUids = (List<String>) doc.get("parentUid");
                    if (parentUids == null || parentUids.isEmpty()) {
                        Log.e("Rapid Rescue", "No parentUid array found");
                        return;
                    }
                    NotificationRepository notifRepo = new NotificationRepository();
                    for (String pUid : parentUids) {
                        if (pUid == null) {
                            continue;
                        }
                        NotifType type;
                        if (choice == 2) {
                            type = NotifType.WORSE_DOSE;
                        } else if (choice == 1) {
                            type = NotifType.RAPID_RESCUE;
                        } else {
                            type = NotifType.INVENTORY;
                        }
                        Notification notif = new Notification(cUid, false, Timestamp.now(), type);
                        NotifType finalType = type;
                        notifRepo.createNotification(pUid, notif)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("NotificationRepo", "Notification (" + finalType + ") created for parent " + pUid))
                                .addOnFailureListener(e ->
                                        Log.e("NotificationRepo", "Failed to create notification for " + pUid, e));
                    }


                })
                .addOnFailureListener(e ->
                        Log.e("Rapid Rescue", "Failed to load child document", e));
    }

    // low rescue badge (last 30 days)

    private void updateLowRescueBadge() {
        if (uid == null || uid.isEmpty()) return;
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        // Reference to this child document
        DocumentReference childRef =
                db.collection("children").document(uid);

        // Read thresholds from child doc
        childRef.get().addOnSuccessListener((DocumentSnapshot childDoc) -> {
            if (!childDoc.exists()) {
                return;
            }

            // default threshold = 4 rescue days / 30 days
            final long[] rescueThresh = new long[]{4L};

            @SuppressWarnings("unchecked")
            Map<String, Object> thresholds =
                    (Map<String, Object>) childDoc.get("thresholds");

            if (thresholds != null && thresholds.get("rescue_thresh") instanceof Number) {
                rescueThresh[0] = ((Number) thresholds.get("rescue_thresh")).longValue();
            }

            // load all rescue logs and count distinct days in last 30 days
            childRef.collection("rescueLog")
                    .get()
                    .addOnSuccessListener((QuerySnapshot qs) -> {
                        List<String> rescueDays = new ArrayList<>();
                        SimpleDateFormat fmt =
                                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        long now = System.currentTimeMillis();
                        long thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L;
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            com.google.firebase.Timestamp ts = doc.getTimestamp("timeTaken");
                            if (ts != null) {
                                long t = ts.toDate().getTime();
                                // only count rescues within last 30 days
                                if (now - t <= thirtyDaysMillis) {
                                    rescueDays.add(fmt.format(ts.toDate()));
                                }
                            }
                        }
                        // count unique days
                        Set<String> uniqueDays = new HashSet<>(rescueDays);
                        int rescueDayCount = uniqueDays.size();
                        boolean achieved = (rescueDayCount <= rescueThresh[0]);

                        //update badge in Firebase
                        childRef.update("badges.lowRescueBadge", achieved);
                    });
        });
    }


// controller and adherence badge (last 7 days)

    private void updateControllerBadgeAndStreak() {
        if (uid == null || uid.isEmpty()) return;

        DocumentReference childRef = db.collection("children").document(uid);

        childRef.get().addOnSuccessListener(childDoc -> {
            if (!childDoc.exists()) return;
            //Fetch the thresholds and weekly schedule
            @SuppressWarnings("unchecked")
            Map<String, Boolean> weeklySchedule = (Map<String, Boolean>) childDoc.get("weeklySchedule");
            //If any of them are null or empty, just default badge as false and return
            if (weeklySchedule == null || weeklySchedule.isEmpty()) {
                childRef.update("badges.lowRescueBadge", false); //no weekly schedule, so default false
                return; // nothing to compute against
            }
            //make planned days for last 7 days of the week
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.List<String> plannedDates = new java.util.ArrayList<>();

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            cal.add(Calendar.DAY_OF_YEAR, -6); // start 6 days ago
            Date startOfWindow = cal.getTime();
            //iterate through 7 day window and check if the week is all good
            for (int i = 0; i < 7; i++) {
                Date day = cal.getTime();
                String dow = StringFormatters.dayNameForCalendar(cal); //grab the 'Monday' ... type fields of the week
                if (weeklySchedule.containsKey(dow) && weeklySchedule.get(dow)) {
                    plannedDates.add(dateFmt.format(day));
                } //find planned dates (YYYY-MM-DD)
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            //If there are no dates, then return (no planned dates - no streak to follow)
            if (plannedDates.isEmpty()) {
                childRef.update("controllerStats.currentStreak", 0); //no date - no streak
                childRef.update("controllerStats.controllerBadge", false);
                return;
            }

            //FIRST COMPUTE CONTROLLER STREAK!
            Calendar todayCal = Calendar.getInstance();
            Timestamp lastPlannedTs = childDoc.getTimestamp("controllerStats.lastPlannedDay");
            Long streakLong = childDoc.getLong("controllerStats.plannedDayStreak");
            int currentStreak = streakLong != null ? streakLong.intValue() : 0; //current streak of controller
            Date lastPlannedDay = lastPlannedTs != null ? lastPlannedTs.toDate() : null;
            String todayDow = StringFormatters.dayNameForCalendar(todayCal); //already set to today
            boolean todayIsPlanned = weeklySchedule.getOrDefault(todayDow, false); //check if today is planned date
            // today is a planned day - check if streak needs to be changed
            if (todayIsPlanned) {
                // If lastPlannedDay is null or DNE (first log), start streak at 1
                if (lastPlannedDay == null) {
                    currentStreak = 1;
                } else {
                    int plannedDaysBetween = countPlannedDaysBetween(lastPlannedDay, new Date(), weeklySchedule);
                    if (plannedDaysBetween == 0) {
                        //dont increment if logged today
                    } else if (plannedDaysBetween == 1) {
                        //a consecutive day, so log it
                        currentStreak++;
                    } else {
                        //broke the streak
                        currentStreak = 1;
                    }
                }
            }

            childRef.update("controllerStats.lastPlannedDay", new Timestamp(todayCal.getTime()));
            childRef.update("controllerStats.plannedDayStreak", currentStreak);


            // Query controllerLog within that window
            childRef.collection("controllerLog")
                    .whereGreaterThanOrEqualTo("timeTaken", startOfWindow)
                    .get()
                    .addOnSuccessListener(qs -> {
                        Set<String> controllerDates = new java.util.HashSet<>();
                        //convert into timestamp
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            Timestamp ts = doc.getTimestamp("timeTaken");
                            if (ts != null) {
                                controllerDates.add(dateFmt.format(ts.toDate()));
                            }
                        }
                        //get size of our planned counts of days
                        int plannedCount = plannedDates.size();
                        int daysWithDose = 0;
                        //iterate through all planned dates and check if there is an instance in controller dates
                        for (String d : plannedDates) {
                            if (controllerDates.contains(d)) {
                                daysWithDose++;
                            }
                        }
                        boolean perfectWeek = (plannedCount > 0 && daysWithDose >= plannedCount);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("badges.controllerBadge", perfectWeek);
                        if (!updates.isEmpty()) {
                            childRef.update(updates);
                        }
                    });
        });
    }


    //Function counts the #of planned days in between
    private int countPlannedDaysBetween(Date lastDate, Date today, Map<String, Boolean> weeklySchedule) {
        if (lastDate == null) return 1; //none so the first planned day
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);
        int count = 0;
        cal.add(Calendar.DAY_OF_YEAR, 1); // start from next day after lastDate (and then check the gap)
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(today);
        //iterate until today
        while (!cal.after(endCal)) {
            String dow = StringFormatters.dayNameForCalendar(cal); //get the Day
            if (weeklySchedule.getOrDefault(dow, false)) {
                count++;        //count scheduled days
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);   //iterate through days
        }
        return count;
    }
}