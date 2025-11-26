package com.example.smart_air;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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

import com.example.smart_air.Contracts.AuthContract;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.Fragments.CheckInFragment;
import com.example.smart_air.fragments.HistoryFragment;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.fragments.NotificationFragment;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.User;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
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
    private ListenerRegistration parentListener;


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

        // get menu item to disable/enable
        MenuItem dailyCheckIn = bottomNavigationView.getMenu().findItem(R.id.checkin);
        MenuItem triage = bottomNavigationView.getMenu().findItem(R.id.triage);

        // switch child button
        sharedModel = new ViewModelProvider(this).get(SharedChildViewModel.class);
        getChildren(); // fill array list of children in share modal
        ImageButton switchChildButton = findViewById(R.id.switchChildButton);
        setUpButtonAndListener(switchChildButton, bottomNavigationView, dailyCheckIn, triage); // set up button
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
    private void listenerToParent(String parentUid) {
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
            for (String uid : newChildren) {
                if (!previousChildren.contains(uid)) {
                    getChildren();
                    break;
                }
            }
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
                            convertToNames(list);
                        }
                        if(role.equals("provider")){
                            List<String> list = user.getParentUid();
                            getParentsChildren(list);
                        }
                    }
                });
    }

    private void getParentsChildren(List<String> list) {
        List<String> allChildren = new ArrayList<>();
        AtomicInteger processedCount = new AtomicInteger(0);
        for(String parentId : list) {
            repo.getUserDoc(parentId).addOnSuccessListener(doc -> {
                if(doc.exists()) {
                    User user = doc.toObject(User.class);
                    if (user != null && user.getChildrenUid() != null) {
                        allChildren.addAll(user.getChildrenUid());
                    }
                }

                if (processedCount.incrementAndGet() == list.size()) {
                    convertToNames(allChildren);
                }
            });
        }
    }

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


    private void setUpButtonAndListener(ImageButton switchChildButton, BottomNavigationView bottomNavigationView, MenuItem dailyCheckIn, MenuItem triage) {
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
                            return;
                        }
                        if(role.equals("parent") ){
                            switchChildButton.setVisibility(View.VISIBLE);
                            // enable dailycheckin and triage
                            dailyCheckIn.setEnabled(true);
                            dailyCheckIn.setCheckable(true);
                            dailyCheckIn.setVisible(true);
                            triage.setEnabled(true);
                            triage.setCheckable(true);
                            triage.setVisible(true);

                            // update child switching list when new child is added / deleted
                            listenerToParent(repo.getCurrentUser().getUid());
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

                            // update child switching list when new child is added / deleted
                            listenerToProvider();
                        }
                    }
                    else{
                        return;
                    }
                });

    }

    private void listenerToProvider() {
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null) {return;}
                        List <String> parentUids = user.getParentUid();
                        for(String parentUid: parentUids){
                            listenerToParent(parentUid);
                        }

                    }
                });
    }

    private void convertToNames (List<String> uid) {
        List<Child> children = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger counter = new AtomicInteger(0);

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