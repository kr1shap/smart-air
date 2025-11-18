package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smart_air.Repository.AuthRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    AuthRepository repo;
    Button signout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        TextView textView3 = findViewById(R.id.textView3);

        bottomNavigationView.setOnItemSelectedListener(page -> {
            int id = page.getItemId();

            if (id == R.id.home) {
                textView3.setText("Home clicked!");
            } else if (id == R.id.triage) {
                textView3.setText("Triage clicked!");
            } else if (id == R.id.history) {
                textView3.setText("History clicked!");
            } else if (id == R.id.medicine) {
                textView3.setText("Medicine clicked!");
            } else if (id == R.id.checkin) {
                textView3.setText("Checkin clicked!");
            }

            return true;
        });


        //TODO: Remove after - test for signout
        repo = new AuthRepository();
        signout = findViewById(R.id.signout);

        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repo.signOut();
                 //TODO: Change to respective home page when done
                startActivity(new Intent(MainActivity.this, LandingPageActivity.class));
                finish();
            }
        });


    }
}