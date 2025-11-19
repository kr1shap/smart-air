package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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

        //InputFilter to prevent spaces
        InputFilter spaceFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (Character.isWhitespace(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        };

        // apply InputFilter
        userTextView.setFilters(new InputFilter[] { spaceFilter });
        emailTextView.setFilters(new InputFilter[] { spaceFilter });

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
        Intent intent = new Intent(this, OnboardingActivity.class); //TODO: Replace with onboarding
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
}
