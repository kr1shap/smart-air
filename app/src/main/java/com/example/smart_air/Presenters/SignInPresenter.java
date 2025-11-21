package com.example.smart_air.Presenters;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.User;

public class SignInPresenter implements AuthContract.SignInContract.Presenter {
    private AuthContract.SignInContract.View view;
    private final AuthRepository repo;

    public SignInPresenter(AuthContract.SignInContract.View view) {
        this.view = view;
        this.repo = new AuthRepository();
    }

    @Override
    public void signIn(String emailOrUsername, String password, String role) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            view.showError("Email/Username is required");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            view.showError("Password is required");
            return;
        }

        view.showLoading();

        if ("child".equalsIgnoreCase(role)) {
            repo.signInChild(emailOrUsername.trim(), password, createCallback()); //username -> email handled in method
        } else {
            repo.signIn(emailOrUsername.trim(), password, createCallback());
        }

    }

    @Override
    public void sendPasswordReset(String email) {
        if (email == null || email.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            view.showError("Please enter a valid email address");
            return;
        }

        view.showLoading();

        repo.sendPasswordResetEmail(email.trim(), new AuthContract.GeneralCallback() {
            @Override
            public void onSuccess() {
                if (view != null) {
                    view.hideLoading();
                    view.showSuccess("Password reset email sent! Check your inbox.");
                }
            }

            @Override
            public void onFailure(String error) {
                if (view != null) {
                    view.hideLoading();
                    view.showError("Failed to send reset email: " + error);
                }
            }
        });
    }

    @Override
    public void onRoleSelected(String role) {
        switch (role) {
            case "parentProv":
                view.showEmailField();
                view.hideUsernameField();
                view.showForgotPassword();
                break;

            case "child":
                view.hideEmailField();
                view.showUsernameField();
                view.hideForgotPassword();
                break;
        }
    }

    @Override
    public void onDestroy() {
        view = null;
    }

    private AuthContract.AuthCallback createCallback() {
        return new AuthContract.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                view.hideLoading();
                view.navigateToHome(user); //nav to home (check based on role)
            }

            @Override
            public void onFailure(String error) {
                view.hideLoading();
                view.showError(error);
            }
        };
    }

}
