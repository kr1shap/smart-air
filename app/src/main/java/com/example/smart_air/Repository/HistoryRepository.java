package com.example.smart_air.Repository;

import android.util.Log;

import com.example.smart_air.fragments.HistoryFragment;
import com.example.smart_air.modelClasses.HistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryRepository {
    private FirebaseFirestore db;

    public HistoryRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void getChildUid(HistoryFragment activity) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String currentUid = auth.getCurrentUser().getUid();

        db.collection("users").document(currentUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            boolean isChild = "child".equals(document.getString("role"));
                            String childUid;

                            if (isChild) {
                                childUid = currentUid;
                                activity.setChildUid(childUid);
                                return;
                            }
                            List<String> childrenUid = (List<String>) document.get("childrenUid");
                            if (childrenUid != null && !childrenUid.isEmpty()) {
                                activity.setChildUid(childrenUid.get(0));
                                return;
                            }

                            activity.exitScreen();
                        }
                    }
                });
    }

    public void getDailyCheckIns(String childUid, HistoryFragment activity){
        DocumentReference userDocRef = db.collection("dailyCheckIns").document(childUid);
        CollectionReference entriesRef = userDocRef.collection("entries");

        db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getId(); // the date document ID
                        boolean nightWakingChild = doc.contains("nightWakingchild") && doc.getBoolean("nightWakingchild");
                        boolean nightWakingParent = doc.contains("nightWakingparent") && doc.getBoolean("nightWakingparent");
                        int activityLimitsChild = Math.toIntExact(doc.contains("activityLimitschild") ? doc.getLong("activityLimitschild") : -5);
                        int activityLimitsParent = Math.toIntExact(doc.contains("activityLimitsparent") ? doc.getLong("activityLimitsparent") : -5);
                        int coughingWheezingChild = Math.toIntExact(doc.contains("coughingWheezingchild") ? doc.getLong("coughingWheezingchild") : -5);
                        int coughingWheezingParent = Math.toIntExact(doc.contains("coughingWheezingparent") ? doc.getLong("coughingWheezingparent") : -5);
                        int pef = Math.toIntExact(doc.contains("pef") ? doc.getLong("pef"): -5);
                        List<String> triggersChild = new ArrayList<>();
                        List<String> triggersParent = new ArrayList<>();
                        if (doc.contains("triggerschild")) {
                            triggersChild = (List<String>) doc.get("triggerschild");
                        }
                        if (doc.contains("triggersparent")) {
                            triggersParent = (List<String>) doc.get("triggersparent");
                        }
                        HistoryItem test = new HistoryItem(date,nightWakingChild,nightWakingParent,activityLimitsChild,activityLimitsParent,coughingWheezingChild,coughingWheezingParent,triggersChild,triggersParent,pef);
                        activity.createDailyCard(test);
                    }
                })
                .addOnFailureListener(error -> Log.e("FIRE", "Query failed", error));
    }
}
