package com.example.smart_air.Repository;

import android.media.MediaRouter;
import android.util.Log;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.Invite;
import com.example.smart_air.modelClasses.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.smart_air.Contracts.AuthContract.AuthCallback;

public class AuthRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    //Constructor
    public AuthRepository() {
        this.auth = FirebaseInitalizer.getAuth();
        this.db = FirebaseInitalizer.getDb();
    }

    //SIGNUP - PARENT
    public void signUpParent(String email, String password, String username,
                             AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        //TODO: Ensure we dont need line below (assert)
                        assert firebaseUser != null; //assert that not null, b/c task successful
                        User user = new User(
                                firebaseUser.getUid(), //UID created - save to firestore
                                email, //email
                                null, //no username for parent
                                "parent",
                                null ,//no parent uid needed
                                List.of() //empty list
                        );

                        saveUserToFirestore(user, callback);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // SIGNUP - Provider
    public void signUpProvider(String email, String password,
                               String accessCode, AuthCallback callback) {
        checkAccessCode(accessCode, "provider", parentUid -> {
            if (parentUid != null) {
                //if valid access code (non null)
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                // create user to save
                                User user = new User(
                                        auth.getCurrentUser().getUid(),
                                        email,
                                        null,
                                        "provider",
                                        List.of(parentUid),
                                        List.of() //null for them
                                );

                                saveUserToFirestore(user, new AuthCallback() {
                                    //Define interface, on success do such, on failure do such (callback)
                                    @Override
                                    public void onSuccess(User savedUser) {
                                        //invite as used, and on success
                                        markInviteAsUsed(accessCode);
                                        callback.onSuccess(savedUser); //use callback passed in
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        callback.onFailure(error); //used callback passed in
                                    }
                                });
                            } else { //error occured
                                callback.onFailure(task.getException().getMessage());
                            }
                        });
            } else {
                callback.onFailure("Invalid or expired access code");
            }
        });
    }


    //SIGNUP - child
    public void signUpChild(String username, String accessCode, String password,
                            AuthCallback callback) {
        checkAccessCode(accessCode, "child", parentUid -> {
            if (parentUid != null) {
                //dummy email for child
                String dummyEmail = username + "_child@smartAir.com";

                auth.createUserWithEmailAndPassword(dummyEmail, password)
                        .addOnCompleteListener(authTask -> {
                            if (authTask.isSuccessful()) {
                                //user to save to firestore
                                //TODO: fix yellow underline for null ptr exception (should NEVER occur though)
                                User user = new User(
                                        auth.getCurrentUser().getUid(),
                                        dummyEmail,
                                        username,
                                        "child",
                                        List.of(parentUid), //link to parent
                                        List.of() //last field of children one null
                                );

                                //Call save function (helper)
                                saveUserToFirestore(user, new AuthCallback() {
                                    @Override
                                    public void onSuccess(User savedUser) {
                                        // Mark invite as used
                                        addChildToParent(parentUid, auth.getCurrentUser().getUid(), new AuthContract.GeneralCallback() {
                                            @Override
                                            public void onSuccess() {
                                                // Mark invite as used
                                                markInviteAsUsed(accessCode);
                                                callback.onSuccess(savedUser);
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                // Even if adding to parent fails, child account was created
                                                // Still mark invite as used and continue
                                                markInviteAsUsed(accessCode);
                                                callback.onSuccess(savedUser);
                                            }
                                        });
                                    }
                                    @Override
                                    public void onFailure(String error) {
                                        callback.onFailure(error); //use callback passed in
                                    }
                                });
                            } else {
                                callback.onFailure(authTask.getException().getMessage()); //internal error
                            }
                        });
            } else {
                callback.onFailure("Invalid or expired access code"); //error with code
            }
        });
    }

    // Helper method to add child UID to parent document
    private void addChildToParent(String parentUid, String childUid, AuthContract.GeneralCallback callback) {
        db.collection("users")
                .document(parentUid)
                .update("childrenUid", FieldValue.arrayUnion(childUid))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    // Sign in (normal)
    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        getUserFromFirestore(firebaseUser.getUid(), callback); //callback passed in
                    } else {
                        callback.onFailure(task.getException().getMessage()); //callback error
                    }
                });
    }

    //Signin - CHILD
    public void signInChild(String username, String password, AuthCallback callback) {
        String dummyEmail = username.toLowerCase() + "_child@smartAir.com";
        signIn(dummyEmail, password, callback);
    }


    //Helper: check access code validity
    private void checkAccessCode(String code, String targetRole, AccessCodeCallback callback) {
        Timestamp now = Timestamp.now();
        Log.d("auth repo access code", "target role: " + targetRole);
        db.collection("invites")
                .whereEqualTo("code", code)
                .whereEqualTo("targetRole", targetRole)
                .whereEqualTo("used", false)
                .whereGreaterThanOrEqualTo("expiresAt", now)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    callback.onResult(querySnapshot.isEmpty() ? null : querySnapshot.getDocuments().get(0).getString("parentUid"));
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    //Mark invite as used AFTER sign up (not after validity)
    private void markInviteAsUsed(String code) {
        db.collection("invites")
            .whereEqualTo("code", code)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    db.collection("invites")
                            .document(doc.getId())
                            .update("used", true);
                }
            });
    }

    //ADD USER TO 'USERS'
    private void saveUserToFirestore(User user, AuthCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("username", user.getUsername());
        userData.put("role", user.getRole());
        userData.put("parentUid", user.getParentUid());
        userData.put("childrenUid", user.getChildrenUid());
        userData.put("createdAt", user.getCreatedAt());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    //GET A 'USER'
    private void getUserFromFirestore(String uid, AuthCallback callback) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) { //check if doc exists in firestore
                        User user = doc.toObject(User.class);
                        callback.onSuccess(user); //pass in user
                    } else {
                        callback.onFailure("User data not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    //SIGN-IN AND SIGN OUT (check if user is still signed in or if they want to signout)
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        auth.signOut();
    }


    //password reset email
    public void sendPasswordResetEmail(String email, AuthContract.GeneralCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    //callback for code validation
    private interface AccessCodeCallback {
        void onResult(String parentUid); // if invalid or exp -> null
    }


}
