package com.example.smart_air.Repository;

import android.util.Log;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.Invite;
import com.example.smart_air.modelClasses.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ChildRepository {

    private static final String TAG = "ChildRepository"; //for logcat
    private final FirebaseFirestore db;

    public ChildRepository() {
        this.db = FirebaseInitalizer.getDb();
    }


    //GENERAL CHILD REPO FUNCTIONS

    // add a new child
    public void addChild(Child child, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        String childId = db.collection("children").document().getId();
        child.setChildUid(childId);

        Log.d(TAG, "Adding child with ID: " + childId + ", Name: " + child.getName());

        db.collection("children")
                .document(childId)
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child added successfully: " + childId);
                    onSuccess.onSuccess(childId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding child", e);
                    onFailure.onFailure(e);
                });
    }

    // update the existing child
    public void updateChild(Child child, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("children")
                .document(child.getChildUid())
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Child updated successfully");
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating child", e);
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
                        if (child != null) { children.add(child); }
                    }
                    Log.d(TAG, "Retrieved " + children.size() + " children for parent: " + parentUid);
                    onSuccess.onSuccess(children);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting children", e);
                    onFailure.onFailure(e);
                });
    }

    // generate invite code (6-8 digit unique code)
    //checks first if the code is unique, and replaces the UNUSED code from parent, rather than generating 1000 of them!!
    public void generateInviteCode(String parentUid, String targetRole, OnSuccessListener<Invite> onSuccess, OnFailureListener onFailure) {
        String code = generateRandomCode();
        // expires 7 days from now
        long expiresAt = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);

        Invite invite = new Invite(code, parentUid, targetRole, expiresAt);

        db.collection("invites")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("targetRole", targetRole) //only query invites that are of specfic role
                .get()
                .addOnSuccessListener(query -> {
                    //get all unused invites (should only be one though, but for now assume list)
                    List<DocumentSnapshot> unused = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Boolean used = doc.getBoolean("used");
                        if (used == null || !used) { unused.add(doc); }
                    }
                    //if its not empty, we replace the code
                    if (!unused.isEmpty()) {
                        // Replace unused code
                        DocumentSnapshot unusedInvite = unused.get(0);
                        Log.d(TAG, "Found unused invite to replace: " + unusedInvite.getId());

                        //collision before regenerating code
                        checkCodeCollision(code,
                                () -> {
                                    unusedInvite.getReference().delete()
                                            .addOnSuccessListener(v -> add_replaceInvite(invite, onSuccess, onFailure))
                                            .addOnFailureListener(onFailure);
                                },
                                onFailure
                        );   //we are able to delete the invite, and replace it with a new one

                    } else {
                        // generate a new one if none are found!
                        Log.d(TAG, "No unused invites; creating new.");
                        checkCodeCollision(code, () -> add_replaceInvite(invite, onSuccess, onFailure), onFailure);
                    }

                })
                .addOnFailureListener(onFailure);
    }

    // validate invite code
    public void validateInviteCode(String code, OnSuccessListener<Invite> onSuccess, OnFailureListener onFailure) {
        Log.d(TAG, "Validating invite code: " + code);

        db.collection("invites")
                .document(code)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Document exists: " + documentSnapshot.exists());

                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "Invite code not found: " + code);
                        onFailure.onFailure(new Exception("Invalid code"));
                        return;
                    }

                    Invite invite = documentSnapshot.toObject(Invite.class);
                    if (invite == null) {
                        Log.e(TAG, "Failed to parse invite object");
                        onFailure.onFailure(new Exception("Invalid code"));
                        return;
                    }

                    long currentTime = System.currentTimeMillis();

                    // check if expired
                    if (currentTime > invite.getExpiresAt()) {
                        Log.e(TAG, "Code expired. Current: " + currentTime + ", Expires: " + invite.getExpiresAt());
                        onFailure.onFailure(new Exception("Code expired"));
                        return;
                    }

                    // check if already used
                    if (invite.isUsed()) {
                        Log.e(TAG, "Code already used");
                        onFailure.onFailure(new Exception("Code already used"));
                        return;
                    }

                    Log.d(TAG, "Invite code validated successfully: " + code);
                    onSuccess.onSuccess(invite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error validating invite code", e);
                    onFailure.onFailure(e);
                });
    }

    // use invite code (mark as used and link user)
    public void useInviteCode(String code, String userUid, String role, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        Log.d(TAG, "Using invite code: " + code + " for user: " + userUid + " with role: " + role);

        validateInviteCode(code, invite -> {
            // mark invite as used
            db.collection("invites")
                    .document(code)
                    .update("used", true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Invite marked as used");
                        // link user to parent based on role
                        linkUserToParent(userUid, invite.getParentUid(), role, onSuccess, onFailure);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error marking invite as used", e);
                        onFailure.onFailure(e);
                    });
        }, onFailure);
    }

    // link user (child or provider) to parent
    private void linkUserToParent(String userUid, String parentUid, String role,
                                  OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        Log.d(TAG, "Linking user " + userUid + " to parent " + parentUid + " as " + role);

        // update users parentUid list
        db.collection("users")
                .document(userUid) //find child or provider
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        Log.e(TAG, "User not found: " + userUid);
                        onFailure.onFailure(new Exception("User not found"));
                        return;
                    }

                    List<String> parentUids = user.getParentUid();
                    if (parentUids == null) { parentUids = new ArrayList<>(); }
                    if (!parentUids.contains(parentUid)) { parentUids.add(parentUid); }

                    db.collection("users")
                            .document(userUid)
                            .update("parentUid", parentUids)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User's parentUid list updated");
                                // update parents' childrenUid list if linking a child
                                if ("child".equalsIgnoreCase(role)) {
                                    updateParentChildrenList(parentUid, userUid, onSuccess, onFailure);
                                } else {
                                    Log.d(TAG, "User linked to parent successfully");
                                    onSuccess.onSuccess(parentUid);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating user's parentUid", e);
                                onFailure.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user", e);
                    onFailure.onFailure(e);
                });
    }

    // update parents children list
    private void updateParentChildrenList(String parentUid, String childUid,
                                          OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        Log.d(TAG, "Updating parent's children list");

        db.collection("users")
                .document(parentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User parent = documentSnapshot.toObject(User.class);
                    if (parent == null) {
                        Log.e(TAG, "Parent not found: " + parentUid);
                        onFailure.onFailure(new Exception("Parent not found"));
                        return;
                    }

                    List<String> childrenUids = parent.getChildrenUid();
                    if (childrenUids == null) { childrenUids = new ArrayList<>(); }
                    if (!childrenUids.contains(childUid)) { childrenUids.add(childUid); }

                    db.collection("users")
                            .document(parentUid)
                            .update("childrenUid", childrenUids)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Parent's children list updated successfully");
                                onSuccess.onSuccess(parentUid);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating parent's children list", e);
                                onFailure.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching parent", e);
                    onFailure.onFailure(e);
                });
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
                .whereEqualTo("used", false) //must be unused function
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Invite invite = doc.toObject(Invite.class);
                        if (invite != null) {
                            if (System.currentTimeMillis() < invite.getExpiresAt()) {
                                Log.d(TAG, "Found active invite: " + invite.getCode()); //return this invite
                                onSuccess.onSuccess(invite);
                                return;
                            }
                            //If the invite has expired, simply delete it as it is unused
                            else {
                                doc.getReference().delete();
                                Log.d(TAG, "Invite expired; deleted.");
                            }
                        }
                    }
                    Log.d(TAG, "No active invite found");
                    onSuccess.onSuccess(null); // no active invite found
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for active invite", e);
                    onFailure.onFailure(e);
                });
    }

    // Add these methods to your ChildRepository.java class:

    // Get all linked providers for a parent
    public void getLinkedProviders(String parentUid, OnSuccessListener<List<Invite>> onSuccess,
                                   OnFailureListener onFailure) {
        Log.d(TAG, "Fetching linked providers for parent: " + parentUid);

        db.collection("invites")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("targetRole", "provider")
                .whereEqualTo("used", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Invite> providers = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Invite invite = doc.toObject(Invite.class);
                        if (invite != null) {
                            providers.add(invite);
                            Log.d(TAG, "Found provider: " + invite.getUsedByEmail());
                        }
                    }
                    Log.d(TAG, "Retrieved " + providers.size() + " linked providers");
                    onSuccess.onSuccess(providers);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching linked providers", e);
                    onFailure.onFailure(e);
                });
    }


    //UNLINKING FUNCTIONS

    // delete child
    //TODO: MAKE IT SO IT DELETES ALL CHILDREN REFERENCES ACROSS THE DB!
    public void deleteChild(String childUid, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
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

    // Unlink a provider (mark invite as inactive or delete)
    public void unlinkProvider(String inviteCode, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Log.d(TAG, "Unlinking provider with invite code (transaction): " + inviteCode);
        db.runTransaction(transaction -> {

            //get document for invite
            DocumentReference inviteRef = db.collection("invites").document(inviteCode);
            DocumentSnapshot inviteSnap = transaction.get(inviteRef);

            if (!inviteSnap.exists()) {
                throw new FirebaseFirestoreException("Invite not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String usedByUid = inviteSnap.getString("usedByUid");
            String parentUid = inviteSnap.getString("parentUid");
            //if uid is null, dne (just an invalid invite generation)
            if (usedByUid == null || parentUid == null ||
                    usedByUid.trim().isEmpty() || parentUid.trim().isEmpty()) {
                throw new FirebaseFirestoreException("Invite missing required UIDs", FirebaseFirestoreException.Code.ABORTED);
            }

            // STEP 2 : Remove the parentUid from the provider UID
            DocumentReference providerRef = db.collection("users").document(usedByUid);
            //get a snapshot (transaction)
            DocumentSnapshot providerSnap = transaction.get(providerRef);
            //get the parent list
            List<String> parentList = (List<String>) providerSnap.get("parentUid");
            //check if provider list contains parent uid
            if (parentList == null || !parentList.contains(parentUid)) {
                throw new FirebaseFirestoreException("Provider does not contain that parent UID", FirebaseFirestoreException.Code.ABORTED);
            }
            //remove from array
            transaction.update(providerRef, "parentUid", FieldValue.arrayRemove(parentUid));
            //delete the invite doc
            transaction.delete(inviteRef);
            return null;

        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Transaction completed: Provider unlinked & invite deleted");
            onSuccess.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed", e);
            onFailure.onFailure(e);
        });
    }

    //PRIVATE HELPER FUNCTIONS

    // random 6-7 (& 8) digit code generator
    private String generateRandomCode() {
        Random random = new Random();
        int length = 6 + random.nextInt(3); // 6, 7, or 8 digits
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        String generatedCode = code.toString();
        Log.d(TAG, "Generated random code: " + generatedCode + " (length: " + length + ")");
        return generatedCode;
    }

    //Function checks if the code generated already exists (as unique number)
    private void checkCodeCollision(String code, Runnable onSafe, OnFailureListener onFailure) {
        db.collection("invites")
                .document(code)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        onFailure.onFailure(new Exception("Invite code collision: " + code));
                    } else {
                        onSafe.run();
                    }
                })
                .addOnFailureListener(onFailure);
    }


    //Private function: ADD OR REPLACE THE INVITE GIVEN!
    private void add_replaceInvite(Invite inv, OnSuccessListener<Invite> onSuccess, OnFailureListener onFailure) {
        db.collection("invites")
                .document(inv.getCode())
                .set(inv)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New invite created successfully: " + inv.getCode());
                    onSuccess.onSuccess(inv);
                })
                .addOnFailureListener(onFailure);
    }
}