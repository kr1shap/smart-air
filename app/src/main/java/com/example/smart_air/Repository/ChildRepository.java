package com.example.smart_air.Repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.Child;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChildRepository {

    private static final String TAG = "ChildRepository";
    private static final String CHILDREN_COLLECTION = "children";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;
    private ListenerRegistration childrenListener;

    public ChildRepository() {
        this.db = FirebaseInitalizer.getDb();
    }

    // Add a new child
    public void addChild(Child child, String parentUid, OnChildOperationListener listener) {
        // Generate new child UID if not provided
        String childUid = child.getChildUid();
        if (childUid == null || childUid.isEmpty()) {
            childUid = db.collection(CHILDREN_COLLECTION).document().getId();
            child.setChildUid(childUid);
        }
        child.setParentUid(parentUid);

        final String finalChildUid = childUid;

        // Add child to children collection
        db.collection(CHILDREN_COLLECTION)
                .document(finalChildUid)
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    // Add child UID to parent's childrenUid array
                    db.collection(USERS_COLLECTION)
                            .document(parentUid)
                            .update("childrenUid", FieldValue.arrayUnion(finalChildUid))
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "Child added successfully");
                                listener.onSuccess("Child added successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error linking child to parent", e);
                                listener.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding child", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Get all children for a parent (real-time updates)
    public LiveData<List<Child>> getChildrenForParent(String parentUid) {
        MutableLiveData<List<Child>> childrenLiveData = new MutableLiveData<>();

        // First get the parent's childrenUid array
        db.collection(USERS_COLLECTION)
                .document(parentUid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to parent", error);
                        childrenLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        @SuppressWarnings("unchecked")
                        List<String> childrenUids = (List<String>) documentSnapshot.get("childrenUid");
                        if (childrenUids == null || childrenUids.isEmpty()) {
                            childrenLiveData.setValue(new ArrayList<>());
                            return;
                        }

                        // Fetch all children data
                        fetchChildrenData(childrenUids, childrenLiveData);
                    } else {
                        childrenLiveData.setValue(new ArrayList<>());
                    }
                });

        return childrenLiveData;
    }

    // Fetch multiple children data
    private void fetchChildrenData(List<String> childUids, MutableLiveData<List<Child>> childrenLiveData) {
        List<Child> children = new ArrayList<>();
        final int[] fetchCount = {0};

        for (String childUid : childUids) {
            db.collection(CHILDREN_COLLECTION)
                    .document(childUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Child child = documentSnapshot.toObject(Child.class);
                            if (child != null) {
                                children.add(child);
                            }
                        }
                        fetchCount[0]++;
                        if (fetchCount[0] == childUids.size()) {
                            childrenLiveData.setValue(children);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching child data", e);
                        fetchCount[0]++;
                        if (fetchCount[0] == childUids.size()) {
                            childrenLiveData.setValue(children);
                        }
                    });
        }
    }

    // Get a specific child (real-time updates)
    public LiveData<Child> getChild(String childUid) {
        MutableLiveData<Child> childLiveData = new MutableLiveData<>();

        db.collection(CHILDREN_COLLECTION)
                .document(childUid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to child", error);
                        childLiveData.setValue(null);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Child child = documentSnapshot.toObject(Child.class);
                        childLiveData.setValue(child);
                    } else {
                        childLiveData.setValue(null);
                    }
                });

        return childLiveData;
    }

    // Update child information
    public void updateChild(Child child, OnChildOperationListener listener) {
        db.collection(CHILDREN_COLLECTION)
                .document(child.getChildUid())
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child updated successfully");
                    listener.onSuccess("Child updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating child", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Update specific field (like name, dob, notes, personalBest)
    public void updateChildField(String childUid, String fieldName, Object value, OnChildOperationListener listener) {
        db.collection(CHILDREN_COLLECTION)
                .document(childUid)
                .update(fieldName, value)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child field updated: " + fieldName);
                    listener.onSuccess("Updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating child field", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Update sharing settings for a child
    public void updateSharingSettings(String childUid, Map<String, Boolean> sharingSettings, OnChildOperationListener listener) {
        db.collection(CHILDREN_COLLECTION)
                .document(childUid)
                .update("sharing", sharingSettings)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sharing settings updated successfully");
                    listener.onSuccess("Sharing settings updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating sharing settings", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Update a single sharing toggle (real-time, per requirement R2)
    public void updateSingleSharingToggle(String childUid, String sharingKey, boolean enabled, OnChildOperationListener listener) {
        db.collection(CHILDREN_COLLECTION)
                .document(childUid)
                .update("sharing." + sharingKey, enabled)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sharing toggle updated: " + sharingKey + " = " + enabled);
                    listener.onSuccess("Sharing updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating sharing toggle", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Delete a child
    public void deleteChild(String childUid, String parentUid, OnChildOperationListener listener) {
        // Remove from children collection
        db.collection(CHILDREN_COLLECTION)
                .document(childUid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from parent's childrenUid array
                    db.collection(USERS_COLLECTION)
                            .document(parentUid)
                            .update("childrenUid", FieldValue.arrayRemove(childUid))
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "Child deleted successfully");
                                listener.onSuccess("Child removed");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error removing child from parent list", e);
                                listener.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting child", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Clean up listeners
    public void removeListeners() {
        if (childrenListener != null) {
            childrenListener.remove();
        }
    }

    // Callback interface
    public interface OnChildOperationListener {
        void onSuccess(String message);
        void onFailure(String error);
    }
}