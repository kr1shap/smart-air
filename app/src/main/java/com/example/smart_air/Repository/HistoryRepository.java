package com.example.smart_air.Repository;

import android.util.Log;
import android.widget.LinearLayout;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.google.firebase.Timestamp;
import java.util.Calendar;

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

        Query childQuery = filterQuery(activity, childUid,"child");
        Query parentQuery = filterQuery(activity, childUid,"parent");

        List<HistoryItem> results = new ArrayList<>();

        childQuery.get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot doc : snapshot) {
                results.add(buildHistoryItem(doc));
            }

            parentQuery.get().addOnSuccessListener(snapshot2 -> {
                for (DocumentSnapshot doc : snapshot2) {
                    HistoryItem item = buildHistoryItem(doc);
                    if (!results.contains(item)) { // avoid duplicates
                        results.add(item);
                    }
                }

                Collections.sort(results, (a, b) -> b.date.compareTo(a.date));

//                for (HistoryItem item : results) {
//                    activity.createDailyCard(item);
//                }
                activity.createRecycleView(results);
            });
        });


        //getDailyCheckInsQuery(childUid,activity,q);


    }

    private HistoryItem buildHistoryItem(DocumentSnapshot doc) {
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
        return test;
    }

    public Query filterQuery(HistoryFragment activity, String childUid, String role){
        Query q = db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries");

        if(!activity.filters[0].equals("ALL")) {
            q = q.whereEqualTo("nightWaking"+role,Boolean.parseBoolean(activity.filters[0]));
        }
        if(!activity.filters[1].equals("ALL")){
            if(activity.filters[1].length() == 2){
                q = q.whereEqualTo("activityLimits"+role,10);
            }
            else{
                String[] parts = activity.filters[1].split("-");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());

                q = q.whereGreaterThanOrEqualTo("activityLimits" + role, min)
                        .whereLessThanOrEqualTo("activityLimits" + role, max);
            }
        }
        if(!activity.filters[2].equals("ALL")){
            if(activity.filters[2].equals("No Coughing")){
                q = q.whereEqualTo("coughingWheezing"+role,0);
            }
            else if(activity.filters[2].equals("Wheezing")){
                q = q.whereEqualTo("coughingWheezing"+role,1);
            }
            else if(activity.filters[2].equals("Coughing")){
                q = q.whereEqualTo("coughingWheezing"+role,2);
            }
            else{
                q = q.whereEqualTo("coughingWheezing"+role,3);
            }
        }
        if(!activity.filters[3].equals("ALL")){
            q = q.whereArrayContains("triggers"+role, activity.filters[3]);
        }
        Calendar cal = Calendar.getInstance();
        if(activity.filters[4].equals("ALL")){
            cal.add(Calendar.MONTH, -6);
        }
        else if(activity.filters[4].equals("Past 3 months")){
            cal.add(Calendar.MONTH, -3);
        }
        else if(activity.filters[4].equals("Past month")){
            cal.add(Calendar.MONTH, -1);
        }
        else {
            cal.add(Calendar.WEEK_OF_YEAR, -2);
        }
        Timestamp timeFrame = new Timestamp(cal.getTime());
        q = q.whereGreaterThanOrEqualTo("date", timeFrame);

        return q;
    }

}
