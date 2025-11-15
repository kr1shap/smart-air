package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

// This Activity will correspond to your landing page XML layout.
public class LandingPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // link java file to xml layout
        setContentView(R.layout.activity_landing);
        // ref to get started
        Button getStartedButton = findViewById(R.id.getStartedButton);

        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingPageActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 4. Implement your auth logic here before setting the listener if needed
        // For example, if user is already logged in, you might redirect them instantly:
        // if (isUserLoggedIn()) {
        //     Intent homeIntent = new Intent(LandingPageActivity.this, HomeActivity.class);
        //     startActivity(homeIntent);
        //     finish();
        // }
    }
}