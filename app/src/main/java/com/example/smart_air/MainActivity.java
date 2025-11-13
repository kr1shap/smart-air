package com.example.smart_air;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

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
                textView3.setText("Meidicine clicked!");
            } else if (id == R.id.checkin) {
                textView3.setText("Checkin clicked!");
            }

            return true;
        });





    }
}