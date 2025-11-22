package com.example.smart_air.Repository;

import android.util.Log;

//import androidx.annotation.NonNull;

import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.Invite;
import com.example.smart_air.modelClasses.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ChildRepository {

    private static final String TAG = "ChildRepository";
    private final FirebaseFirestore db;

    public ChildRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // add a new child
    public void addChild(Child child, OnSuccessListener<String> onSuccess, OnFailureListener
            onFailure) {
        String childId = db.collection("children").document().getId();
        child.setChildUid(childId);

        db.collection("children")
                .document(childId)
                .set(child)
                .addOnSuccessListener(aVoid ->{
                    Log.d(TAG, "Child added successfully");
                    onSuccess.onSuccess(childId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding child", e);
                    onFailure.onFailure(e);
                });
    }

    // update the existing child
    public void updateChild(Child child, OnSuccessListener<Void> onSuccess, OnFailureListener
            onFailure) {
        db.collection("children")
                .document(child.getChildUid())
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child updated successfully");
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e ->{
                    Log.e(TAG, "Error updating child", e);
                    onFailure.onFailure(e);
                });
    }

    // delete child
    public void deleteChild(String childUid, OnSuccessListener<Void> onSuccess, OnFailureListener
            onFailure) {
        db.collection("children")
                .document(childUid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child deleted successfully");
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting child", e);
                    onFailure.onFailure(e);
                });
    }

    // get all children by parent UID
    public void getChildrenByParent(String parentUid, OnSuccessListener<List<Child>> onSuccess,
                                    OnFailureListener onFailure) {
        db.collection("children").whereEqualTo("parentUid", parentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Child> children = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Child child = doc.toObject(Child.class);
                        if (child != null) {
                            children.add(child);
                        }
                    }
                    Log.d(TAG, "Retrieved " + children.size() + " children");
                    onSuccess.onSuccess(children);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting children", e);
                    onFailure.onFailure(e);
                });
    }

    // generate invite code (6(7)-8 digit unique code)
    public void generateInviteCode(String parentUid, String targetRole, OnSuccessListener<Invite>
            onSuccess, OnFailureListener onFailure) {
        String code = generateRandomCode();
        // expires 7 days from now
        long expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000);

        Invite invite = new Invite(code, parentUid, targetRole, expiresAt);

        db.collection("invites")
                .document(code)
                .set(invite)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invite code generated: " + code);
                    onSuccess.onSuccess(invite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error generating invite code", e);
                    onFailure.onFailure(e);
                });
    }

    // validate invite code
    public void validateInviteCode(String code, OnSuccessListener<Invite> onSuccess,
                                   OnFailureListener onFailure) {
        db.collection("invites")
                .document(code)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        onFailure.onFailure(new Exception("Invalid code"));
                        return;
                    }

                    Invite invite = documentSnapshot.toObject(Invite.class);
                    if (invite == null) {
                        onFailure.onFailure(new Exception("Invalid code"));
                        return;
                    }

                    // check if expired cude
                    if (System.currentTimeMillis() > invite.getExpiresAt()) {
                        onFailure.onFailure(new Exception("Code expired"));
                        return;
                    }

                    // check if already used
                    if (invite.isUsed()) {
                        onFailure.onFailure(new Exception("Code already used"));
                        return;
                    }

                    Log.d(TAG, "Invite code validated: " + code);
                    onSuccess.onSuccess(invite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error validating invite code", e);
                    onFailure.onFailure(e);
                });
    }

    // use invite code (mark as used and link user)
    public void useInviteCode(String code, String userUid, String role, OnSuccessListener<String>
            onSuccess, OnFailureListener onFailure) {
        validateInviteCode(code, invite -> {
            // mark invite as used
            db.collection("invites")
                    .document(code)
                    .update("used", true)
                    .addOnSuccessListener(aVoid -> {
                        // link user to parent based on role
                        linkUserToParent(userUid, invite.getParentUid(), role, onSuccess, onFailure);
                    })
                    .addOnFailureListener(onFailure);
        }, onFailure);
    }

    // link user (child or provider) to parent
    private void linkUserToParent(String userUid, String parentUid, String role,
                                  OnSuccessListener<String> onSuccess, OnFailureListener
                                          onFailure) {
        // update users parentUid list
        db.collection("users")
                .document(userUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        onFailure.onFailure(new Exception("User not found"));
                        return;
                    }

                    List<String> parentUids = user.getParentUid();
                    if (parentUids == null) {
                        parentUids = new ArrayList<>();
                    }
                    if (!parentUids.contains(parentUid)) {
                        parentUids.add(parentUid);
                    }

                    db.collection("users")
                            .document(userUid)
                            .update("parentUid", parentUids)
                            .addOnSuccessListener(aVoid -> {
                                // update parents' childrenUid list if linking a child
                                if ("child".equalsIgnoreCase(role)) {
                                    updateParentChildrenList(parentUid, userUid, onSuccess,
                                            onFailure);
                                } else {
                                    Log.d(TAG, "User linked to parent successfully");
                                    onSuccess.onSuccess(parentUid);
                                }
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    // update parents children list
    private void updateParentChildrenList(String parentUid, String childUid,
                                          OnSuccessListener<String> onSuccess, OnFailureListener
                                                  onFailure) {
        db.collection("users")
                .document(parentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User parent = documentSnapshot.toObject(User.class);
                    if (parent == null) {
                        onFailure.onFailure(new Exception("Parent not found"));
                        return;
                    }

                    List<String> childrenUids = parent.getChildrenUid();
                    if (childrenUids == null) {
                        childrenUids = new ArrayList<>();
                    }
                    if (!childrenUids.contains(childUid)) {
                        childrenUids.add(childUid);
                    }

                    db.collection("users")
                            .document(parentUid)
                            .update("childrenUid", childrenUids)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Parent's children list updated");
                                onSuccess.onSuccess(parentUid);
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    // update the sharing toggles for a child
    public void updateSharingToggles(String childUid, Map<String, Boolean> sharing, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("children")
                .document(childUid)
                .update("sharing", sharing)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sharing toggles updated successfully");
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating sharing toggles", e);
                    onFailure.onFailure(e);
                });
    }

    // get existing invite for parent (to check if one already exists)
    public void getActiveInviteForParent(String parentUid, String targetRole, OnSuccessListener<Invite> onSuccess, OnFailureListener onFailure) {
        db.collection("invites")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("targetRole", targetRole)
                .whereEqualTo("used", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Invite invite = doc.toObject(Invite.class);
                        if (invite != null && System.currentTimeMillis() < invite.getExpiresAt()) {
                            onSuccess.onSuccess(invite);
                            return;
                        }
                    }
                    onSuccess.onSuccess(null); // no active invite found
                })
                .addOnFailureListener(onFailure);
    }

    // random 6-7-8 digit code generator
    private String generateRandomCode() {
        Random random = new Random();
        int length = 6 + random.nextInt(3); // 6, 7, or 8 digits
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}