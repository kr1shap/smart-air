package com.example.smart_air.Presenters;

import android.util.Patterns;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.User;

public class SignUpPresenter implements AuthContract.SignUpContract.Presenter  {
    private AuthContract.SignUpContract.View view; //is the UI handling
    private final AuthRepository repo;

    public SignUpPresenter(AuthContract.SignUpContract.View view) {
        this.view = view;
        this.repo = new AuthRepository();
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


    private boolean isValidEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void signUp(String email, String password, String username,
                       String accessCode, String role) {

        //Check child-specific validation
        if(role.equals("child") && (username == null || username.trim().isEmpty())) {
            view.showError("Username is required");
            return;
        }
        //Check provider and child specific validation
        if(!role.equals("parent") && (accessCode == null || accessCode.trim().isEmpty() || accessCode.length() < 6)) {
            view.showError("Access Code is required, or not in format XXXXXX");
            return;
        }
        //Parent and provider-specific validation
        if(!role.equals("child") && (email == null || email.trim().isEmpty()) ) {
            view.showError("Email is required");
            return;
        } else if (!isValidEmail(email)) {
            view.showError("Invalid email format");
            return;
        }

        //Password validation

        if (!validatePassword(password) || password.length() < 6) {
            view.showError("Password must be at least 6 characters, 1 digit, 1 uppercase, 1 lowercase, and 1 special character.");
            return;
        }

        view.showLoading();

        switch (role.toLowerCase()) {
            case "parent":
                repo.signUpParent(email, password, username,
                        createCallback());
                break;
            case "provider":
                repo.signUpProvider(email, password, accessCode,
                        createCallback());
                break;
            case "child":
                repo.signUpChild(username, accessCode, password,
                        createCallback());
                break;
            default: //should never occur
                view.hideLoading();
                view.showError("Invalid role selected");
        }
    }

    @Override
    public void onRoleSelected(String role) {
        switch (role.toLowerCase()) {
            case "parent":
                view.showEmailField();
                view.hideAccessCodeField();
                view.hideUsernameField();
                break;

            case "provider":
                view.showEmailField();
                view.hideUsernameField();
                view.showAccessCodeField("Enter access code from parent");
                break;

            case "child":
                view.hideEmailField();
                view.showUsernameField();
                view.showAccessCodeField("Enter access code from parent/provider");
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
                    view.navigateToOnboarding(user); //move on to next step
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

}
