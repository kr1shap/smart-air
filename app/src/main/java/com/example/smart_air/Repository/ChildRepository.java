package com.example.smart_air.Repository;

import android.util.Log;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.BadgeData;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.Invite;
import com.example.smart_air.modelClasses.User;
import com.example.smart_air.modelClasses.formatters.StringFormatters;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ChildRepository {

    private static final String TAG = "ChildRepository"; //for logcat
    private final FirebaseFirestore db;

    public ChildRepository() {
        this.db = FirebaseInitalizer.getDb();
    }


    //GENERAL CHILD REPO FUNCTIONS

    // update the existing child
    public void updateChild(Child child, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("children")
                .document(child.getChildUid())
                .set(child, SetOptions.merge())
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
    public void validateInviteCode(String code, String role, OnSuccessListener<Invite> onSuccess, OnFailureListener onFailure) {
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

                    //check if role is not matching
                    if(invite.getTargetRole() != null && !invite.getTargetRole().equals(role)) {
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

    // use invite code [PROVIDER] [MARK AS READ AND USED]
    public void useInviteCode(String code, String userUid, String role, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        validateInviteCode(code, role, invite -> {
            db.collection("users").document(userUid).get()
                    .addOnSuccessListener(userSnapshot -> {
                        if (userSnapshot.exists()) {
                            List<String> parentUids = (List<String>) userSnapshot.get("parentUid");
                            if (parentUids != null && parentUids.contains(invite.getParentUid())) {
                                onFailure.onFailure(new Exception("You are already connected to this person."));
                                return;
                            }
                        }

                        // if not connected use the invite code
                        db.collection("invites")
                                .document(code)
                                .update(
                                        "usedByUid", userUid,
                                        "used", true,
                                        "usedByEmail", FirebaseInitalizer.getAuth().getCurrentUser().getEmail()
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Invite marked as used");
                                    linkUserToParent(userUid, invite.getParentUid(), role, onSuccess, onFailure);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error marking invite as used", e);
                                    onFailure.onFailure(e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking user's connections", e);
                        onFailure.onFailure(e);
                    });
        }, onFailure);
    }

    // link user (child or provider) to parent
    private void linkUserToParent(String userUid, String parentUid, String role,
                                  OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        db.collection("users")
                .document(userUid)
                .update("parentUid", FieldValue.arrayUnion(parentUid))
                .addOnSuccessListener(aVoid -> {
                    if ("child".equalsIgnoreCase(role)) {
                        // if it's a child, add child to parent's childrenUid
                        updateParentChildrenList(
                                parentUid,
                                userUid,
                                onSuccess,
                                onFailure
                        );
                    } else {
                        // provider case
                        onSuccess.onSuccess(parentUid);
                    }
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    // update parents children list
    private void updateParentChildrenList(String parentUid, String childUid,
                                          OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        Log.d(TAG, "Updating parent's children list");
        db.collection("users")
                .document(parentUid)
                .update("childrenUid", FieldValue.arrayUnion(childUid))
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess(parentUid))
                .addOnFailureListener(onFailure::onFailure);
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
        AuthRepository repo = new AuthRepository();
        if(repo.getCurrentUser() == null) return;
        //find parent, and remove childUid from list
        DocumentReference parentRef = db.collection("users").document(repo.getCurrentUser().getUid());
        DocumentReference childRefUser = db.collection("users").document(childUid);
        DocumentReference childRef = db.collection("children").document(childUid);
        parentRef.update("childrenUid", FieldValue.arrayRemove(childUid))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Removed child from parent list");
                    //delete the parent from the child's user document (b/c cannot directly delete due to auth)
                    childRefUser.update("parentUid", null)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Removed parentUid list from child");
                                //delete child document
                                childRef.delete()
                                        .addOnSuccessListener(aVoid3 -> {
                                            Log.d(TAG, "Deleted child document");
                                            //go and delete the related documents
                                            deleteChildRelatedDocs(db, childUid,
                                                    () -> {
                                                        Log.d(TAG, "Deleted all related documents");
                                                        onSuccess.onSuccess(null);
                                                    }, onFailure);

                                        }).addOnFailureListener(onFailure);

                            }).addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    // Helper method to delete related documents in other collections
    private void deleteChildRelatedDocs(FirebaseFirestore db, String childUid,
                                        Runnable onSuccess, OnFailureListener onFailure) {
        //collection names
        String[] collections = {"actionPlan", "dailyCheckins", "incidentLog", "invites"};
        WriteBatch batch = db.batch();
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (String col : collections) {
            if (col.equals("invites")) {
                // get all invite docs where childUid is used
                Task<QuerySnapshot> task = db.collection(col)
                        .whereEqualTo("usedByUid", childUid)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot doc : querySnapshot) {
                                batch.delete(doc.getReference());
                            }
                        });
                tasks.add(task);
            } else {
                DocumentReference docRef = db.collection(col).document(childUid);
                batch.delete(docRef);
            }
        }

        //wait for all queries
        Tasks.whenAll(tasks)
                .addOnSuccessListener(aVoid -> {
                    batch.commit()
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "Deleted all related documents");
                                onSuccess.run();
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    // Unlink a provider (mark invite as inactive or delete)
    public void unlinkProvider(String inviteCode, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        //get the invite first
        db.collection("invites").document(inviteCode).get()
                .addOnSuccessListener(inviteSnap -> {
                    if (!inviteSnap.exists()) {
                        onFailure.onFailure(new Exception("Invite not found"));
                        return;
                    }
                    //get the uids (used and the parent one)
                    String usedByUid = inviteSnap.getString("usedByUid");
                    String parentUid = inviteSnap.getString("parentUid");
                    //if any null, error with the code
                    if (usedByUid == null || parentUid == null || usedByUid.trim().isEmpty() || parentUid.trim().isEmpty()) {
                        onFailure.onFailure(new Exception("Invite missing required UIDs"));
                        return;
                    }
                    //query all children for the parent uid, and now we remove the provider from the children list
                    db.collection("children")
                            .whereEqualTo("parentUid", parentUid)
                            .get()
                            .addOnSuccessListener(childrenSnapshot -> {
                                runUnlinkTransaction(inviteCode, usedByUid, parentUid, childrenSnapshot, onSuccess, onFailure);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching children", e);
                                onFailure.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching invite", e);
                    onFailure.onFailure(e);
                });
    }

    //private function to run the transaction
    private void runUnlinkTransaction(String inviteCode, String usedByUid, String parentUid,
                                      QuerySnapshot childrenSnapshot,
                                      OnSuccessListener<Void> onSuccess,
                                      OnFailureListener onFailure) {
        db.runTransaction(transaction -> {
            // remove the parentUid from the provider parentUid array
            DocumentReference providerRef = db.collection("users").document(usedByUid);
            DocumentSnapshot providerSnap = transaction.get(providerRef);
            List<String> parentList = (List<String>) providerSnap.get("parentUid");

            if (parentList == null || !parentList.contains(parentUid)) {
                throw new FirebaseFirestoreException("Provider does not contain that parent UID",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            transaction.update(providerRef, "parentUid", FieldValue.arrayRemove(parentUid));
            //remove provider from all providerUid lists in children
            for (DocumentSnapshot childDoc : childrenSnapshot.getDocuments()) {
                DocumentReference childRef = db.collection("children").document(childDoc.getId());
                transaction.update(childRef, "allowedProviderUids", FieldValue.arrayRemove(usedByUid));
                Log.d(TAG, "Removed provider " + usedByUid + " from child: " + childDoc.getId());
            }

            //delete invite document
            DocumentReference inviteRef = db.collection("invites").document(inviteCode);
            transaction.delete(inviteRef);
            return null;

        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Transaction completed: Provider unlinked, removed from all children, & invite deleted");
            onSuccess.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed", e);
            onFailure.onFailure(e);
        });
    }

    //Get all providers for parent (return USER object)
    public void getProvidersUserForParent(String parentUid,
                                          OnSuccessListener<List<User>> onSuccess,
                                          OnFailureListener onFailure) {
        db.collection("users")
                .whereEqualTo("role", "provider")
                .whereArrayContains("parentUid", parentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> providers = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User provider = doc.toObject(User.class);
                        if (provider != null) {
                            provider.setUid(doc.getId());
                            providers.add(provider);
                        }
                    }
                    onSuccess.onSuccess(providers);
                })
                .addOnFailureListener(onFailure);
    }

    //Get badge information from a child (return as task)
    public Task<BadgeData> getBadgeData(String childUid) {
        TaskCompletionSource<BadgeData> taskSource = new TaskCompletionSource<>();
        db.collection("children").document(childUid).get()
                .addOnSuccessListener(snap -> {
                    //default
                    boolean controllerBadge = false;
                    boolean techniqueBadge = false;
                    boolean rescueBadge = false;
                    int techniqueStreak = 0;
                    int controllerStreak = 0;
                    String techniqueDate = null;
                    String controllerDate = null;

                    if (snap.exists()) {
                        // badges map
                        Map<String, Object> badges = (Map<String, Object>) snap.get("badges");
                        if (badges != null) {
                            controllerBadge = badges.get("controllerBadge") != null ?
                                    (Boolean) badges.get("controllerBadge") : false;
                            techniqueBadge = badges.get("techniqueBadge") != null ?
                                    (Boolean) badges.get("techniqueBadge") : false;
                            rescueBadge = badges.get("lowRescueBadge") != null ?
                                    (Boolean) badges.get("lowRescueBadge") : false;
                        }

                        // techniqueStats map
                        Map<String, Object> techniqueStats = (Map<String, Object>) snap.get("techniqueStats");
                        if (techniqueStats != null) {
                            techniqueStreak = techniqueStats.get("currentStreak") != null ?
                                    ((Number) techniqueStats.get("currentStreak")).intValue() : 0;
                            techniqueDate = techniqueStats.get("lastSessionDate") != null ?
                                    (String) techniqueStats.get("lastSessionDate") : null;
                        }
                        //DO NOT add controller stats map due to timestamp - will be made if DNE

                    }
                    //UI change for technique streak - if streak invalid just change to 0 ui-based
                    //next time child logs in a new session an actual change will be made
                    if(techniqueDate != null && (!StringFormatters.getToday().equals(techniqueDate) || !StringFormatters.getYesterday().equals(techniqueDate))) techniqueStreak = 0;
                    BadgeData data = new BadgeData(controllerBadge, techniqueBadge, rescueBadge, techniqueStreak, controllerStreak);
                    taskSource.setResult(data);
                })
                .addOnFailureListener(taskSource::setException);

        return taskSource.getTask();
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