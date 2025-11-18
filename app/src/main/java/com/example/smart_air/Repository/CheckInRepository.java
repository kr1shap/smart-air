package com.example.smart_air.Repository;

import android.util.Log;

import com.example.smart_air.Fragments.CheckInFragment;
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
        data.put("nightWaking"+userRole, nightWaking);
        data.put("activityLimits"+userRole, activityLevel);
        data.put("coughingWheezing"+userRole, coughingValue);
        data.put("triggers"+userRole, selected);
        data.put("date", new com.google.firebase.Timestamp(todayDateOnly));
        if(userRole.equals("parent")){
            data.put("pef",pef);
        }

        String childUid = uid;
        if(userRole.equals("parent")){
            childUid = correspondingUid;
        }
        String childUidFinal = childUid;

        DocumentReference userInfo = db.collection("dailyCheckins").document(childUid); // getting the uid
        userInfo.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                // user doesn't exist yet, create it
                Map<String, Object> userData = new HashMap<>();
                userData.put("role", userRole);
                if(userRole.equals("child")){
                    userData.put("parentUid", correspondingUid);
                }
                else {
                    userData.put("parentUid", uid);
                }
                userInfo.set(userData)
                        .addOnSuccessListener(aVoid -> {
                            // add daily entry
                            addDataUser(data, context, childUidFinal);
                        })
                        .addOnFailureListener(e -> Log.e("FIRESTORE", "Failed to create user", e));
            } else {
                // user already exists so just add the daily entry
                addDataUser(data, context, childUidFinal);
            }
        });
    }

    private void addDataUser(Map<String, Object> data, CheckInFragment context, String childUid) {
        // create document id
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntryRef = db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .document(todayDocId);

        dailyEntryRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // success
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Failed to add daily entry", e);
                });

    }

    public void getUserInput(CheckInFragment activity, String userRole, String correspondingUid){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            activity.updateInfoInputWithoutValues();
        }
        String childUid = correspondingUid;
        if(userRole.equals("child")){
            String uid = user.getUid();
            childUid = uid;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntry = db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .document(todayDocId);

        dailyEntry.get().addOnSuccessListener(document ->{
            if (!(document.exists())) {
                activity.updateInfoInputWithoutValues();
                return;
            }

            if (!(document.contains("nightWaking"+userRole))){
                activity.updateInfoInputWithoutValues();
                return;
            }

            Boolean nightWaking = document.getBoolean("nightWaking"+userRole);
            Long activityLimits = document.getLong("activityLimits"+userRole);
            Long coughingWheezing = document.getLong("coughingWheezing"+userRole);
            List<String> triggers = (List<String>) document.get("triggers"+userRole);
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
        String childUid = otherUid;
        String otherUserRole = "child";
        if(userRole.equals("child")){
            String uid = user.getUid();
            childUid = uid;
            otherUserRole = "parent";
        }
        String otherUserRoleFinal = otherUserRole;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntry = db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .document(todayDocId);

        dailyEntry.get().addOnSuccessListener(document ->{
            if (!(document.exists())) {
                activity.updateInfoInputOtherWithoutValues();
                return;
            }

            if (!(document.contains("nightWaking"+otherUserRoleFinal))){
                activity.updateInfoInputOtherWithoutValues();
                return;
            }

            Boolean nightWaking = document.getBoolean("nightWaking"+otherUserRoleFinal);
            Long activityLimits = document.getLong("activityLimits"+otherUserRoleFinal);
            Long coughingWheezing = document.getLong("coughingWheezing"+otherUserRoleFinal);
            List<String> triggers = (List<String>) document.get("triggers"+otherUserRoleFinal);
            Long pef = 0L;
            if(userRole.equals("child") && document.contains("pef")){
                Long temp = document.getLong("pef");
                pef = temp != null ? temp : 400L;
            }

            activity.updateInfoInputOther(nightWaking, activityLimits, coughingWheezing, triggers, pef);
        });
    }
}

