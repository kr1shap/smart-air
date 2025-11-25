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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
                            activity.setChildUid("");

                            activity.exitScreen();
                        }
                    }
                });
    }



    public void getCards(String childUid, HistoryFragment activity){
        DocumentReference userDocRef = db.collection("dailyCheckIns").document(childUid);
        CollectionReference entriesRef = userDocRef.collection("entries");

        Query childQuery = filterQuery(activity, childUid,"child");
        Query parentQuery = filterQuery(activity, childUid,"parent");

        List<HistoryItem> results = new ArrayList<>();

        childQuery.get().addOnSuccessListener(dailyChild -> {
            for (DocumentSnapshot doc : dailyChild) {
                results.add(buildDailyItem(doc));
            }

            parentQuery.get().addOnSuccessListener(dailyParent -> {
                for (DocumentSnapshot doc : dailyParent) {
                    HistoryItem item = buildDailyItem(doc);
                    if (!results.contains(item)) {
                        results.add(item);
                    }
                }

                db.collection("incidentLog")
                                .document(childUid)
                                        .collection("triageSessions")
                                                .get()
                                                        .addOnSuccessListener(triageDoc -> {
                                                            for(DocumentSnapshot doc: triageDoc){
                                                                HistoryItem triageItem = builtTriageItem(doc);
                                                                results.add(triageItem);
                                                            }

                                                            removeUnPassed(results,activity);
                                                            Collections.sort(results, (a, b) -> b.accDate.compareTo(a.accDate));

                                                            activity.createRecycleView(results);
                                                        });
            });
        });
    }

    private void removeUnPassed(List<HistoryItem> results, HistoryFragment activity) {
        Map<String, List<HistoryItem>> historyMap = new HashMap<>();
        for(HistoryItem result:results){
            if((result.date).compareTo(activity.filters[4]) < 0){
                continue;
            }
            if(!historyMap.containsKey(result.date)){
                historyMap.put(result.date, new ArrayList<>());
            }
            historyMap.get(result.date).add(result);
        }
        if(!activity.filters[5].equals("")){
            boolean setTriage = activity.filters[5].equals("Days with Triage");
            for (Map.Entry<String, List<HistoryItem>> entry : historyMap.entrySet()) {
                List<HistoryItem> cards = entry.getValue();
                boolean hasTriage = false;
                for(HistoryItem card: cards){
                    if(card.cardType == HistoryItem.typeOfCard.triage){
                        hasTriage = true;
                        break;
                    }
                }
                if(setTriage != hasTriage){
                    for(HistoryItem card: cards){
                        card.passFilter = false;
                    }
                }
            }
        }
        for (Map.Entry<String, List<HistoryItem>> entry : historyMap.entrySet()) {
            List<HistoryItem> cards = entry.getValue();
            boolean passFilter = false;
            for(HistoryItem card: cards){
                if(card.cardType != HistoryItem.typeOfCard.triage){
                    passFilter = true;
                    break;
                }
            }
            if(!passFilter){
                for(HistoryItem card: cards){
                    card.passFilter = false;
                }
            }
        }

        results.clear();
        for (List<HistoryItem> items : historyMap.values()) {
            results.addAll(items);
        }

        results.removeIf(result -> !result.passFilter);
    }

    private HistoryItem builtTriageItem(DocumentSnapshot doc) {
        Timestamp ts = doc.getTimestamp("date");
        Date accDate = ts != null ? ts.toDate() : null;
        List<String> flaglist = new ArrayList<>();
        if (doc.contains("flagList")) {
            flaglist = (List<String>) doc.get("flagList");
        }
        if(flaglist.isEmpty()){
            flaglist.add("None");
        }
        String emergencyCall = "No emergency call!";
        if (doc.contains("guidance")){
            List<String> guidanceList = (List<String>) doc.get("guidance");

            if (guidanceList != null && !guidanceList.isEmpty()) {
                emergencyCall = guidanceList.get(0);
            }
        }
        List<String> userRes = new ArrayList<>();
        if (doc.contains("userRes")){
            userRes = (List<String>) doc.get("userRes");
        }
        int pef = Math.toIntExact(doc.contains("pef") ? doc.getLong("pef"): -5);
        int rescueAttempts = Math.toIntExact(doc.contains("rescueAttempts") ? doc.getLong("rescueAttempts") : -5);
        HistoryItem test = new HistoryItem(accDate,flaglist,emergencyCall,userRes,pef,rescueAttempts);
        return test;
    }

    private HistoryItem buildDailyItem(DocumentSnapshot doc) {
        String date = doc.getId(); // the date document ID
        boolean nightWakingChild = doc.contains("nightWakingchild") && doc.getBoolean("nightWakingchild");
        boolean nightWakingParent = doc.contains("nightWakingparent") && doc.getBoolean("nightWakingparent");
        int activityLimitsChild = Math.toIntExact(doc.contains("activityLimitschild") ? doc.getLong("activityLimitschild") : -5);
        int activityLimitsParent = Math.toIntExact(doc.contains("activityLimitsparent") ? doc.getLong("activityLimitsparent") : -5);
        int coughingWheezingChild = Math.toIntExact(doc.contains("coughingWheezingchild") ? doc.getLong("coughingWheezingchild") : -5);
        int coughingWheezingParent = Math.toIntExact(doc.contains("coughingWheezingparent") ? doc.getLong("coughingWheezingparent") : -5);
        int pef = Math.toIntExact(doc.contains("pef") ? doc.getLong("pef"): -5);
        Timestamp ts = doc.getTimestamp("date");
        Date accDate = ts != null ? ts.toDate() : null;

        String zone = doc.contains("zoneColour") ? doc.getString("zoneColour"): "";
        List<String> triggersChild = new ArrayList<>();
        List<String> triggersParent = new ArrayList<>();
        if (doc.contains("triggerschild")) {
            triggersChild = (List<String>) doc.get("triggerschild");
        }
        if (doc.contains("triggersparent")) {
            triggersParent = (List<String>) doc.get("triggersparent");
        }
        HistoryItem test = new HistoryItem(date,nightWakingChild,nightWakingParent,activityLimitsChild,activityLimitsParent,coughingWheezingChild,coughingWheezingParent,triggersChild,triggersParent,pef,zone,accDate);
        return test;
    }

    public Query filterQuery(HistoryFragment activity, String childUid, String role){
        Query q = db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries");

        if(!activity.filters[0].equals("")) {
            q = q.whereEqualTo("nightWaking"+role,Boolean.parseBoolean(activity.filters[0]));
        }
        if(!activity.filters[1].equals("")){
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
        if(!activity.filters[2].equals("")){
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
        if(!activity.filters[3].equals("")){
            q = q.whereArrayContains("triggers"+role, activity.filters[3]);
        }
        return q;
    }

}
