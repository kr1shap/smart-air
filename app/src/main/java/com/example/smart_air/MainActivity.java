package com.example.smart_air;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.Fragments.CheckInFragment;
import com.example.smart_air.fragments.HistoryFragment;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.fragments.NotificationFragment;
import com.example.smart_air.modelClasses.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;

import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    AuthRepository repo;
    Button signout;
    ImageButton notification;
    private ListenerRegistration unreadNotifListener;
    private NotificationRepository notifRepo;
    private boolean notifOnLogin; //so the notification toast fires only when new ones come in online
    private int prevNotifCount = -1; //previous count
    User user;

    // children tracking variables
    private SharedChildViewModel sharedModel;


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

        //init repo, db
        repo = new AuthRepository();
        notifRepo = new NotificationRepository();

        //if no user signed in
        repo = new AuthRepository();
        if(repo.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LandingPageActivity.class));
            finish();
        }

        // switch child button
        sharedModel = new ViewModelProvider(this).get(SharedChildViewModel.class);
        getChildren(); // fill array list of children in share modal
        ImageButton switchChildButton = findViewById(R.id.switchChildButton);
        setUpChildSwitch(switchChildButton); // set up button
        switchChildButton.setOnClickListener(v -> {
            sharedModel.getAllChildren().observe(this, children -> {
                if (children != null) {
                    showChildPopup(children);
                }
            });
        });

        //notif button
        notification = findViewById(R.id.notificationButton);
        setupNotificationIcon(); //checks current user and role

        //add listen on click
        notification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                  NotificationFragment notificationFragment = new NotificationFragment();
                  getSupportFragmentManager()
                          .beginTransaction()
                          .replace(R.id.fragment_container, notificationFragment) //go to notif fragment
                          .commit();
            }
        });

        //signout button
        signout = findViewById(R.id.signout);
        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repo.signOut();
                startActivity(new Intent(MainActivity.this, LandingPageActivity.class));
                finish();
            }
        });

        //bottom nav view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(page -> {
            int id = page.getItemId();

            Fragment selectedFragment = null;

            if (id == R.id.home) {
                // add fragment for dashboard
            } else if (id == R.id.triage) {
                // add fragment for triage
            } else if (id == R.id.history) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof HistoryFragment)) {
                    selectedFragment = new HistoryFragment();
                }
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


    }

    private void getChildren(){
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null){
                            return;
                        }
                        String role = user.getRole();
                        if(role.equals("child")){
                            return;
                        }
                        if(role.equals("parent") ){
                            List<String> list = user.getChildrenUid();
                            sharedModel.setChildren(list);
                        }
                        if(role.equals("provider")){
                            //switchChildButton.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        return;
                    }
                });
    }

    private void showChildPopup(List<String> children) {
        Integer value = sharedModel.getCurrentChild().getValue();
        AtomicInteger currentIndex = new AtomicInteger(value != null ? value : 0); // currently selected item

        String[] childArray = children.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Switch Child")
                .setSingleChoiceItems(childArray, currentIndex.get(), (dialog, which) -> {
                    sharedModel.setCurrentChild(which);

                    dialog.dismiss();
                })
                .show();
    }


    private void setUpChildSwitch(ImageButton switchChildButton) {
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null){
                            return;
                        }
                        String role = user.getRole();
                        if(role.equals("child")){
                            return;
                        }
                        if(role.equals("parent") ){
                            switchChildButton.setVisibility(View.VISIBLE);
                        }
                        if(role.equals("provider")){
                            switchChildButton.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        return;
                    }
                });

    }


    private void setupNotificationIcon() {
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user != null && !user.getRole().equals("parent")) {
                            notification.setVisibility(View.GONE);
                        } else {
                            notification.setVisibility(View.VISIBLE);
                            setupUnreadNotificationsBadge(repo.getCurrentUser().getUid());
                        }
                    }
                });
    }
    private void setupUnreadNotificationsBadge(String uid) {
         unreadNotifListener = notifRepo.listenForNotifications(uid, (value, error) -> {
            if (error != null || value == null) return;
            int size = value.size();
            if (prevNotifCount == -1) prevNotifCount = size;
            updateToolbarBadge(size); //unread count
            if(!notifOnLogin) notifOnLogin = true; //on login, notification toast only shows when a new one comes and they're online
            else if (prevNotifCount < size) Toast.makeText(MainActivity.this, "A new notification!", Toast.LENGTH_SHORT).show();
            prevNotifCount = size; //only show changes when size of document increases
         });
    }

    public void updateToolbarBadge(int count) {
        TextView badge = findViewById(R.id.badge_text);
        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(String.valueOf(count));
        } else {
            badge.setVisibility(View.GONE);
        }
    }
}