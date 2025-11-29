package com.example.smart_air;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.Presenters.SignInPresenter;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.User;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SignInPresenterTest {

    @Mock
    private AuthContract.SignInContract.View mockView;

    @Mock
    private AuthRepository mockRepository;

    @Captor
    private ArgumentCaptor<AuthContract.AuthCallback> callbackCaptor; //captures any arguments passed through
    @Captor
    private ArgumentCaptor<AuthContract.GeneralCallback> simpleCallbackCaptor;

    private SignInPresenter presenter;

    @Before
    public void setUp() {
        //use mock repository and view
        presenter = new SignInPresenter(mockView, mockRepository);
    }

    //TEST TO GET COVERAGE FOR FIRST CONSTRUCTOR (IGNORE)
    @Test
    public void testDefaultConstructor() {
        AuthContract.SignInContract.View mockView = mock(AuthContract.SignInContract.View.class);
        SignInPresenter presenter = new SignInPresenter(mockView);
        assertNotNull(presenter);  // nothing else needed
    }

    /*
    * PARENT OR PROVIDER SIGN IN TESTS
    */
    @Test
    public void signIn_parentProvWithValidEmailAndPassword_shouldCallRepository() {
        String email = "test@test.com";
        String password = "password123";
        //when sign in is called
        presenter.signIn(email, password, "parent");

        verify(mockView).showLoading();
        verify(mockRepository).signIn(eq(email), eq(password), any(AuthContract.AuthCallback.class));
    }

    //TEST 2: NO EMAIL PROVIDED
    @Test
    public void signIn_parentProvWithNoEmail_shouldShowError() {
        String password = "Password123";
        //when sign in is called
        presenter.signIn(null, password, "parentProv");

        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    //TEST 2: EMAIL EMPTY
    @Test
    public void signIn_parentProvWithEmptyEmail_shouldShowError() {
        String password = "Password123";

        //when sign in is called
        presenter.signIn("    ", password, "parentProv");

        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }


    /*
     * CHILD SIGN IN TESTS
     */

    //TEST 1: Valid child sign-in
    @Test
    public void signIn_asChild_shouldCallSignInChild() {
        String password = "Password123";

        //when sign in is called
        presenter.signIn("childUser", password, "child");

        verify(mockView).showLoading();
        verify(mockRepository).signInChild(eq("childUser"), eq(password), any(AuthContract.AuthCallback.class));
    }

    //TEST 2: NO EMAIL PROVIDED
    @Test
    public void signIn_childWithNoUser_shouldShowError() {
        String password = "Password123";

        //when sign in is called
        presenter.signIn(null, password, "child");

        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    //TEST 3: EMAIL EMPTY
    @Test
    public void signIn_childWithEmptyUser_shouldShowError() {
        String password = "Password123";

        //when sign in is called
        presenter.signIn("    ", password, "child");

        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    /*
    * GENERAL SIGN IN TESTS
    */
    //TEST 1: PASSWORD NOT GIVEN
    @Test
    public void signIn_WithNoPassword_shouldShowError() {
        //when sign in is called
        presenter.signIn("test@test.com", null, "parentProv");

        verify(mockView).showError("Password is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    //TEST 2: PASSWORD EMPTY
    @Test
    public void signIn_WithEmptyPassword_shouldShowError() {
        //when sign in is called
        presenter.signIn("test@test.com", "   ", "parentProv");

        verify(mockView).showError("Password is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    /*
    * ON SUCCESS SIGN IN TESTS
    */

    @Test
    public void signIn_success_shouldNavigateToHome() {
        String email = "test@test.com";
        String password = "password123";
        User mockUser = new User("uid123", email, null, "parent", null, List.of());

        //when sign in is called
        presenter.signIn(email, password, "parentProv");
        //verify the callback
        verify(mockRepository).signIn(eq(email), eq(password), callbackCaptor.capture());
        //mock success
        callbackCaptor.getValue().onSuccess(mockUser);

        verify(mockView).hideLoading();
        verify(mockView).navigateToHome(mockUser);
    }

    @Test
    public void signIn_failure_shouldShowError() {
        String email = "test@test.com";
        String password = "wrongpassword";
        String errorMessage = "Incorrect password. Please try again.";

        //when sign in is called
        presenter.signIn(email, password, "parent");

        verify(mockRepository).signIn(eq(email), eq(password), callbackCaptor.capture());

        //failure mock
        callbackCaptor.getValue().onFailure(errorMessage);

        verify(mockView).hideLoading();
        verify(mockView).showError(errorMessage);
    }

    /*
    * PASSWORD RESET EMAILS
    */

    //TEST 1: Send with valid email, call to repo
    @Test
    public void sendPasswordReset_withValidEmail_shouldCallRepository() {
        String email = "test@test.com";
        //when password reset
        presenter.sendPasswordReset(email);

        verify(mockView).showLoading();
        verify(mockRepository).sendPasswordResetEmail(eq(email), any(AuthContract.GeneralCallback.class));
    }

    //TEST 2: Send reset email, but null email
    @Test
    public void sendPasswordReset_withEmptyEmail_shouldShowError() {
        //when password reset
        presenter.sendPasswordReset(null);

        verify(mockView).showError("Please enter a valid email address");
        verify(mockRepository, never()).sendPasswordResetEmail(anyString(), any());
    }

    //TEST 3: Put in invalid email for reset
    @Test
    public void sendPasswordReset_withInvalidEmail_shouldShowError() {
        //when password reset
        presenter.sendPasswordReset("oasjao@");

        verify(mockView).showError("Please enter a valid email address");
        verify(mockRepository, never()).sendPasswordResetEmail(anyString(), any());
    }

    //TEST 4: Send reset email, success message
    @Test
    public void sendPasswordReset_success_shouldShowSuccessMessage() {
        //when password reset
        presenter.sendPasswordReset("test@test.com");

        // get callback
        verify(mockRepository).sendPasswordResetEmail(eq("test@test.com"), simpleCallbackCaptor.capture());

        // simulate success
        simpleCallbackCaptor.getValue().onSuccess();

        // Then
        verify(mockView).hideLoading();
        verify(mockView).showSuccess("Password reset email sent! Check your inbox.");
    }

    //TEST 5: Failure to send email (i.e. doesnt exist)
    @Test
    public void sendPasswordReset_failure_shouldShowError() {
        String errorMessage = "User not found";

        //when password reset
        presenter.sendPasswordReset("test@test.com");

        // callback
        verify(mockRepository).sendPasswordResetEmail(eq("test@test.com"), simpleCallbackCaptor.capture());

        // simulate failure
        simpleCallbackCaptor.getValue().onFailure(errorMessage);

        // then
        verify(mockView).hideLoading();
        verify(mockView).showError("Failed to send reset email: " + errorMessage);
    }


    /*
    * ROLE SELECTION TESTS
    */
    @Test
    public void onRoleSelected_child_shouldShowUsernameAccessHideForgotField() {
        presenter.onRoleSelected("child");

        verify(mockView).hideEmailField();
        verify(mockView).showUsernameField();
        verify(mockView).hideForgotPassword();
    }

    @Test
    public void onRoleSelected_parentProv_shouldShowEmailForgotPassField() {
        presenter.onRoleSelected("parentProv");

        verify(mockView).showEmailField();
        verify(mockView).hideUsernameField();
        verify(mockView).showForgotPassword();
    }

    //ON DESTROY TEST
    @Test
    public void onDestroy_shouldPreventLateCallbacksFromCrashing() {
        String email = "parent@test.com";
        String password = "Password123!";
        User mockUser = new User("uid", email, null, "parent", null, List.of());
        //when signup before destroy
        presenter.signIn(email, password, "parentProv");
        // verify repo called
        verify(mockRepository).signIn(
                eq(email),
                eq(password),
                callbackCaptor.capture()
        );

        // destroy view
        presenter.onDestroy();
        //simulate callback after view destroyed
        callbackCaptor.getValue().onSuccess(mockUser);
        // make sure no view method called after (b/c null)
        verify(mockView, never()).getSelectedRole();
        verify(mockView, never()).hideLoading();
        verify(mockView, never()).navigateToHome(any(User.class));

    }

    //ON DESTROY TEST
    @Test
    public void onDestroy_shouldPreventLateCallbacksFromCrashingEmailReset() {
        //when password reset
        presenter.sendPasswordReset("test@test.com");

        // get callback
        verify(mockRepository).sendPasswordResetEmail(eq("test@test.com"), simpleCallbackCaptor.capture());

        // simulate success
        simpleCallbackCaptor.getValue().onSuccess();

        // destroy view
        presenter.onDestroy();
        // simulate success
        simpleCallbackCaptor.getValue().onSuccess();
        // make sure no view method called after (b/c null)
        verify(mockView, never()).getSelectedRole();
        verify(mockView, never()).navigateToHome(any(User.class));

    }
}
