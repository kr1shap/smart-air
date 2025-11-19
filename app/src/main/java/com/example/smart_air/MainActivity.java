package com.example.smart_air;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.TimerTask;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.Fragments.CheckInFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    AuthRepository repo;
    Button signout;
    static final String CHANNEL_ID ="smartairnotif";
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
        ensureNotificationPermission();
        createNotifChannel(); // makes notification channel
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(page -> {
            int id = page.getItemId();

            Fragment selectedFragment = null;

            if (id == R.id.home) {
                // add fragment for dashboard
            } else if (id == R.id.triage) {
                // switch page
                Intent intenttri = new Intent(MainActivity.this, TriageActivity.class);
                startActivity(intenttri);
                // parent alert function
                parentalertnotif();
                // timer function
                long endtime=System.currentTimeMillis()+10*60*1000L;
                TriageState.triageendtime=endtime;
            } else if (id == R.id.history) {
                // add fragment for history
            } else if (id == R.id.medicine) {
                // add fragment for medicine
            } else if (id == R.id.checkin) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof CheckInFragment)) {
                    selectedFragment = new CheckInFragment();
                }
            } else {
                return false; // unrecognized item
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
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
    public void parentalertnotif() {
        NotificationCompat.Builder mbuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.parentalert)
                .setContentTitle("Parent Alert")
                .setContentText("A triage session has started!")
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, mbuilder.build());
    }
    public void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SmartAir Notifications";
            String description = "General notifications for SmartAir app";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private static final int REQ_POST_NOTIFS = 1001;

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFS
                );
            }
        }
    }

}