package com.example.smart_air;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.TimerTask;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.fragments.TriageFragment;
import com.example.smart_air.fragments.CheckInFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import com.example.smart_air.fragments.HistoryFragment;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.fragments.NotificationFragment;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.User;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    private ListenerRegistration parentListener; // listener for when parent gets new child
    private ListenerRegistration providerChildrenListener; // listener for when child gets new provider
    private boolean removeDailyCheckIn = true; // boolean for parent when they have no children and thus daily check in should be removed


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
        //check if child account is useless, then delete
        checkChildDeletion();

        //notif button
        notification = findViewById(R.id.notificationButton);

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
                // switch page
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof TriageFragment)) {
                    selectedFragment = new TriageFragment();
                }
                //fragment for triage
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

        // get menu item to disable/enable
        MenuItem dailyCheckIn = bottomNavigationView.getMenu().findItem(R.id.checkin);
        MenuItem triage = bottomNavigationView.getMenu().findItem(R.id.triage);

        // switch child button
        sharedModel = new ViewModelProvider(this).get(SharedChildViewModel.class);
        getChildren(); // fill array list of children in share modal
        ImageButton switchChildButton = findViewById(R.id.switchChildButton);
        setUpButtonAndListener(switchChildButton, dailyCheckIn, triage); // set up button
        switchChildButton.setOnClickListener(v -> {
            showChildPopup();
        });


    }

    private void checkChildDeletion() {
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null) {
                            return;
                        }
                        if (user.getRole().equals("child") && (user.getParentUid() == null || user.getParentUid().isEmpty())) {
                            //get the array list
                            repo.deleteCurrentUser(deleteCallback());
                        }
                    }
                });
    }
    private void listenerToParent(String parentUid, boolean parent) {
        DocumentReference parentRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(parentUid);

        parentListener = parentRef.addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) {
                return;
            }

            // get new children array from Firestore parent doc
            List<String> newChildren = (List<String>) snapshot.get("childrenUid");
            if (newChildren == null) newChildren = new ArrayList<>();

            // get previous children list from SharedViewModel
            List<String> previousChildren = Arrays.asList(convertChildAllChildrenList());
            if (previousChildren == null) {previousChildren = new ArrayList<>();}

            // check for newly added child
            if(parent){
                for (String uid : newChildren) {
                    if (!previousChildren.contains(uid)) {
                        getChildren();
                        break;
                    }
                }
                for (String uid : previousChildren) {
                    if (!newChildren.contains(uid)) {
                        getChildren();
                        break;
                    }
                }
            }
            else{
                for (String uid : newChildren) {
                    if (!previousChildren.contains(uid)) {
                        getChildren();
                        break;
                    }
                }
            }
        });
    }

    // calls right function to get children based on role
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
                            // if list is empty now removes daily check in
                            if(list.isEmpty()){
                                removeDailyCheckIn = true;
                            }
                            else{
                                removeDailyCheckIn = false;
                            }
                            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
                            MenuItem dailyCheckIn = bottomNavigationView.getMenu().findItem(R.id.checkin);
                            if(removeDailyCheckIn) {
                                dailyCheckIn.setEnabled(false);
                                dailyCheckIn.setCheckable(false);
                                dailyCheckIn.setIcon(R.drawable.checkinlocked);
                                // TODO: go back to home fragment if on check in fragment
                            }
                            else{
                                dailyCheckIn.setEnabled(true);
                                dailyCheckIn.setCheckable(true);
                                dailyCheckIn.setIcon(R.drawable.checkin);
                            }
                            dailyCheckIn.setVisible(true);
                            convertToNames(list);
                        }
                        if(role.equals("provider")){
                            getProviderChildren();
                        }
                    }
                });
    }

    // gets providers children from "children" collection
    private void getProviderChildren() {
        List<String> allChildren = new ArrayList<>();
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("children")
                .whereArrayContains("allowedProviderUids",currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                    for (DocumentSnapshot doc : docs) {
                        allChildren.add(doc.getId());
                    }

                    convertToNames(allChildren);
                    startChildrenListener();
                    });
    }


    // show popup with all children
    private void showChildPopup() {
        Integer value = sharedModel.getCurrentChild().getValue();
        AtomicInteger currentIndex = new AtomicInteger(value != null ? value : 0); // currently selected item

        String [] childArray = convertChildAllChildrenList();

        new AlertDialog.Builder(this)
                .setTitle("Switch Child")
                .setSingleChoiceItems(childArray, currentIndex.get(), (dialog, which) -> {
                    sharedModel.setCurrentChild(which);

                    dialog.dismiss();
                })
                .show();
    }

    // convert array list to array
    private String [] convertChildAllChildrenList(){
        List <Child> children = sharedModel.getAllChildren().getValue();
        if(children == null){
            return new String[0];
        }
        String [] childArray = new String[children.size()];
        for(int i = 0; i < children.size(); i++){
            childArray[i] = children.get(i).getName();
        }
        return childArray;
    }


    // sets up buttons for child dropdown based on role
    private void setUpButtonAndListener(ImageButton switchChildButton, MenuItem dailyCheckIn, MenuItem triage) {
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null){
                            return;
                        }
                        String role = user.getRole();
                        if(role.equals("child")){
                            // enable dailycheckin and triage
                            dailyCheckIn.setEnabled(true);
                            dailyCheckIn.setCheckable(true);
                            dailyCheckIn.setVisible(true);
                            triage.setEnabled(true);
                            triage.setCheckable(true);
                            triage.setVisible(true);
                            //disable notification
                            notification.setVisibility(View.GONE);
                            return;
                        }
                        if(role.equals("parent") ){
                            switchChildButton.setVisibility(View.VISIBLE);
                            // enable dailycheckin
                            if(removeDailyCheckIn) {
                                dailyCheckIn.setEnabled(false);
                                dailyCheckIn.setCheckable(false);
                                dailyCheckIn.setIcon(R.drawable.checkinlocked);
                                // TODO: go back to home fragment if on check in fragment
                            }
                            else{
                                dailyCheckIn.setEnabled(true);
                                dailyCheckIn.setCheckable(true);
                                dailyCheckIn.setIcon(R.drawable.checkin);
                            }
                            dailyCheckIn.setVisible(true);

                            // enable triage
                            triage.setEnabled(true);
                            triage.setCheckable(true);
                            triage.setVisible(true);

                            // update child switching list when new child is added / deleted
                            listenerToParent(repo.getCurrentUser().getUid(), true);
                            //setup notification listener and icon
                            notification.setVisibility(View.VISIBLE);
                            setupUnreadNotificationsBadge(repo.getCurrentUser().getUid());
                        }
                        if(role.equals("provider")){
                            // show button
                            switchChildButton.setVisibility(View.VISIBLE);
                            // disable dailycheckin and triage
                            dailyCheckIn.setEnabled(false);
                            dailyCheckIn.setCheckable(false);
                            dailyCheckIn.setVisible(false);
                            triage.setEnabled(false);
                            triage.setCheckable(false);
                            triage.setVisible(false);
                            //disable notification
                            notification.setVisibility(View.GONE);

                        }
                    }
                    else{
                        return;
                    }
                });

    }

    // takes children uid and gives a Child list with it's uid and name to sharemodel
    private void convertToNames (List<String> uid) {
        List<Child> children = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger counter = new AtomicInteger(0);

        if(uid.isEmpty()){
            sharedModel.setChildren(children);
            return;
        }

        for (String childUid : uid) {
            db.collection("children").document(childUid).get()
                    .addOnSuccessListener(doc -> {
                        String name = childUid;
                        if (doc.exists()) {
                            if (name != null) {
                                name = doc.getString("name");
                            }
                        }
                        Child currentChild = new Child(childUid, name);
                        children.add(currentChild);

                        if (counter.incrementAndGet() == uid.size()) {
                            children.sort(Comparator.comparing(c -> c.getName().toLowerCase()));
                            int currentIndex = sharedModel.getCurrentChild().getValue();
                            if(currentIndex >= children.size()){ // if last child gets deleted
                                sharedModel.setCurrentChild(0);
                            }
                            sharedModel.setChildren(children);
                        }
                    });

        }
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

    // checks children to see if current providers list has changed
    private void startChildrenListener() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        providerChildrenListener = db.collection("children")
                .whereArrayContains("allowedProviderUids", currentUid)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (querySnapshot == null) return;

                    List<String> allChildren = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        allChildren.add(doc.getId());
                    }

                    convertToNames(allChildren);
                });
    }

    //Callback for main call in general, used to delete account
    private AuthContract.GeneralCallback deleteCallback() {
        return new AuthContract.GeneralCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LandingPageActivity.class));
                finish();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show(); //error in view
            }
        };
    }
}
