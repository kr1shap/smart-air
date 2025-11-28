package com.example.smart_air.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.smart_air.R;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.modelClasses.enums.NotifType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MedicinesPageFragment extends Fragment {

    public MedicinesPageFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_medicines_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText controllerInput = view.findViewById(R.id.edit_controller_name);
        EditText rescueInput = view.findViewById(R.id.edit_rescue_name);

        Button saveControllerBtn = view.findViewById(R.id.btn_save_controller_name);
        Button saveRescueBtn = view.findViewById(R.id.btn_save_rescue_name);

        View backBtn = view.findViewById(R.id.btn_back_meds);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String childId = user.getUid();  // This is the key under /children

            // Load controller name
            db.collection("children")
                    .document(childId)
                    .collection("inventory")
                    .document("controller")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String controllerName = document.getString("name");
                            if (controllerName != null) controllerInput.setText(controllerName);
                        }
                    });

            // Load rescue name
            db.collection("children")
                    .document(childId)
                    .collection("inventory")
                    .document("rescue")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String rescueName = document.getString("name");
                            if (rescueName != null) rescueInput.setText(rescueName);
                        }
                    });

            // Save controller name
            saveControllerBtn.setOnClickListener(v -> {
                String newName = controllerInput.getText().toString().trim();
                if (!newName.isEmpty()) {
                    db.collection("children")
                            .document(childId)
                            .collection("inventory")
                            .document("controller")
                            .update("name", newName)
                            .addOnSuccessListener(unused -> Toast.makeText(requireContext(), "Controller name updated!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update controller name.", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(requireContext(), "Name can't be empty", Toast.LENGTH_SHORT).show();
                }
            });

            // Save rescue name
            saveRescueBtn.setOnClickListener(v -> {
                String newName = rescueInput.getText().toString().trim();
                if (!newName.isEmpty()) {
                    db.collection("children")
                            .document(childId)
                            .collection("inventory")
                            .document("rescue")
                            .update("name", newName)
                            .addOnSuccessListener(unused -> Toast.makeText(requireContext(), "Rescue name updated!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update rescue name.", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(requireContext(), "Name can't be empty", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    // rapid rescue alerts section
    /*
    check if 3+ rescue attempts made in 3 hours
     */
    public void rapidrescuealerts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null){
            return;
        }
        String childUid = user.getUid();
        long now=System.currentTimeMillis();
        long threeHours=3*60*60*1000;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .document(childUid)
                .collection("rescueLog")
                .get()
                .addOnSuccessListener(query -> {
                    int count = 0;
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("timeTaken");
                        if (ts != null) {
                            long rescueTime = ts.toDate().getTime();
                            if (now - rescueTime <= threeHours) {
                                count++;
                            }
                        }
                    }
                    if (count >= 3) {
                        sendAlert();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RescueCheck", "Failed to read rescueLog: ", e);
                });
    }
    /*
   Used to send notifications to parent
    */
    public void sendAlert() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String cUid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        getchildname(cUid, childName -> {
            db.collection("users")
                    .document(cUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Log.e("Rapid Rescue", "Child user document missing");
                            return;
                        }
                        List<String> parentUids = (List<String>) doc.get("parentUid");
                        if (parentUids == null || parentUids.isEmpty()) {
                            Log.e("Rapid Rescue", "No parentUid array found");
                            return;
                        }
                        NotificationRepository notifRepo = new NotificationRepository();
                        for (String pUid : parentUids) {
                            if (pUid == null){
                                continue;
                            }
                            Notification notif = new Notification(cUid, false, Timestamp.now(), NotifType.RAPID_RESCUE, childName);
                            notifRepo.createNotification(pUid, notif)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d("NotificationRepo", "Notification created for parent " + pUid))
                                    .addOnFailureListener(e ->
                                            Log.e("NotificationRepo", "Failed to notify parent " + pUid, e));
                        }

                    })
                    .addOnFailureListener(e ->
                            Log.e("Rapid Rescue", "Failed to load child document", e)
                    );

        }, error -> {
            Log.e("Rapid Rescue", "Failed to fetch child name", error);
        });
    }
    /*
    get child's name
     */
    public void getchildname(String childUid, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String childName = doc.getString("name");
                        onSuccess.onSuccess(childName);
                    } else {
                        onFailure.onFailure(new Exception("Child document not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

}
