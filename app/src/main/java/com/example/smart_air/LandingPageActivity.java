package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_air.Repository.AuthRepository;
import com.google.android.material.button.MaterialButton;

public class LandingPageActivity extends AppCompatActivity {

    AuthRepository repo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //init repo
        repo = new AuthRepository();
        // link java file to xml layout
        setContentView(R.layout.activity_landing);
        // ref to get started
        MaterialButton getStartedButton = findViewById(R.id.getStartedButton);

        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingPageActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // instant redirect user to page if already logged in
        //TODO: replace main with correct landing page
         if (repo.getCurrentUser() != null) {
             Intent homeIntent = new Intent(LandingPageActivity.this, MainActivity.class);
             startActivity(homeIntent);
             finish();
         }
    }
}