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
import com.example.smart_air.Presenters.SignUpPresenter;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.User;

import java.util.Arrays;
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
        // Given
        String email = "test@test.com";
        String password = "password123";
        String role = "parent";

        // When
        presenter.signIn(email, password, role);

        // Then
        verify(mockView).showLoading();
        verify(mockRepository).signIn(eq(email), eq(password), any(AuthContract.AuthCallback.class));
    }

    //TEST 2: NO EMAIL PROVIDED
    @Test
    public void signIn_parentProvWithNoEmail_shouldShowError() {
        // Given
        String password = "password123";
        // When
        presenter.signIn(null, password, "parentProv");

        // Then
        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    //TEST 2: EMAIL EMPTY
    @Test
    public void signIn_parentProvWithEmptyEmail_shouldShowError() {
        // Given
        String password = "password123";

        // When
        presenter.signIn("    ", password, "parentProv");

        // Then
        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }


    /*
     * CHILD SIGN IN TESTS
     */

    //TEST 1: Valid child sign-in
    @Test
    public void signIn_asChild_shouldCallSignInChild() {
        // Given
        String password = "password123";
        // When
        presenter.signIn("childUser", password, "child");

        // Then
        verify(mockView).showLoading();
        verify(mockRepository).signInChild(eq("childUser"), eq(password), any(AuthContract.AuthCallback.class));
    }

    //TEST 2: NO EMAIL PROVIDED
    @Test
    public void signIn_childWithNoUser_shouldShowError() {
        // Given
        String password = "password123";
        // When
        presenter.signIn(null, password, "child");

        // Then
        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    //TEST 3: EMAIL EMPTY
    @Test
    public void signIn_childWithEmptyUser_shouldShowError() {
        // Given
        String password = "password123";

        // When
        presenter.signIn("    ", password, "child");

        // Then
        verify(mockView).showError("Email/Username is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    /*
    * GENERAL SIGN IN TESTS
    */
    //TEST 1: PASSWORD NOT GIVEN
    @Test
    public void signIn_WithNoPassword_shouldShowError() {
        // When
        presenter.signIn("test@test.com", null, "parentProv");

        // Then
        verify(mockView).showError("Password is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    //TEST 2: PASSWORD EMPTY
    @Test
    public void signIn_WithEmptyPassword_shouldShowError() {
        // When
        presenter.signIn("test@test.com", "   ", "parentProv");

        // Then
        verify(mockView).showError("Password is required");
        verify(mockRepository, never()).signIn(anyString(), anyString(), any());
    }

    /*
    * ON SUCCESS SIGN IN TESTS
    */

    @Test
    public void signIn_success_shouldNavigateToHome() {
        // Given
        String email = "test@test.com";
        String password = "password123";
        User mockUser = new User("uid123", email, null, "parent", null, List.of());
        // When
        presenter.signIn(email, password, "parentProv");
        // Capture the callback
        verify(mockRepository).signIn(eq(email), eq(password), callbackCaptor.capture());
        // success
        callbackCaptor.getValue().onSuccess(mockUser);
        // Then
        verify(mockView).hideLoading();
        verify(mockView).navigateToHome(mockUser);
    }

    @Test
    public void signIn_failure_shouldShowError() {
        // Given
        String email = "test@test.com";
        String password = "wrongpassword";
        String role = "parent";
        String errorMessage = "Incorrect password. Please try again.";

        // When
        presenter.signIn(email, password, role);

        //callback
        verify(mockRepository).signIn(eq(email), eq(password), callbackCaptor.capture());

        //failure
        callbackCaptor.getValue().onFailure(errorMessage);

        // Then
        verify(mockView).hideLoading();
        verify(mockView).showError(errorMessage);
    }

    /*
    * PASSWORD RESET EMAILS
    */

    //TEST 1: Send with valid email, call to repo
    @Test
    public void sendPasswordReset_withValidEmail_shouldCallRepository() {
        // Given
        String email = "test@test.com";
        // When
        presenter.sendPasswordReset(email);
        // Then
        verify(mockView).showLoading();
        verify(mockRepository).sendPasswordResetEmail(eq(email), any(AuthContract.GeneralCallback.class));
    }

    //TEST 2: Send reset email, but null email
    @Test
    public void sendPasswordReset_withEmptyEmail_shouldShowError() {
        // When
        presenter.sendPasswordReset(null);

        // Then
        verify(mockView).showError("Please enter a valid email address");
        verify(mockRepository, never()).sendPasswordResetEmail(anyString(), any());
    }

    //TEST 3: Put in invalid email for reset
    @Test
    public void sendPasswordReset_withInvalidEmail_shouldShowError() {
        // When
        presenter.sendPasswordReset("oasjao@");

        // Then
        verify(mockView).showError("Please enter a valid email address");
        verify(mockRepository, never()).sendPasswordResetEmail(anyString(), any());
    }

    //TEST 4: Send reset email, success message
    @Test
    public void sendPasswordReset_success_shouldShowSuccessMessage() {
        // When
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
        // Given
        String errorMessage = "User not found";

        // When
        presenter.sendPasswordReset("test@test.com");

        // callback
        verify(mockRepository).sendPasswordResetEmail(eq("test@test.com"), simpleCallbackCaptor.capture());

        // Simulate failure
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
        // When
        presenter.onRoleSelected("child");

        // Then
        verify(mockView).hideEmailField();
        verify(mockView).showUsernameField();
        verify(mockView).hideForgotPassword();
    }

    @Test
    public void onRoleSelected_parentProv_shouldShowEmailForgotPassField() {
        // When
        presenter.onRoleSelected("parentProv");

        // Then
        verify(mockView).showEmailField();
        verify(mockView).hideUsernameField();
        verify(mockView).showForgotPassword();
    }

    //ON DESTROY TEST
    @Test
    public void onDestroy_shouldPreventLateCallbacksFromCrashing() {
        // Given
        String email = "parent@test.com";
        String password = "Password123!";
        User mockUser = new User("uid", email, null, "parent", null, List.of());
        //call signup before destroy
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
        // When
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
