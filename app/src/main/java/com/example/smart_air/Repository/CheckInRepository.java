package com.example.smart_air.Repository;

import android.util.Log;

import com.example.smart_air.fragments.CheckInFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckInRepository {
    private FirebaseFirestore db;

    public CheckInRepository() {
        db = FirebaseFirestore.getInstance();
    }
    public void getUserInfo(CheckInFragment activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            activity.noUserFound();
            return;
        }

        String uid = user.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        activity.noUserFound();
                        return;
                    }

                    String role = document.getString("role");
                    String correspondingUid = null;

                    if ("parent".equals(role)) {
                        List<String> list = (List<String>) document.get("childrenUid");
                        if (list != null && !list.isEmpty()) {
                            correspondingUid = list.get(0);
                        }
                    } else if ("child".equals(role)) {
                        List<String> list = (List<String>) document.get("parentUid");
                        if (list != null && !list.isEmpty()) {
                            correspondingUid = list.get(0);
                        }
                    }

                    if (correspondingUid == null){
                        activity.noUserFound();
                    }
                    activity.userInfoLoaded(role, correspondingUid);
                })
                .addOnFailureListener(e -> {
                    activity.noUserFound();
                });


    }

    public void saveUserData(CheckInFragment context, String userRole, String [] triggers, boolean [] selectedTriggers, String correspondingUid, boolean nightWaking, int activityLevel, int coughingValue, int pef) {
        // getting user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        // getting today's date
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);cal.set(Calendar.MILLISECOND, 0);
        Date todayDateOnly = cal.getTime();

        // getting triggered list
        ArrayList<String> selected = new ArrayList<String>();
        for(int i = 0; i < triggers.length; i++){
            if (selectedTriggers[i]){
                selected.add(triggers[i]);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nightWaking", nightWaking);
        data.put("activityLimits", activityLevel);
        data.put("coughingWheezing", coughingValue);
        data.put("triggers", selected);
        data.put("date", new com.google.firebase.Timestamp(todayDateOnly));
        if(pef != 0 && userRole.equals("parent")){
            data.put("pef",pef);
        }


        DocumentReference userInfo = db.collection("dailyCheckins").document(uid); // getting the uid
        userInfo.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                // user doesn't exist yet, create it
                Map<String, Object> userData = new HashMap<>();
                userData.put("role", userRole);
                if(userRole.equals("child")){
                    userData.put("parentUid", correspondingUid);
                }
                else {
                    userData.put("childrenUid", correspondingUid);
                }
                userInfo.set(userData)
                        .addOnSuccessListener(aVoid -> {
                            // add daily entry
                            addDataUser(data, context, uid);
                        })
                        .addOnFailureListener(e -> Log.e("FIRESTORE", "Failed to create user", e));
            } else {
                // user already exists so just add the daily entry
                addDataUser(data, context, uid);
            }
        });
    }

    private void addDataUser(Map<String, Object> data, CheckInFragment context, String uid) {
        // create document id
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntryRef = db.collection("dailyCheckins")
                .document(uid)
                .collection("entries")
                .document(todayDocId);

        dailyEntryRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // success
                })
                .addOnFailureListener(e -> {
                    // failure
                });

    }

    public void getUserInput(CheckInFragment activity, String userRole){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            activity.updateInfoInputWithoutValues();
        }
        String uid = user.getUid();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntry = db.collection("dailyCheckins")
                .document(uid)
                .collection("entries")
                .document(todayDocId);

        dailyEntry.get().addOnSuccessListener(document ->{
            if (!(document.exists())) {
                activity.updateInfoInputWithoutValues();
                return;
            }

            Boolean nightWaking = document.getBoolean("nightWaking");
            Long activityLimits = document.getLong("activityLimits");
            Long coughingWheezing = document.getLong("coughingWheezing");
            List<String> triggers = (List<String>) document.get("triggers");
            Long pef = 0L;
            if(userRole.equals("parent")){
                pef = document.getLong("pef");
            }

            activity.updateInfoInput(nightWaking, activityLimits, coughingWheezing, triggers, pef);
        });
    }

    public void getUserInputOther(CheckInFragment activity, String otherUid, String userRole){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            activity.updateInfoInputOtherWithoutValues();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntry = db.collection("dailyCheckins")
                .document(otherUid)
                .collection("entries")
                .document(todayDocId);

        dailyEntry.get().addOnSuccessListener(document ->{
            if (!(document.exists())) {
                activity.updateInfoInputOtherWithoutValues();
                return;
            }

            Boolean nightWaking = document.getBoolean("nightWaking");
            Long activityLimits = document.getLong("activityLimits");
            Long coughingWheezing = document.getLong("coughingWheezing");
            List<String> triggers = (List<String>) document.get("triggers");
            Long pef = 0L;
            if(userRole.equals("child")){
                pef = document.getLong("pef");
            }

            activity.updateInfoInputOther(nightWaking, activityLimits, coughingWheezing, triggers, pef);
        });
    }
}

