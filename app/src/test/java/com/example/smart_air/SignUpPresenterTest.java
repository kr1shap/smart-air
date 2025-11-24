package com.example.smart_air;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.Presenters.SignUpPresenter;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.User;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SignUpPresenterTest {
    //Setup all mock objects
    @Mock
    private AuthContract.SignUpContract.View mockView;

    @Mock
    private AuthRepository mockRepository;

    @Captor
    private ArgumentCaptor<AuthContract.AuthCallback> callbackCaptor; //captures any arguments passed through

    private SignUpPresenter presenter;

    @Before
    public void setUp() {
        //use mock repository and view
        presenter = new SignUpPresenter(mockView, mockRepository);
    }

    /*
        PARENT SIGN UP TESTS
    */

    //TEST 1: Sign up parent, with valid data
    @Test
    public void signUp_parent_withValidData_shouldCallRepository() {
        // made-up credentials
        String email = "parent@test.com";
        String password = "Password123!";
        // test presenter
        presenter.signUp(email, password, null, null, "parent");
        //verify
        verify(mockView).showLoading();
        verify(mockRepository).signUpParent(
                eq(email),
                eq(password),
                any(AuthContract.AuthCallback.class)
        );
    }

    //TEST 2: Sign up parent, no email
    @Test
    public void signUp_parent_withEmptyEmail_shouldShowError() {
        // Given
        String email = "";
        String password = "password123!";
        // When
        presenter.signUp(email, password, null, null,"parent");
        // Then
        verify(mockView).showError("Email is required");
        verify(mockRepository, never()).signUpParent(anyString(), anyString(), any()); //never called
    }

    //TEST 3: Sign up parent, invalid email
    @Test
    public void signUp_parent_withInvalidEmail_shouldShowError() {
        // Given
        String email = "ddududu@s";
        String password = "password123!";
        // When
        presenter.signUp(email, password, null, null,"parent");
        // Then
        verify(mockView).showError("Invalid email format");
        verify(mockRepository, never()).signUpParent(anyString(), anyString(), any()); //never called
    }

    //TEST 3: Sign up (GENERAL), weak password
    @Test
    public void signUp_parent_withWeakPassword_shouldShowError() {
        // Given
        String email = "parent@test.com";
        String password = "cscb07weak";
        // When
        presenter.signUp(email, password, null, null,"parent");
        // Then
        verify(mockView).showError("Password must be at least 6 characters, 1 digit, 1 uppercase, 1 lowercase, and 1 special character.");
        verify(mockRepository, never()).signUpParent(anyString(), anyString(), any()); //never called
    }

    //TEST 4: Sign up (parent), navigate to onboarding
    @Test
    public void signUp_parent_success_shouldNavigateToHome() {
        // Given
        String email = "parent@test.com";
        String password = "Password123!";

        User mockUser = new User("uid123", email, null, "parent", null, List.of());

        // When
        presenter.signUp(email, password, null, null, "parent");
        // get the callback
        verify(mockRepository).signUpParent(eq(email), eq(password), callbackCaptor.capture());
        // simulate success
        callbackCaptor.getValue().onSuccess(mockUser);
        // Then
        verify(mockView).hideLoading();
        verify(mockView).navigateToOnboarding(mockUser);
    }

    //TEST 5: Sign up (parent), with already existing email
    @Test
    public void signUp_parent_failure_shouldShowError() {
        // Given
        String email = "parent@test.com";
        String password = "Password123!";
        String errorMessage = "An account with this email already exists.";
        // When
        presenter.signUp(email, password, null,null, "parent");
        // Capture the callback
        verify(mockRepository).signUpParent(eq(email), eq(password), callbackCaptor.capture());
        // make a fake failure
        callbackCaptor.getValue().onFailure(errorMessage);
        // Then
        verify(mockView).hideLoading();
        verify(mockView).showError(errorMessage);
    }


    /*
        PROVIDER SIGN UP TESTS
    */


    //TEST 1: Sign up the provider, calling repo check
    @Test
    public void signUp_provider_withValidData_shouldCallRepository() {
        // Given
        String email = "provider@test.com";
        String password = "Password123!";
        String accessCode = "ABC12345";

        // When
        presenter.signUp(email, password, null, accessCode, "provider");

        // Then
        verify(mockView).showLoading();
        verify(mockRepository).signUpProvider(
                eq(email),
                eq(password),
                eq(accessCode),
                any(AuthContract.AuthCallback.class)
        );
    }

    //TEST 2: Provider sign up without access code
    @Test
    public void signUp_provider_withoutAccessCode_shouldShowError() {
        // Given
        String email = "provider@test.com";
        String password = "password123!";
        String username = "providerUser";
        String accessCode = "";
        // When
        presenter.signUp(email, password, username, accessCode, "provider");
        // Then
        verify(mockView).showError("Access Code is required, or not in format.");
        verify(mockRepository, never()).signUpProvider(anyString(), anyString(), anyString(), any());
    }

    //TEST 3: Provider sign up with an invalid access code
    @Test
    public void signUp_provider_withInvalidAccessCode_shouldShowError() {
        // Given
        String email = "provider@test.com";
        String password = "password123!";
        String username = "providerUser";
        String accessCode = "123"; //length less than 6
        // When
        presenter.signUp(email, password, username, accessCode, "provider");
        // Then
        verify(mockView).showError("Access Code is required, or not in format.");
        verify(mockRepository, never()).signUpProvider(anyString(), anyString(), anyString(), any());
    }

    //TEST 4: Provider sign up with access code that is already in use, or doesnt exist
    @Test
    public void signUpProvider_withInvalidAccessCode_shouldShowError() {
        // Given
        String email = "provider@test.com";
        String password = "Password123!";
        String accessCode = "INVALID";

        // When
        presenter.signUp(email, password, null, accessCode, "provider");
        //verify showloading
        verify(mockView).showLoading();
        // capture callback
        verify(mockRepository).signUpProvider(
                eq(email),
                eq(password),
                eq(accessCode),
                callbackCaptor.capture()
        );

        // simulate invalid call
        callbackCaptor.getValue().onFailure("Invalid or expired access code");

        // Then
        verify(mockView).hideLoading();
        verify(mockView).showError("Invalid or expired access code");
    }

    //TEST 5: Provider sign up, valid and onto onboarding
    @Test
    public void signUpProvider_successfulSignup_shouldNavigateToOnboarding() {
        // Given
        String email = "provider@test.com";
        String password = "Password123!";
        String accessCode = "123456";

        User mockProviderUser = new User(
                "provider_uid_123",
                "provider@test.com",
                null,
                "provider",
                List.of("parent_uid_456"),  //link to parent
                List.of()  //no children
        );

        // When
        presenter.signUp(email, password, null, accessCode, "provider");

        // Capture the callback
        verify(mockRepository).signUpProvider(
                eq(email),
                eq(password),
                eq(accessCode),
                callbackCaptor.capture()
        );

        // simulate success
        callbackCaptor.getValue().onSuccess(mockProviderUser);

        // Then
        verify(mockView).hideLoading();
        verify(mockView).navigateToOnboarding(mockProviderUser);
        verify(mockView, never()).showError(anyString());
    }


    /*
    * CHILD TESTS
    */

    //TEST 1: Verify repo called for child
    @Test
    public void signUp_child_withValidData_shouldCallRepository() {
        // Given
        String username = "childUser";
        String password = "Password123!";
        String accessCode = "1234567";
        // When
        presenter.signUp(null, password, username, accessCode, "child");

        // Then
        verify(mockView).showLoading();
        verify(mockRepository).signUpChild(
                eq(username),
                eq(accessCode),
                eq(password),
                any(AuthContract.AuthCallback.class)
        );
    }

    //TEST 2: Child sign up without access code
    @Test
    public void signUp_child_withoutAccessCode_shouldShowError() {
        // Given
        String email = "provider@test.com";
        String password = "password123!";
        String username = "providerUser";
        String accessCode = "";
        // When
        presenter.signUp(email, password, username, accessCode, "child");
        // Then
        verify(mockView).showError("Access Code is required, or not in format.");
        verify(mockRepository, never()).signUpChild(anyString(), anyString(), anyString(), any());
    }

    //TEST 3: Child sign up without username (empty)
    @Test
    public void signUp_child_withoutUserName_shouldShowError() {
        // Given
        String email = "provider@test.com";
        String password = "password123!";
        String username = "";
        String accessCode = "129029";
        // When
        presenter.signUp(email, password, username, accessCode, "child");
        // Then
        verify(mockView).showError("Username is required and of minimum length 3");
        verify(mockRepository, never()).signUpChild(anyString(), anyString(), anyString(), any());
    }

    //TEST 3: Child sign up with weak username (length < 3)
    @Test
    public void signUp_child_weakUserName_shouldShowError() {
        // Given
        String email = "provider@test.com";
        String password = "password123!";
        String username = "de";
        String accessCode = "129029";
        // When
        presenter.signUp(email, password, username, accessCode, "child");
        // Then
        verify(mockView).showError("Username is required and of minimum length 3");
        verify(mockRepository, never()).signUpChild(anyString(), anyString(), anyString(), any());
    }

    //TEST 4: Child signup with success, navigate to home
    @Test
    public void signUp_child_success_shouldNavigateToHome() {
        // Given
        String username = "childUser";
        String password = "Password123!";
        String accessCode = "123456";

        User mockUser = new User("uid456", "childUser_child@smartair.com", username, "child", Arrays.asList("parent123"), List.of());
        // When
        presenter.signUp(null, password, username, accessCode, "child");
        // get callback
        verify(mockRepository).signUpChild(eq(username), eq(accessCode), eq(password), callbackCaptor.capture());
        // success
        callbackCaptor.getValue().onSuccess(mockUser);
        // Then
        verify(mockView).hideLoading();
        verify(mockView).navigateToOnboarding(mockUser);
    }

    /*
    * INVALID ROLE INSERTION TEST
    * */
    @Test
    public void signUp_invalidRole() {
        // Given
        String username = "childUser";
        String password = "Password123!";
        String accessCode = "123456";
        // When
        presenter.signUp("childUser_child@gmail.com", password, username, accessCode, "InvalidRole");
        // Then
        verify(mockView).hideLoading();
        verify(mockView).showError("Invalid role selected");
        //Verify none of the other stuff is called with invalid role
        verify(mockRepository, never()).signUpChild(anyString(), anyString(), anyString(), any());
        verify(mockRepository, never()).signUpProvider(anyString(), anyString(), anyString(), any());
        verify(mockRepository, never()).signUpParent(anyString(), anyString(), any()); //never called

    }

    /*
    * ON ROLE SELECTION TESTS
    */
    @Test
    public void onRoleSelected_child_shouldShowUsernameAccessField() {
        // When
        presenter.onRoleSelected("child");

        // Then
        verify(mockView).hideEmailField();
        verify(mockView).showUsernameField();
        verify(mockView).showAccessCodeField("Enter access code from your parent.");
    }

    @Test
    public void onRoleSelected_parent_shouldShowEmailHideAccessField() {
        // When
        presenter.onRoleSelected("parent");

        // Then
        verify(mockView).showEmailField();
        verify(mockView).hideAccessCodeField();
        verify(mockView).hideUsernameField();
    }

    @Test
    public void onRoleSelected_provider_shouldShowEmailAccessField() {
        // When
        presenter.onRoleSelected("provider");

        // Then
        verify(mockView).showEmailField();
        verify(mockView).hideUsernameField();
        verify(mockView).showAccessCodeField("Enter access code the caregiver.");

    }

    @Test
    public void onDestroy_shouldPreventLateCallbacksFromCrashing() {
        // Given
        String email = "parent@test.com";
        String password = "Password123!";
        User mockUser = new User("uid", email, null, "parent", null, List.of());
        //call signup before destroy
        presenter.signUp(email, password, null, null, "parent");
        // verify repo called
        verify(mockRepository).signUpParent(
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
        verify(mockView, never()).navigateToOnboarding(any(User.class));

    }

    //signUp(String email, String password, String username,String accessCode, String role)


}
