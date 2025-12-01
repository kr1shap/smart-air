package com.example.smart_air.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class ChildTogglesViewModel extends ViewModel {

    private final MutableLiveData<Map<String, Boolean>> sharingToggles = new MutableLiveData<>(new HashMap<>());
    private ListenerRegistration childListener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<Map<String, Boolean>> getSharingToggles() {
        return sharingToggles;
    }

    /* listener for a specific child UID; will detach previous listener auto */
    public void attachChildListener(String childUid) {
        // get rid of previous listener
        if (childListener != null) childListener.remove();

        if (childUid == null || childUid.isEmpty()) {
            sharingToggles.setValue(new HashMap<>());
            return;
        }

        childListener = db.collection("children")
                .document(childUid)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    Map<String, Boolean> sharing = (Map<String, Boolean>) doc.get("sharing");
                    sharingToggles.setValue(sharing != null ? sharing : new HashMap<>());
                });
    }

    @Override
    public void onCleared() {
        super.onCleared();
        if (childListener != null) childListener.remove();
    }
}
