package com.example.smart_air.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.R;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.modelClasses.BadgeData;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.User;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class BadgeFragment extends Fragment {
    private FirebaseFirestore db = FirebaseInitalizer.getDb();
    private AuthRepository repo;
    private ChildRepository childRepo;
    private SharedChildViewModel sharedModel;
    private Button btnClose;
    private CardView badge3Card, badge2Card, badge1Card; //[technique, controller, and rescue]
    private TextView controllerStreakNum, techniqueStreakNum;
    String childUid;
    //cache for badge data
    HashMap<String, BadgeData> badgeDataCache = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_badge_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        //init repo
        repo = new AuthRepository();
        childRepo = new ChildRepository();
        //check if user is authenticated
        if (repo.getCurrentUser() == null) { destroyFragment(); return; }
        //initalize all views and buttons, etc
        btnClose = view.findViewById(R.id.btnClose);
        badge1Card = view.findViewById(R.id.badge1Card);
        badge2Card = view.findViewById(R.id.badge2Card);
        badge3Card = view.findViewById(R.id.badge3Card);
        badge1Card.setAlpha(0.4f); //dimmen opacity of the card
        badge2Card.setAlpha(0.4f);
        badge3Card.setAlpha(0.4f);
        controllerStreakNum = view.findViewById(R.id.controllerStreakNum);
        techniqueStreakNum = view.findViewById(R.id.techniqueStreakNum);

        //extra check just to ensure role is child
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user == null) { return; }
                        String role = user.getRole();
                        if (role.equals("child")) { childUid = repo.getCurrentUser().getUid(); getBadgeStreakInfo(); }
                    }
                });

        // shared viewmodal
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> { // set up intial child (for when user is parent or provider)
            if (children != null && !children.isEmpty()) {
                int currentIndex = sharedModel.getCurrentChild().getValue() != null
                        ? sharedModel.getCurrentChild().getValue()
                        : 0;
                String currentChildUid = children.get(currentIndex).getChildUid();
                this.childUid = currentChildUid;
                getBadgeStreakInfo();
            }
        });

        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), currentIndex -> { // update each time child index changed
            List<Child> children = sharedModel.getAllChildren().getValue();
            if (children != null && !children.isEmpty() && currentIndex != null) {
                this.childUid = children.get(currentIndex).getChildUid();
                getBadgeStreakInfo();
            }
        });

        btnClose.setOnClickListener(v -> destroyFragment()); //close tab when button is pressed
    }

    /*
    * Method gets the badge streak info to update
    */
    private void getBadgeStreakInfo() {
        if(badgeDataCache.get(childUid) != null) {
            BadgeData data = badgeDataCache.get(childUid);
            updateBadgeUI(data.isControllerBadge(), data.isTechniqueBadge(), data.isLowRescueBadge(),
                    data.getTechniqueStreak(), data.getControllerStreak());
            return;
        }
        childRepo.getBadgeData(childUid)
                .addOnSuccessListener(data -> {
                    badgeDataCache.put(childUid, data);
                    updateBadgeUI(data.isControllerBadge(), data.isTechniqueBadge(), data.isLowRescueBadge(),
                            data.getTechniqueStreak(), data.getControllerStreak());
                })
                .addOnFailureListener(e -> {
                    Log.e("BadgeFragment", "Failed to fetch badges and streaks!", e);
                    Toast.makeText(getActivity(), "Failed to load badges and streaks!", Toast.LENGTH_SHORT).show();
                });
    }

    /*
    * Method updates badge UI
    */
    private void updateBadgeUI(boolean controllerBadge, boolean techniqueBadge, boolean rescueBadge,
                               int techniqueStreak, int controllerStreak) {
        Log.d("BadgeFragment", "Updating badge UI with controller: + " + controllerBadge +
                ", technique: " + techniqueBadge + ", rescue: " + rescueBadge);
        badge1Card.setAlpha(techniqueBadge ? 1.0f : 0.4f); //dimmen opacity of the card
        badge2Card.setAlpha(controllerBadge ? 1.0f : 0.4f);
        badge3Card.setAlpha(rescueBadge ? 1.0f : 0.4f);
        //set the streak text
        techniqueStreakNum.setText(String.valueOf(techniqueStreak));
        controllerStreakNum.setText(String.valueOf(controllerStreak));
    }

    /*
     * a method called when an error occurs, goes back the main activity
     */
    public void destroyFragment() {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment instanceof BadgeFragment) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }
}
