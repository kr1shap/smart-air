package com.example.smart_air.Repository;

import com.example.smart_air.fragments.HistoryFragment;
import com.example.smart_air.modelClasses.HistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.Timestamp;

public class HistoryRepository {
    private FirebaseFirestore db;
    private ListenerRegistration childListener;

    public HistoryRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // if child gets it's own uid
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



    // get's current cards
    public void getCards(String childUid, HistoryFragment activity){
        // null cehck
        if(childUid == null || childUid.isEmpty() || activity == null) {
            android.util.Log.e("HistoryRepository", "getCards called with null parameters");
            return;
        }

        // queries for both child and parent based on filters
        Query childQuery = filterQuery(activity, childUid,"child");
        Query parentQuery = filterQuery(activity, childUid,"parent");

        // array list with new cards
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

                                                            removeProviderInfo(results, activity);
                                                            removeUnPassed(results,activity);

                                                            markSharedItems(results, activity);
                                                            Collections.sort(results, (a, b) -> b.accDate.compareTo(a.accDate)); // sort results
                                                            
                                                            activity.createRecycleView(results); // create cards on screen
                                                        });
            });
        });
    }

    // goes through provider toggles and edits
    private void removeProviderInfo(List<HistoryItem> results, HistoryFragment activity) {
        // booleans based on toggle arrays
        boolean removeSymptoms = !activity.options[2];
        boolean removeTriageCard = !activity.options[3];
        boolean removeRescueOnly = !activity.options[1];
        boolean removePefTriage = !removeTriageCard && !activity.options[0];
        boolean removeTriggers = !activity.options[4];

        // update each card accordingly
        for(HistoryItem card: results){
            if(card.cardType == HistoryItem.typeOfCard.triage){
                if(removeTriageCard){ card.passFilter = false; }
                else {
                    if (removePefTriage) {
                        card.pef = -10; // implies n/a
                    }
                    if (removeRescueOnly){
                        card.rescueAttempts = -10; // implies n/a
                    }
                }
            }
            else{
                // set booleans to fix other stuff in adapter
                if(removeSymptoms){ card.removeSymptoms = true;}
                if(removeTriggers){ card.removeTrigger = true;}
            }
        }
    }


    // removes cards based on filter
    private void removeUnPassed(List<HistoryItem> results, HistoryFragment activity) {
        Map<String, List<HistoryItem>> historyMap = new HashMap<>(); // a map to sort cards by date
        for(HistoryItem result:results){
            if((result.date).compareTo(activity.filters[4]) < 0){ // checks to see if card is within date range
                continue;
            }
            if(!historyMap.containsKey(result.date)){ // adds to map
                historyMap.put(result.date, new ArrayList<>());
            }
            historyMap.get(result.date).add(result);
        }
        // keeps cards based on triage filter
        if(!activity.filters[5].equals("") && activity.options[3]){
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
        if(activity.options[3]) {
            for (Map.Entry<String, List<HistoryItem>> entry : historyMap.entrySet()) {
                List<HistoryItem> cards = entry.getValue();
                boolean passFilter = false;
                for (HistoryItem card : cards) {
                    if (card.cardType != HistoryItem.typeOfCard.triage) {
                        passFilter = true;
                        break;
                    }
                }
                if (!passFilter) {
                    for (HistoryItem card : cards) {
                        card.passFilter = false;
                    }
                }
            }
        }

        // re adds cards back into results
        results.clear();
        for (List<HistoryItem> items : historyMap.values()) {
            results.addAll(items);
        }

        // remove cards that didn't pass
        results.removeIf(result -> !result.passFilter);
    }

    // marks items as shared w/ provider & what specific data is shared
    private void markSharedItems(List<HistoryItem> results, HistoryFragment activity) {
        // safety check
        if (results == null || activity == null || activity.options == null) {
            return;
        }

        // only mark items as shared if user is parent (providers see everything they have access to)
        // check if any sharing is enabled (at least one option is true)
        boolean hasSharing = false;
        for(boolean option : activity.options) {
            if (option) {
                hasSharing = true;
                break;
            }
        }

        if(!hasSharing){
            return; // nothing is shared, no need to mark anything
        }

        for (HistoryItem card : results) {
            List<String> sharedData = new ArrayList<>();

            if (card.cardType == HistoryItem.typeOfCard.triage) {
                // triage cards
                if (activity.options[3]) { // triage toggle
                    card.sharedWithProvider = true;
                    sharedData.add("Triage Data");

                    if (activity.options[0] && card.pef != -10) { // PEF
                        sharedData.add("PEF");
                    }
                    if (activity.options[1] && card.rescueAttempts != -10) { // Rescue
                        sharedData.add("Rescue Attempts");
                    }
                }
            } else {
                // daily cards
                boolean hasSharedData = false;

                if (activity.options[2]) { // symptoms
                    hasSharedData = true;
                    sharedData.add("Symptoms");
                }
                if (activity.options[4]) { // triggers
                    hasSharedData = true;
                    sharedData.add("Triggers");
                }
                if (activity.options[0] && card.pef > 0) { // PEF (if present)
                    hasSharedData = true;
                    sharedData.add("PEF");
                }

                if (hasSharedData) {
                    card.sharedWithProvider = true;
                }
            }

            card.sharedItems = sharedData;
        }
    }

    // given triage info builds HistoryItem triage card
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

    // given daily info builds a HistoryItem for a daily info card
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

    // queries based on filters
    public Query filterQuery(HistoryFragment activity, String childUid, String role){
        Query q = db.collection("dailyCheckins") // goes to collection
                .document(childUid)
                .collection("entries");

        // query based on nightWaking
        if(!activity.filters[0].equals("") && activity.options[2] == true) {
            q = q.whereEqualTo("nightWaking"+role,Boolean.parseBoolean(activity.filters[0]));
        }
        // query based on activity
        if(!activity.filters[1].equals("") && activity.options[2] == true){
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
        // query based on coughing
        if(!activity.filters[2].equals("") && activity.options[2] == true){
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
        // query based on triggers
        if(!activity.filters[3].equals("") && activity.options[4] == true){
            q = q.whereArrayContains("triggers"+role, activity.filters[3]);
        }
        return q;
    }

    // listener to update toggles
    public void updateToggles(String childUid, HistoryFragment activity){
        if (childListener != null) {
            childListener.remove(); // remove previous ones
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        String uid = user.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        return;
                    }

                    String role = document.getString("role");

                    if ("child".equals(role) || "parent".equals(role)) { // if not provider call fixToggles to return all true
                        activity.fixToggles(null, true);
                        return;
                    }

                    DocumentReference childrenDoc = db.collection("children").document(childUid); // check children to find specific toggles for them
                    childListener = childrenDoc.addSnapshotListener((snapshot, e) -> { // listener to check for change
                        if (e != null || snapshot == null || !snapshot.exists()) return;

                        Map<String, Boolean> sharing =
                                (Map<String, Boolean>) snapshot.get("sharing");

                        if (sharing != null) {
                            activity.fixToggles(sharing, false); // update list
                        }
                    });
                });

    }



}
