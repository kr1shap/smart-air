package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.Gson;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.modelClasses.User;
import com.example.smart_air.Presenters.SignUpPresenter;
import java.util.Arrays;
import java.util.List;

public class SignUpActivity extends AppCompatActivity implements AuthContract.SignUpContract.View {
    private SignUpPresenter presenter;
    private EditText emailTextView, passwordTextView, userTextView, accessTextView;
    private LinearLayout emailLayout, userLayout, accessLayout;
    private Button submit, parentBtn, providerBtn, childBtn;
    List<Button> buttons; //button list
    private String selectedRole;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //Init presenter
        presenter = new SignUpPresenter(this);
        //Initalize buttons
        parentBtn = findViewById(R.id.parent);
        providerBtn = findViewById(R.id.provider);
        childBtn = findViewById(R.id.child);
        //Initalize layout
        emailLayout = findViewById(R.id.emailLayout);
        userLayout = findViewById(R.id.userLayout);
        accessLayout = findViewById(R.id.accessLayout);
        //Init progress bar
        progressBar = findViewById(R.id.progressBar);

        // Initialize text views, button submit
        emailTextView = findViewById(R.id.registerEmail);
        passwordTextView = findViewById(R.id.registerPassword);
        accessTextView = findViewById(R.id.registerAccessCode);
        userTextView = findViewById(R.id.registerUsername);
        submit = findViewById(R.id.signupButton);
//
        buttons = Arrays.asList(parentBtn, providerBtn, childBtn);
        updateSelection(parentBtn);

        parentBtn.setBackgroundTintList(getColorStateList(R.color.role_selected_bg));
        parentBtn.setOnClickListener(v -> {updateSelection(parentBtn);});
        providerBtn.setOnClickListener(v -> {updateSelection(providerBtn);});
        childBtn.setOnClickListener(v ->    {updateSelection(childBtn);});

        submit.setOnClickListener(v -> {
            String email = emailTextView.getText().toString();
            String username = userTextView.getText().toString();
            String password = passwordTextView.getText().toString();
            String accessCode = accessTextView.getText().toString();

            presenter.signUp(email, password, username, accessCode, selectedRole);
        });

        hideLoading();
        //TODO: uncomment after finishing
        //nav to signin
        findViewById(R.id.signInStatement).setOnClickListener(v -> {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });
    }


    private void updateSelection(Button selectedBtn) {
        if (selectedBtn == parentBtn) {
            selectedRole = "parent";
            presenter.onRoleSelected("parent");
        } else if (selectedBtn == providerBtn) {
            selectedRole = "provider";
            presenter.onRoleSelected("provider");
        } else if (selectedBtn == childBtn) {
            selectedRole = "child";
            presenter.onRoleSelected("child");
        }

        for (Button b : buttons) {
            if (b == selectedBtn) {
                b.setBackgroundTintList(getColorStateList(R.color.role_selected_bg));
            } else {
                b.setBackgroundTintList(getColorStateList(R.color.role_default_bg));
            }
        }
    }


    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        submit.setEnabled(false);
    }

    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
        submit.setEnabled(true);
    }

    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    @Override
    public void navigateToOnboarding(User user) {
        Intent intent = new Intent(this, MainActivity.class); //TODO: Replace with onboarding
        intent.putExtra("user", new Gson().toJson(user));
        startActivity(intent);
        finish(); //finish current activity
    }

    //TODO: remove?
    @Override
    public String getSelectedRole() { return selectedRole; }

    @Override
    public void showAccessCodeField(String hint) { accessLayout.setVisibility(View.VISIBLE); }

    @Override
    public void hideAccessCodeField() { accessLayout.setVisibility(View.GONE); }

    @Override
    public void showEmailField() { emailLayout.setVisibility(View.VISIBLE); }

    @Override
    public void hideEmailField() { emailLayout.setVisibility(View.GONE); }

    @Override
    public void showUsernameField() { userLayout.setVisibility(View.VISIBLE); }

    @Override
    public void hideUsernameField() { userLayout.setVisibility(View.GONE); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }


//
//    private void changeFields() {
//        //access text view
//        if (selectedRole.equals("parent")) {
//            accessLayout.setVisibility(View.GONE);
//        } else {
//            accessLayout.setVisibility(View.VISIBLE);
//        }
//
//        //email text view
//        if (selectedRole.equals("provider") || selectedRole.equals("parent")) {
//            emailLayout.setVisibility(View.VISIBLE);
//            userLayout.setVisibility(View.GONE);
//        } else {
//            emailLayout.setVisibility(View.GONE);
//            userLayout.setVisibility(View.VISIBLE);
//        }
//
//    }
//
//    private void updateSelection(Button selectedBtn) {
//
//        if (selectedBtn == parentBtn) {
//            selectedRole = "parent";
//        } else if (selectedBtn == providerBtn) {
//            selectedRole = "provider";
//        } else if (selectedBtn == childBtn) {
//            selectedRole = "child";
//        }
//
//        for (Button b : buttons) {
//            if (b == selectedBtn) {
//                b.setBackgroundTintList(getColorStateList(R.color.role_selected_bg));
//            } else {
//                b.setBackgroundTintList(getColorStateList(R.color.role_default_bg));
//            }
//        }
//    }
//
//    //Helper function
//    private boolean validatePassword(String password) {
//        if (password.isEmpty()) return false;
//
//        // strong password regex
//        String regex = "^(?=.*[0-9])" +        // at least 1 digit
//                "(?=.*[a-z])" +         // at least 1 lowercase
//                "(?=.*[A-Z])" +         // at least 1 uppercase
//                "(?=.*[@#$%^&+=!])" +   // at least 1 special char
//                "(?=\\S+$).{8,}$";      // no whitespace, min 8 chars
//
//        return password.matches(regex);
//    }
//    private boolean validateEmailUser(String emailUser) {
//        return !emailUser.isEmpty();
//    }
//
//    private void checkAccessCode(String accessCode, Consumer<String> callback) {
//        Timestamp now = Timestamp.now();
//        db.collection("invites")
//                .whereEqualTo("accessCode", accessCode)
//                .whereGreaterThanOrEqualTo("expiry", now)
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
//                        String parentUid = task.getResult().getDocuments().get(0).getString("parentUid");
//                        callback.accept(parentUid);
//                    } else {
//                        callback.accept(null); //not found
//                    }
//                });
//    }
//
//    private boolean registerParent() {
//        if (validateEmailUser(emailTextView.getText().toString()) && validatePassword(passwordTextView.getText().toString()))
//            return true;
//        else {
//            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//   }
//
//    private void registerProvider(String email, String password, String accessCode) {
//        // validate email/password first
//        if (!validateEmailUser(email) || !validatePassword(password)) {
//            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // check access code asynchronously
//        checkAccessCode(accessCode, parentUid -> {
//            if (parentUid != null) {
//                // Access code valid — proceed to create Firebase user
//                auth.createUserWithEmailAndPassword(email, password)
//                        .addOnCompleteListener(task -> {
//                            if (task.isSuccessful()) {
//                                String providerUid = auth.getCurrentUser().getUid();
//
//                                // Save provider in Firestore with parent UID
//                                db.collection("users").document(providerUid)
//                                        .set(Map.of(
//                                                "role", "provider",
//                                                "parentUid", List.of(parentUid),
//                                                "email", email
//                                        ));
//                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
//                                startActivity(new Intent(this, MainActivity.class));
//                                finish();
//                            } else {
//                                Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                            }
//                        });
//            } else {
//                Toast.makeText(this, "Invalid or expired access code", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//
//    private void registerNewUser() {
//        // Get values from input fields
//        String email = emailTextView.getText().toString().trim();
//        String password = passwordTextView.getText().toString().trim();
//        String accessCode = accessTextView.getText().toString().trim();
////        Log.d("access code", accessCode);
//        String user = userTextView.getText().toString().trim();
//        Boolean validInput = false;
//
//        //Validate input based on the role selected
//
//
//        switch (selectedRole){
//            case "parent":
////                validInput = registerParent();
//                break;
//            case "provider":
//                registerProvider(email, password, accessCode);
//                break;
//            case "child":
////                validateEmailUser(user);
//                break;
//            default:
//                break;
//        }
//        return;
//



        // Register new user with Firebase
//        auth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (task.isSuccessful()) {
//                            Toast.makeText(SignUpActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();
//                            // Navigate to MainActivity
//                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
//                            finish();
//                        } else {
//                            // Registration failed
//                            Toast.makeText(SignUpActivity.this, "Registration failed! Please try again later", Toast.LENGTH_LONG).show();
//                        }
//                    }
//                });
//    }
}
