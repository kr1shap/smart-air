package com.example.smart_air.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smart_air.modelClasses.InventoryData;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Cache only used for document creation (PDF) - to persist across fragments.
public class DashboardViewModel extends ViewModel {

    // uid - list of rescue log documents
    private final Map<String, List<DocumentSnapshot>> weeklyRescueCache = new HashMap<>();

    // uid - list of PEF documents
    private final Map<String, List<DocumentSnapshot>> pefCache = new HashMap<>();

    // child uid - sharing toggle map
    private final Map<String, Map<String, Boolean>> childSharingCache = new HashMap<>();

    //METHODS TO GET AND SET

    // weekly rescue cache
    public Map<String, List<DocumentSnapshot>> getWeeklyRescueCache() { return weeklyRescueCache; }

    public void putWeeklyRescue(String uid, List<DocumentSnapshot> docs) { weeklyRescueCache.put(uid, docs); }

    // pef cache
    public Map<String, List<DocumentSnapshot>> getPefCache() { return pefCache; }

    public void putPefCache(String uid, List<DocumentSnapshot> docs) {
        pefCache.put(uid, docs);
    }

    // sharing cache
    public Map<String, Map<String, Boolean>> getChildSharingCache() { return childSharingCache;}

    // remove page boolean
    private final MutableLiveData<Boolean> removePage = new MutableLiveData<>(false);
    public LiveData<Boolean> getRemovePage() {
        return removePage;
    }

    public void setRemovePage(boolean value) {
        removePage.setValue(value);  // update internally
    }

    public void putChildSharing(String childUid, Map<String, Boolean> sharing) {
        childSharingCache.put(childUid, sharing);
    }

    //clear all caches
    public void clearAllCaches() {
        weeklyRescueCache.clear();
        pefCache.clear();
        childSharingCache.clear();
    }
}
