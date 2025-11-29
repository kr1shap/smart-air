package com.example.smart_air.Repository;

import android.util.Log;

import com.example.smart_air.fragments.CheckInFragment;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.modelClasses.enums.NotifType;
import com.google.firebase.Timestamp;
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

    // function gets parent's uid if it's a child
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

                    if ("child".equals(role)) {
                        List<String> list = (List<String>) document.get("parentUid");
                        if (list != null && !list.isEmpty()) {
                            correspondingUid = list.get(0);
                        } else {
                            activity.noUserFound();
                            return;
                        }
                    }
                    else{
                        correspondingUid = "";
                    }

                    if (correspondingUid == null){
                        activity.noUserFound();
                        return;
                    }
                    activity.userInfoLoaded(correspondingUid);
                })
                .addOnFailureListener(e -> {
                    activity.noUserFound();
                });


    }

    // function saves data into a map to be but into db
    public void saveUserData(CheckInFragment context, String userRole, String [] triggers, boolean [] selectedTriggers, String correspondingUid, boolean nightWaking, int activityLevel, int coughingValue, int pef,int pre, int post) {
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

        // putting info into a map
        Map<String, Object> data = new HashMap<>();
        data.put("nightWaking"+userRole, nightWaking);
        data.put("activityLimits"+userRole, activityLevel);
        data.put("coughingWheezing"+userRole, coughingValue);
        data.put("triggers"+userRole, selected);
        data.put("date", new com.google.firebase.Timestamp(todayDateOnly));
        if(userRole.equals("parent") && pef != -5){
            data.put("pef",pef);
            if(pre != 0 && post != 0){
                data.put("pre",pre);
                data.put("post",post);
            }
            data.put("zoneColour",context.zoneColour(pef));
            data.put("zoneNumber", context.zoneNumber(pef));
        }

        String childUid = uid;
        if(userRole.equals("parent")){
            childUid = correspondingUid;
        }
        String childUidFinal = childUid;

        // creating document for child in dailyCheckIns if they don't already have one
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

    // adding data map into dailyCheckIns collection for that child
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

    // get's user input to update form with previous submission for the day
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
            int pre = 0;
            int post = 0;
            if(userRole.equals("parent") && document.contains("pef")){
                pef = document.getLong("pef");
                if(document.contains("pre") && document.contains("post")) {
                    pre = Math.toIntExact(document.getLong("pre"));
                    post = Math.toIntExact(document.getLong("post"));
                }
            }

            activity.updateInfoInput(nightWaking, activityLimits, coughingWheezing, triggers, pef,pre,post);
        });
    }

    // get's other user input to update form with previous submission for the day
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

    // checks to see if it's already in red before this save
    public void checkIfRed(String childUid, String uid, CheckInFragment activity) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntry = db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .document(todayDocId);

        dailyEntry.get().addOnSuccessListener(document ->{
            if (!(document.exists())) {
                sendRedZoneNotification(uid, activity);
                return;
            }

            if (!document.contains("zoneColour")) {
                sendRedZoneNotification(uid, activity);
                return;
            }

            String zone = document.getString("zoneColour");
            if (zone != null && zone.equals("red")) {
                return;
            }
            sendRedZoneNotification(uid, activity);
        });
    }

    // sends a notification for being red zone
    public void sendRedZoneNotification(String uid, CheckInFragment activity){
        NotificationRepository notifRepo = new NotificationRepository();
        Notification notif = new Notification(activity.correspondingUid, false, Timestamp.now(), NotifType.RED_ZONE);
        notifRepo.createNotification(uid, notif)
                .addOnSuccessListener(aVoid -> {
                    Log.d("NotificationRepo", "Notification created successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e("NotificationRepo", "Failed to create notification", e);
                });
    }

    public interface PefCallback {
        void onResult(int maxPef);
    }

    // getting max pef so pef is only updated with higher number
    public void maxPef(String correspondingUid, String userRole, int inputPef, PefCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(inputPef);
            return;
        }

        String childUid = userRole.equals("child") ? user.getUid() : correspondingUid;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDocId = sdf.format(new Date());

        DocumentReference dailyEntry = db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .document(todayDocId);

        dailyEntry.get().addOnSuccessListener(document -> {

            int firestorePef = inputPef;

            if (document.exists() && document.contains("pef")) {
                firestorePef = Math.toIntExact(document.getLong("pef"));
            }

            int maxValue = Math.max(inputPef, firestorePef);
            callback.onResult(maxValue);

        }).addOnFailureListener(e -> {
            callback.onResult(inputPef);
        });
    }
}

