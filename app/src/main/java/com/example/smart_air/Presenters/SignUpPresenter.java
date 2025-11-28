package com.example.smart_air.Presenters;

import androidx.annotation.VisibleForTesting;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.modelClasses.User;

import com.example.smart_air.Repository.AuthRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;


public class SignUpPresenter implements AuthContract.SignUpContract.Presenter  {
    private AuthContract.SignUpContract.View view; //is the UI handling
    public AuthRepository repo;

    public SignUpPresenter(AuthContract.SignUpContract.View view) {
        this.view = view;
        this.repo = new AuthRepository();
    }

    //constructor with mock repo, solely for testing purposes only
    @VisibleForTesting
    public SignUpPresenter(AuthContract.SignUpContract.View view, AuthRepository repo) {
        this.view = view;
        this.repo = repo;
    }

    //Helper function
    private boolean validatePassword(String password) {
        if (password == null || password.isEmpty()) return false;
        // strong password regex
        String regex = "^(?=.*[0-9])" +  // at least 1 digit
                "(?=.*[a-z])" +         // at least 1 lowercase
                "(?=.*[A-Z])" +         // at least 1 uppercase
                "(?=.*[@#$%^&+=!])" +   // at least 1 special char
                "(?=\\S+$).{8,}$";      // no whitespace, min 8 chars
        return password.matches(regex);
    }

    @Override
    public void signUp(String email, String password, String username,
                       String accessCode, String role) {
        if (view == null) {return;}
        //Check if the role is valid
        if(!role.equals("child") && !role.equals("parent") && !role.equals("provider")) {
            view.hideLoading();
            view.showError("Invalid role selected");
            return;
        }
        //Check child-specific validation
        if(role.equals("child") && (username == null || username.trim().isEmpty() || username.trim().length() < 3)) {
            view.showError("Username is required and of minimum length 3");
            return;
        }
        //Check provider and child specific validation
        if(!role.equals("parent") && (accessCode == null || accessCode.trim().isEmpty() || accessCode.length() < 6)) {
            view.showError("Access Code is required, or not in format.");
            return;
        }
        //Parent and provider-specific validation
        if(!role.equals("child") && (email == null || email.trim().isEmpty()) ) {
            view.showError("Email is required");
            return;
        } else if (!role.equals("child") && !validEmail(email)) {
            view.showError("Invalid email format");
            return;
        }

        //Password validation

        if (!validatePassword(password) || password.length() < 6) {
            view.showError("Password must be at least 6 characters, 1 digit, 1 uppercase, 1 lowercase, and 1 special character.");
            return;
        }

        view.showLoading();
        String emailTrim = email != null ? email.trim() : null;
        switch (role.toLowerCase()) {
            case "parent":
                repo.signUpParent(emailTrim, password,
                        createCallback());
                break;
            case "provider":
                repo.signUpProvider(emailTrim, password, accessCode,
                        createCallback());
                break;
            case "child":
                repo.signUpChild(username.trim(), accessCode, password,
                        createCallback());
                break;
        }
    }

    @Override
    public void onRoleSelected(String role) {
        if (view == null) {return;}
        switch (role.toLowerCase()) {
            case "parent":
                view.showEmailField();
                view.hideAccessCodeField();
                view.hideUsernameField();
                break;

            case "provider":
                view.showEmailField();
                view.hideUsernameField();
                view.showAccessCodeField("Enter access code the caregiver.");
                break;

            case "child":
                view.hideEmailField();
                view.showUsernameField();
                view.showAccessCodeField("Enter access code from your parent.");
                break;
        }
    }

    @Override
    public void onDestroy() {
        view = null;
    }

    //Callback
    private AuthContract.AuthCallback createCallback() {
        return new AuthContract.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                if (view != null) {
                    view.hideLoading();

                    // 🔽 Firestore logic
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String uid = user.getUid();

                    Map<String, Object> controller = new HashMap<>();
                    controller.put("name", "Ventolin");
                    controller.put("dailyUsage", 0);
                    controller.put("usageLeft", 200);
                    controller.put("reminders", "Once per day");

                    Map<String, Object> rescue = new HashMap<>();
                    rescue.put("name", "Flovent");
                    rescue.put("dailyUsage", 0);
                    rescue.put("usageLeft", 200);
                    rescue.put("reminders", "No reminders");

                    db.collection("users")
                            .document(uid)
                            .collection("medications")
                            .document("controller")
                            .set(controller);

                    db.collection("users")
                            .document(uid)
                            .collection("medications")
                            .document("rescue")
                            .set(rescue);

                    // 🔽 Continue app flow
                    view.navigateToOnboarding(user);
                }
            }


            @Override
            public void onFailure(String error) {
                if (view != null) {
                    view.hideLoading();
                    view.showError(error); //error in view
                }
            }
        };
    }

    //Helper function to check if valid email
    public static boolean validEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.trim().matches(emailRegex);
    }


}
