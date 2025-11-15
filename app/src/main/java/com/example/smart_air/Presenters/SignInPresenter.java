package com.example.smart_air.Presenters;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.User;

public class SignInPresenter implements AuthContract.SignInContract.Presenter {
    private AuthContract.SignInContract.View view;
    private AuthRepository repo;

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
            repo.signInChild(emailOrUsername, password, createCallback()); //username -> email handled in method
        } else {
            repo.signIn(emailOrUsername, password, createCallback());
        }

    }

    //TODO: probably remove this
    @Override
    public void onRoleSelected(String role) {
        switch (role) {
            case "parentProv":
                view.showEmailField();
                view.hideUsernameField();
                break;

            case "child":
                view.hideEmailField();
                view.showUsernameField();
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
