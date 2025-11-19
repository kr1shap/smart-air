package com.example.smart_air.Repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.Invite;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class InviteRepository {

    private static final String TAG = "InviteRepository";
    private static final String INVITES_COLLECTION = "invites";

    private final FirebaseFirestore db;

    public InviteRepository() {
        this.db = FirebaseInitalizer.getDb();
    }

    // Generate a provider invite code
    public void generateProviderInvite(String parentUid, OnInviteGeneratedListener listener) {
        String code = generateUniqueCode();

        // Set expiry to 7 days from now (in milliseconds)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        long expiresAt = calendar.getTimeInMillis();

        Invite invite = new Invite(code, parentUid, "provider", expiresAt);

        saveInvite(invite, listener);
    }

    // Generate a child invite code
    public void generateChildInvite(String parentUid, OnInviteGeneratedListener listener) {
        String code = generateUniqueCode();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        long expiresAt = calendar.getTimeInMillis();

        Invite invite = new Invite(code, parentUid, "child", expiresAt);

        saveInvite(invite, listener);
    }

    // Save invite to Firestore
    private void saveInvite(Invite invite, OnInviteGeneratedListener listener) {
        db.collection(INVITES_COLLECTION)
                .add(invite)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Invite created: " + invite.getCode());
                    listener.onSuccess(invite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating invite", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Get all active provider invites for a parent
    public LiveData<List<Invite>> getActiveProviderInvites(String parentUid) {
        MutableLiveData<List<Invite>> invitesLiveData = new MutableLiveData<>();

        long now = System.currentTimeMillis();

        db.collection(INVITES_COLLECTION)
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("targetRole", "provider")
                .whereEqualTo("used", false)
                .whereGreaterThanOrEqualTo("expiresAt", now)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to invites", error);
                        invitesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Invite> invites = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Invite invite = doc.toObject(Invite.class);
                            invites.add(invite);
                        }
                    }
                    invitesLiveData.setValue(invites);
                });

        return invitesLiveData;
    }

    // Get all active child invites for a parent
    public LiveData<List<Invite>> getActiveChildInvites(String parentUid) {
        MutableLiveData<List<Invite>> invitesLiveData = new MutableLiveData<>();

        long now = System.currentTimeMillis();

        db.collection(INVITES_COLLECTION)
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("targetRole", "child")
                .whereEqualTo("used", false)
                .whereGreaterThanOrEqualTo("expiresAt", now)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to child invites", error);
                        invitesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Invite> invites = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Invite invite = doc.toObject(Invite.class);
                            invites.add(invite);
                        }
                    }
                    invitesLiveData.setValue(invites);
                });

        return invitesLiveData;
    }

    // Revoke an invite by code
    public void revokeInviteByCode(String code, OnInviteOperationListener listener) {
        db.collection(INVITES_COLLECTION)
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        db.collection(INVITES_COLLECTION)
                                .document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Invite revoked");
                                    listener.onSuccess("Invite revoked");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error revoking invite", e);
                                    listener.onFailure(e.getMessage());
                                });
                    } else {
                        listener.onFailure("Invite not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding invite", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Regenerate a provider invite (revoke old, create new)
    public void regenerateProviderInvite(String parentUid, String oldCode, OnInviteGeneratedListener listener) {
        // First revoke the old invite
        revokeInviteByCode(oldCode, new OnInviteOperationListener() {
            @Override
            public void onSuccess(String message) {
                // Then generate a new one
                generateProviderInvite(parentUid, listener);
            }

            @Override
            public void onFailure(String error) {
                // If revoke fails, still try to generate new one
                generateProviderInvite(parentUid, listener);
            }
        });
    }

    // Generate a unique 8-character code
    private String generateUniqueCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }

        return code.toString();
    }

    // Check if a code is valid (for verification before use)
    public void validateInviteCode(String code, String targetRole, OnValidationListener listener) {
        long now = System.currentTimeMillis();

        db.collection(INVITES_COLLECTION)
                .whereEqualTo("code", code)
                .whereEqualTo("targetRole", targetRole)
                .whereEqualTo("used", false)
                .whereGreaterThanOrEqualTo("expiresAt", now)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Invite invite = querySnapshot.getDocuments().get(0).toObject(Invite.class);
                        listener.onValid(invite);
                    } else {
                        listener.onInvalid("Invalid or expired code");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error validating code", e);
                    listener.onInvalid("Error checking code");
                });
    }

    // Callback interfaces
    public interface OnInviteGeneratedListener {
        void onSuccess(Invite invite);
        void onFailure(String error);
    }

    public interface OnInviteOperationListener {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface OnValidationListener {
        void onValid(Invite invite);
        void onInvalid(String reason);
    }
}