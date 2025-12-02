package com.example.smart_air;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.security.CodeSigner;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.example.smart_air.fragments.BadgeFragment;
import com.example.smart_air.fragments.DashboardFragment;
import com.example.smart_air.fragments.TechniqueHelperFragment;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.fragments.DialogCodeFragment;
import com.example.smart_air.fragments.TriageFragment;
import com.example.smart_air.fragments.CheckInFragment;
import com.example.smart_air.viewmodel.DashboardViewModel;
import com.example.smart_air.viewmodel.NotificationViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.smart_air.fragments.HistoryFragment;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.fragments.NotificationFragment;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.User;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.example.smart_air.fragments.MedicineTabFragment;
public class MainActivity extends AppCompatActivity {
    AuthRepository repo;
    Button signout;
    ImageButton notification, providerCodeBtn;
    private NotificationRepository notifRepo;
    private boolean notifOnLogin; //so the notification toast fires only when new ones come in online
    private int prevNotifCount = -1; //previous count
    private NotificationViewModel notifVM;
    User user;
    private String userRole;

    // children tracking variables
    private SharedChildViewModel sharedModel;
    private DashboardViewModel dashboardsharedModel;
    private ListenerRegistration parentListener; // listener for when parent gets new child
    private ListenerRegistration providerChildrenListener; // listener for when child gets new provider

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        dashboardsharedModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new DashboardFragment(), "dashboard")
                .commit();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //init repo, db
        repo = new AuthRepository();
        notifRepo = new NotificationRepository();

        //if no user signed in
        if(repo.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LandingPageActivity.class));
            finish();
        }
        //check if child account is useless, then delete
        checkChildDeletion();

        // get menu item to disable/enable and switch child button
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        MenuItem dailyCheckIn = bottomNavigationView.getMenu().findItem(R.id.checkin);
        MenuItem triage = bottomNavigationView.getMenu().findItem(R.id.triage);
        ImageButton switchChildButton = findViewById(R.id.switchChildButton);
        //add code button for provider
        providerCodeBtn = findViewById(R.id.providerCodeBtn);

        //get user role
        sharedModel = new ViewModelProvider(this).get(SharedChildViewModel.class);
        getUserRole();
        sharedModel.getCurrentRole().observe(this, role -> {
            if (role != null) {
                this.userRole = role;
                setUpButtonAndListener(switchChildButton, dailyCheckIn, triage); // set up button
                getChildren();
            }
        });

        // showing child dropdown pop up
        switchChildButton.setOnClickListener(v -> { showChildPopup(); });

        //notif button & notif VM setup
        notifVM = new ViewModelProvider(this).get(NotificationViewModel.class);
        notifVM.setChildVM(sharedModel);
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

        //providerCodeBtn button
        providerCodeBtn.setOnClickListener(v -> {
            if(userRole != null && userRole.equals("provider")) {
                DialogCodeFragment dialog = new DialogCodeFragment();
                dialog.show(getSupportFragmentManager(), "DialogCodeFragment");
            }
        });

        //bottom nav view
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(page -> {
            int id = page.getItemId();

            Fragment selectedFragment = null;

            if (id == R.id.home) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof DashboardFragment)) {
                    selectedFragment = new DashboardFragment();
                }
            } else if (id == R.id.triage) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof TriageFragment)) {
                    selectedFragment = new TriageFragment();
                }
            } else if (id == R.id.history) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof HistoryFragment)) {
                    selectedFragment = new HistoryFragment();
                }
            } else if (id == R.id.medicine) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof MedicineTabFragment)) {
                    selectedFragment = new MedicineTabFragment();
                }
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

    private void getUserRole() {
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null){
                            return;
                        }
                        String role = user.getRole();
                        sharedModel.setCurrentRole(role);
                    }
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
        if(userRole.equals("child")){
            return;
        }
        else if(userRole.equals("parent")){
            repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null) {
                            return;
                        }
                        List<String> list = user.getChildrenUid();
                        convertToNames(list);
                    }
                });

        }
        else if(userRole.equals("provider")){
            getProviderChildren();
        }
    }

    // changes bottom navigation button based on parent's child list
    private void setBottomNavButtoms (boolean access){
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        MenuItem dailyCheckIn = bottomNavigationView.getMenu().findItem(R.id.checkin);
        MenuItem triage = bottomNavigationView.getMenu().findItem(R.id.triage);
        MenuItem history = bottomNavigationView.getMenu().findItem(R.id.history);
        MenuItem medicine = bottomNavigationView.getMenu().findItem(R.id.medicine);

        dailyCheckIn.setEnabled(access);
        dailyCheckIn.setCheckable(access);
        triage.setEnabled(access);
        triage.setCheckable(access);
        history.setEnabled(access);
        history.setCheckable(access);
        medicine.setEnabled(access);
        medicine.setCheckable(access);

        dailyCheckIn.setVisible(true);
        triage.setVisible(true);
        history.setVisible(true);
        medicine.setVisible(true);

        if(access){
            dailyCheckIn.setIcon(R.drawable.checkin);
            triage.setIcon(R.drawable.triage);
            history.setIcon(R.drawable.history);
            medicine.setIcon(R.drawable.medicine_24);
        }
        else{
            bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
            bottomNavigationView.setSelectedItemId(R.id.home);

            DashboardFragment dashboard = new DashboardFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, dashboard)
                    .commit();

            dailyCheckIn.setIcon(R.drawable.checkinlocked);
            triage.setIcon(R.drawable.triage_lock);
            history.setIcon(R.drawable.history_lock);
            medicine.setIcon(R.drawable.medicine_lock);
        }
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

                    convertToNamesAndDob(allChildren);
                    startChildrenListener();
                    });
    }

    private void convertToNamesAndDob(List<String> uid) {
        List<Child> children = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger counter = new AtomicInteger(0);

        if(uid.isEmpty()){
            sharedModel.setChildren(children);
            dashboardsharedModel.setRemovePage(true);
            return;
        }
        else{
            dashboardsharedModel.setRemovePage(false);
        }

        for (String childUid : uid) {
            db.collection("children").document(childUid).get()
                    .addOnSuccessListener(doc -> {
                        String name = childUid;
                        String dob = "";
                        if (doc.exists()) {
                            if (name != null) {
                                name = doc.getString("name");
                            }
                            if (doc.contains("dob")){
                                Timestamp dobTimestamp = doc.getTimestamp("dob");
                                Date date = dobTimestamp.toDate();

                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                dob = sdf.format(date);
                            }
                        }
                        if(!dob.isEmpty()){
                            name = name + " [" + dob + "]";
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
        if(userRole.equals("child")) {
            // enable dailycheckin and triage
             dailyCheckIn.setEnabled(true);
             dailyCheckIn.setCheckable(true);
             dailyCheckIn.setVisible(true);
             triage.setEnabled(true);
             triage.setCheckable(true);
             triage.setVisible(true);
             //disable notification
             notification.setVisibility(View.GONE);
             //disable add code btn
             providerCodeBtn.setVisibility(View.GONE);
             providerCodeBtn.setEnabled(false);
             return;
        }
        else if(userRole.equals("parent")) {
            switchChildButton.setVisibility(View.VISIBLE);

            // remove page if no children
            dashboardsharedModel.getRemovePage().observe(this, removePage -> {
                if(removePage != null){
                    setBottomNavButtoms(!removePage);
                }
            });

            // enable triage
            triage.setEnabled(true);
            triage.setCheckable(true);
            triage.setVisible(true);
            //disable add code btn
            providerCodeBtn.setVisibility(View.GONE);
            providerCodeBtn.setEnabled(false);
            // update child switching list when new child is added / deleted
            listenerToParent(repo.getCurrentUser().getUid(), true);
            //setup notification listener and icon
            notification.setVisibility(View.VISIBLE);
            setupUnreadNotificationsBadge(repo.getCurrentUser().getUid());
        }
        else if(userRole.equals("provider")) {
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
            //enable add code
            providerCodeBtn.setVisibility(View.VISIBLE);
            providerCodeBtn.setEnabled(true);
            // remove page if no children
            dashboardsharedModel.getRemovePage().observe(this, removePage -> {
                if(removePage != null){
                    setBottomNavButtoms(!removePage);
                }
            });
        }

    }

    // takes children uid and gives a Child list with it's uid and name to sharemodel
    private void convertToNames (List<String> uid) {
        List<Child> children = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger counter = new AtomicInteger(0);

        if(uid.isEmpty()){
            sharedModel.setChildren(children);
            dashboardsharedModel.setRemovePage(true);
            return;
        }
        else{
            dashboardsharedModel.setRemovePage(false);
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
        if (!"parent".equals(userRole)) return;
        notifVM.getUnreadCount().observe(this, count -> {
            if (count == null) return;
            updateToolbarBadge(count);
            // first time, don't show new notif
            if (!notifOnLogin) { notifOnLogin = true; }
            // new or future notif notifications
            else if (prevNotifCount < count) { Toast.makeText(MainActivity.this, "A new notification!", Toast.LENGTH_SHORT).show(); }
            prevNotifCount = count;
        });
        //start listening once only
        notifVM.startListening(uid);
    }

    private void updateToolbarBadge(int count) {
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

                    convertToNamesAndDob(allChildren);
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
