package com.example.smart_air;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart_air.Presenters.SignUpPresenter;
import com.google.gson.Gson;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.modelClasses.User;
import com.example.smart_air.Presenters.SignInPresenter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SignInActivity extends AppCompatActivity implements AuthContract.SignInContract.View {
    private SignInPresenter presenter;
    private EditText emailTextView, passwordTextView, userTextView;
    private LinearLayout emailLayout, userLayout;
    private Button submit, parentProvBtn, childBtn;
    List<Button> buttons; //button list
    private String selectedRole;
    private ProgressBar progressBar;

    private TextView forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        //Init presenter
        presenter = new SignInPresenter(this);
        //Initialize buttons
        parentProvBtn = findViewById(R.id.parentProvider);
        childBtn = findViewById(R.id.child);
        //Initialize layout
        emailLayout = findViewById(R.id.emailLayout);
        userLayout = findViewById(R.id.userLayout);
        //Init progress bar
        progressBar = findViewById(R.id.progressBar);
        // Initialize text views, button submit
        emailTextView = findViewById(R.id.loginEmail);
        passwordTextView = findViewById(R.id.loginPassword);
        userTextView = findViewById(R.id.loginUsername);
        submit = findViewById(R.id.signInButton);
        //text view
        forgotPassword = findViewById(R.id.forgotPassword);

        //Buttons
        buttons = Arrays.asList(parentProvBtn, childBtn);

        parentProvBtn.setOnClickListener(v -> {updateSelection(parentProvBtn);});
        childBtn.setOnClickListener(v ->    {updateSelection(childBtn);});

        submit.setOnClickListener(v -> {
            String password = passwordTextView.getText().toString();
            if (selectedRole.equals("child")) {
                String username = userTextView.getText().toString();
                presenter.signIn(username, password, selectedRole);
            } else {
                String email = emailTextView.getText().toString();
                presenter.signIn(email, password, selectedRole);
            }

        });

        hideLoading();
        updateSelection(parentProvBtn);
        //TODO: uncomment after finishing
        //nav to signup
        forgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        findViewById(R.id.signUpStatement).setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }


    private void updateSelection(Button selectedBtn) {
        if (selectedBtn == parentProvBtn) {
            selectedRole = "parentProv";
            presenter.onRoleSelected("parentProv");
        } else if (selectedBtn == childBtn){
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

    private void showForgotPasswordDialog() {

        View view = getLayoutInflater().inflate(R.layout.forgot_password, null);

        EditText emailInput = view.findViewById(R.id.resetWithEmail);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Send Reset Email", (d, w) -> {
                    presenter.sendPasswordReset(emailInput.getText().toString());
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.WHITE));
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
    public void navigateToHome(User user) {
        Intent intent = new Intent(this, MainActivity.class); //TODO: fix based on per role navigation or change to home if needed
        startActivity(intent);
        finish(); //finish current activity
    }

    //TODO: remove?
    @Override
    public String getSelectedRole() { return selectedRole; }

    @Override
    public void showEmailField() { emailLayout.setVisibility(View.VISIBLE); }

    @Override
    public void hideEmailField() { emailLayout.setVisibility(View.GONE); }

    @Override
    public void showUsernameField() { userLayout.setVisibility(View.VISIBLE); }

    @Override
    public void hideUsernameField() { userLayout.setVisibility(View.GONE); }

    @Override
    public void showForgotPassword() { forgotPassword.setVisibility(View.VISIBLE); }

    @Override
    public void hideForgotPassword() { forgotPassword.setVisibility(View.GONE); }


    @Override
    public void showSuccess(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

}
