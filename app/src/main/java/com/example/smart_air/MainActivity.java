package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.fragments.DashboardFragment;
import com.example.smart_air.fragments.ProviderReportFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    AuthRepository repo;
    Button signout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new DashboardFragment(), "dashboard")
                .commit();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(page -> {
            int id = page.getItemId();

            Fragment selectedFragment = null;

            if (id == R.id.home) {
                selectedFragment = new DashboardFragment();

            } else if (id == R.id.triage) {
                // Add triage fragment

            } else if (id == R.id.history) {
                // Add history fragment

            } else if (id == R.id.medicine) {
                // Add medicine fragment

            } else if (id == R.id.checkin) {
                // Add check-in fragment
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        repo = new AuthRepository();
        signout = findViewById(R.id.signout);

        signout.setOnClickListener(v -> {
            repo.signOut();
            startActivity(new Intent(MainActivity.this, LandingPageActivity.class));
            finish();
        });
    }
    public void openProviderReportPage(int months) {
        ProviderReportFragment fragment = ProviderReportFragment.newInstance(months);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
