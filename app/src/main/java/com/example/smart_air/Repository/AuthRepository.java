package com.example.smart_air.Repository;

import android.util.Log;
import android.util.Patterns;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
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
                                email.toLowerCase(), //email (lowercase as auth makes all lowercase)
                                null, //no username for parent
                                "parent",
                                null ,//no parent uid needed
                                List.of() //empty list
                        );

                        saveUserToFirestore(user, callback);
                    } else {
                        callback.onFailure(mapFirebaseAuthError(task.getException()));
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
                                        email.toLowerCase(),
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
                                callback.onFailure(mapFirebaseAuthError(task.getException()));
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
                String dummyEmail = username.toLowerCase().trim() + "_child@smartair.com";

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
                                                addChildCollection(parentUid, savedUser.getUid(), new AuthContract.GeneralCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        // invite as used
                                                        markInviteAsUsed(accessCode);
                                                        callback.onSuccess(savedUser);
                                                    }

                                                    @Override
                                                    public void onFailure(String error) {
                                                        // child user created but child collection failed
                                                        // still, mark invite as used and continue
                                                        markInviteAsUsed(accessCode);
                                                        callback.onSuccess(savedUser);
                                                    }
                                                });

                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                // even if adding to parent fails, child account was created
                                                // still, attempt to add to children collection
                                                addChildCollection(parentUid, savedUser.getUid(), new AuthContract.GeneralCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        markInviteAsUsed(accessCode);
                                                        callback.onSuccess(savedUser);
                                                    }

                                                    @Override
                                                    public void onFailure(String childError) {
                                                        markInviteAsUsed(accessCode);
                                                        callback.onSuccess(savedUser);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                    @Override
                                    public void onFailure(String error) {
                                        callback.onFailure(error); //use callback passed in
                                    }
                                });
                            } else {
                                callback.onFailure(mapFirebaseAuthError(authTask.getException())); //internal error
                            }
                        });
            } else {
                callback.onFailure("Invalid or expired access code"); //error with code
            }
        });
    }

    //Helper method to add a new child object to children db
    private void addChildCollection(String parentUid, String childUid, AuthContract.GeneralCallback callback) {
        Map<String, Boolean> sharing = new HashMap<>();
        sharing.put("rescue", true);
        sharing.put("controller_as", true);
        sharing.put("symptoms", true);
        sharing.put("triggers",true);
        sharing.put("pef",true);
        sharing.put("triage",true);
        sharing.put("charts",true);

        // Create the object
        Child child = new Child(
                childUid,           // childUid
                parentUid,          // parentUid
                new Date(),        // make it current date, parent will modify
                null,              // extraNotes
                0,                // personalBest
                sharing           // sharing
        );

        db.collection("children")
                .document(child.getChildUid())   // doc id
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Child added using model!");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to add child", e);
                    callback.onFailure(e.getMessage());
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
                        callback.onFailure(mapFirebaseAuthError(task.getException())); //callback error
                    }
                });
    }

    //Signin - CHILD
    public void signInChild(String username, String password, AuthCallback callback) {
        String dummyEmail = username.toLowerCase().trim() + "_child@smartair.com";
        signIn(dummyEmail, password, callback);
    }


    //Helper: check access code validity
    private void checkAccessCode(String code, String targetRole, AccessCodeCallback callback) {
        Timestamp now = Timestamp.now(); //TODO: modify so u check access code using long form, not timestamp
        Log.d("auth repo access code", "target role: " + targetRole);
        db.collection("invites")
                .whereEqualTo("code", code)
                .whereEqualTo("targetRole", targetRole)
                .whereEqualTo("used", false)
                .whereGreaterThanOrEqualTo("expiresAt", System.currentTimeMillis())
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

    //HERLPER FUNCTIONS

    //Helper function to check if valid email
    public boolean validEmail(String email) {
        String emailTrim = email.trim();
        return !emailTrim.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches();
    }

    //Helper function to tailor firebase auth msg for better ux
    private String mapFirebaseAuthError(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException) { return "Incorrect password. Please try again."; }
        if (e instanceof FirebaseAuthInvalidUserException) { return "No account found with this email."; }
        if (e instanceof FirebaseAuthRecentLoginRequiredException) { return "Please log in again to continue.";}
        if (e instanceof FirebaseAuthEmailException) { return "There was a problem with your email. Please check it and try again."; }
        if (e instanceof FirebaseAuthUserCollisionException) { return "An account with this email already exists."; }

        if (e instanceof FirebaseAuthException) {
            FirebaseAuthException authEx = (FirebaseAuthException) e;
            String code = authEx.getErrorCode();
            switch (code) {
                case "ERROR_USER_DISABLED":
                    return "Your account has been disabled. Please contact support.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Too many attempts. Please wait a moment and try again.";
                case "ERROR_INVALID_EMAIL":
                    return "Invalid email format.";
                default:
                    return "Login failed. Please try again.";
            }
        }
        //other
        return "Something went wrong. Please try again.";
    }


    public Task<DocumentSnapshot> getUserDoc(String uid) {
        return FirebaseInitalizer.getDb().collection("users").document(uid).get();
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
