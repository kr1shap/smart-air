package com.example.smart_air.Repository;

import android.util.Log;
import android.util.Patterns;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.smart_air.Contracts.AuthContract.AuthCallback;

public class AuthRepository {
    private static final String TAG = "AuthRepository"; // just a label for LogCat
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    //Constructor
    public AuthRepository() {
        this.auth = FirebaseInitalizer.getAuth();
        this.db = FirebaseInitalizer.getDb();
    }

    //SIGNUP - PARENT
    public void signUpParent(String email, String password,
                             AuthContract.AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        if(firebaseUser == null){
                            Log.e(TAG, "User creation succeeded but getCurrentUser() " +
                                    "returned null");
                            callback.onFailure("Account created but unable to retrieve " +
                                    "user info. Please try logging in.");
                            return;
                        }

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
                               String accessCode, AuthContract.AuthCallback callback) {

        checkAccessCode(accessCode, "provider", parentUid -> {

            if (parentUid != null) {
                //if valid access code (non null)
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser firebaseUser = auth.getCurrentUser();

                                if (firebaseUser == null) {
                                    Log.e(TAG, "Provider account created but getCurrentUser() returned null");
                                    callback.onFailure("Account created but unable to retrieve user info. Please try logging in.");
                                    return;
                                }

                                String providerUid = firebaseUser.getUid();
                                String providerEmail = email.toLowerCase();

                                // create user to save
                                User user = new User(
                                        providerUid,
                                        providerEmail,
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
                                        // mark invite w/ provider details
                                        markInviteAsUsedWithDetails(accessCode, providerUid,
                                                providerEmail);
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

                                FirebaseUser firebaseUser = auth.getCurrentUser();

                                if(firebaseUser == null){
                                    Log.e(TAG, "User creation succeeded but getCurrentUser() " +
                                            "returned null");
                                    callback.onFailure("Account created but unable to retrieve user info. Please try logging in.");
                                    return;
                                }

                                String childUid = firebaseUser.getUid();

                                //user to save to firestore
                                User user = new User(
                                        childUid,
                                        dummyEmail,
                                        username,
                                        "child",
                                        List.of(parentUid), // link to parent
                                        List.of() // last field of children one null
                                );

                                // Call save function (helper)
                                saveUserToFirestore(user, new AuthContract.AuthCallback() {
                                    @Override
                                    public void onSuccess(User savedUser) {
                                        // Mark invite as used
                                        addChildToParent(parentUid, childUid,
                                                new AuthContract.GeneralCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        addChildCollection(parentUid, childUid, username,
                                                                new AuthContract.GeneralCallback() {
                                                                    @Override
                                                                    public void onSuccess() {
                                                                        // invite as used
                                                                        markInviteAsUsedWithDetails(accessCode,
                                                                                childUid, username);
                                                                        callback.onSuccess(savedUser);
                                                                    }

                                                                    @Override
                                                                    public void onFailure(String error) {
                                                                        // give error
                                                                        callback.onFailure(error);
                                                                    }
                                                                });
                                                    }
                                                    @Override
                                                    public void onFailure(String error) {
                                                        // even if adding to parent fails, child account was created
                                                        // still, attempt to add to children collection
                                                        addChildCollection(parentUid, savedUser.getUid(), username,
                                                                new AuthContract.GeneralCallback() {
                                                                    @Override
                                                                    public void onSuccess() {
                                                                        markInviteAsUsedWithDetails(accessCode, childUid, username);
                                                                        callback.onSuccess(savedUser);
                                                                    }

                                                                    @Override
                                                                    public void onFailure(String childError) {
                                                                        markInviteAsUsedWithDetails(accessCode, childUid, username);
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
    private void addChildCollection(String parentUid, String childUid, String childName,
                                    AuthContract.GeneralCallback callback) {
        // Create the object
        Child child = new Child(
                childName,          // name
                childUid,           // childUid
                parentUid,          // parentUid
                new Date(),         // make it current date, parent will modify
                null,               // extraNotes
                0                  // personalBest
        );

        //get the document
        DocumentReference childRef = db.collection("children").document(childUid);

        childRef.set(child)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Child added!");
                    //make inventory subcollection
                    Map<String, Object> emptyInventory = new HashMap<>();
                    emptyInventory.put("amount", 0);
                    emptyInventory.put("name", "N/A");
                    emptyInventory.put("expiryDate", null);
                    emptyInventory.put("purchaseDate", null);

                    // under inventory/rescue
                    childRef.collection("inventory")
                            .document("rescue")
                            .set(emptyInventory);

                    // under inventory/controller
                    childRef.collection("inventory")
                            .document("controller")
                            .set(emptyInventory);

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
        long now = System.currentTimeMillis();
        Log.d(TAG, "Checking access code: " + code + " for role: " + targetRole);

        db.collection("invites")
                .whereEqualTo("code", code)
                .whereEqualTo("targetRole", targetRole)
                .whereEqualTo("used", false)
                .whereGreaterThanOrEqualTo("expiresAt", now)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String parentUid = querySnapshot.getDocuments().get(0).getString("parentUid");
                        Log.d(TAG, "Access code valid! Parent UID: " + parentUid);
                        callback.onResult(parentUid);
                    } else {
                        Log.d(TAG, "No valid invite found");
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking access code", e);
                    callback.onResult(null);
                });
    }

    //Mark invite as used AFTER sign up (not after validity) - WITH user details
    private void markInviteAsUsedWithDetails(String code, String usedByUid, String usedByIdentifier) {
        Log.d(TAG, "Marking invite " + code + " as used by: " + usedByIdentifier);

        db.collection("invites")
                .document(code)
                .update(
                        "used", true,
                        "usedByUid", usedByUid,
                        "usedByEmail", usedByIdentifier
                )
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Invite marked as used successfully"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error marking invite as used", e));
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

    public void deleteCurrentUser(AuthContract.GeneralCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // delete firestore user document if it exists
            DocumentReference userRef = db.collection("users").document(user.getUid());
            userRef.delete()
                    .addOnSuccessListener(aVoid1 -> Log.d("Auth", "User document deleted successfully"))
                    .addOnFailureListener(e -> Log.w("Auth", "User document may not exist: " + e));
            // delete Auth account
            user.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Auth", "Current user deleted successfully");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Auth", "Failed to delete current user", e);
                        callback.onFailure(String.valueOf(e));
                    });
        } else {
            Log.w("Auth", "No user currently signed in");
            callback.onFailure("No user currently signed in");
        }
    }


    //HELPER FUNCTIONS

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