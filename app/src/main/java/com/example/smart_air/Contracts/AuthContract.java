package com.example.smart_air.Contracts;

import com.example.smart_air.modelClasses.User;

public interface AuthContract {

    // sign up contract
    public interface SignUpContract {
        interface View {
            void showLoading();
            void hideLoading();
            void showError(String message);
            void navigateToOnboarding(User user); //check with given role
            String getSelectedRole();
            //show and hide fields
            void showAccessCodeField(String hint);
            void hideAccessCodeField();
            void showEmailField();
            void hideEmailField();
            void showUsernameField();
            void hideUsernameField();
        }

        interface Presenter {
            void signUp(String email, String password, String username,
                        String accessCode, String role);
            void onRoleSelected(String role);
            void onDestroy();
        }
    }

    // sign in contract
    public interface SignInContract {
        interface View {
            void showLoading();
            void hideLoading();
            void showError(String message);
            void navigateToHome(User user); //check based on selected role
            String getSelectedRole();
            void showEmailField();
            void hideEmailField();
            void showUsernameField();
            void hideUsernameField();
            void showForgotPassword();
            void hideForgotPassword();
            void showSuccess(String s);
        }

        interface Presenter {
            void signIn(String emailOrUsername, String password, String role); //sign in fxn
            void sendPasswordReset(String email);
            void onRoleSelected(String role);
            void onDestroy();
        }
    }

    interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }

    interface GeneralCallback {
        void onSuccess();
        void onFailure(String error);
    }
}