package com.example.smart_air.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.LandingPageActivity;
import com.example.smart_air.MainActivity;
import com.example.smart_air.R;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.adapter.NotificationsAdapter;
import com.example.smart_air.modelClasses.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationFragment extends Fragment {
    private View view;
    private AuthRepository repo;
    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private NotificationRepository notifRepo;
    private List<Notification> notificationList = new ArrayList<>();
    private ListenerRegistration notificationListener;
    private ProgressBar progressBar;
    private Map<String, String> childNameCache = new HashMap<>(); //to not fetch 100 times

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notifications_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @javax.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        //init repo
        repo = new AuthRepository();
        //check if user is authenticated
        if (repo.getCurrentUser() == null) { redirectToLogin(); return; }

        notifRepo = new NotificationRepository();
        //get the progress bar
        progressBar = view.findViewById(R.id.notificationsProgressBar);
        //init recycler view
        recyclerView = view.findViewById(R.id.notificationsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //set adapter for recycle view
        adapter = new NotificationsAdapter(notificationList, this::markNotificationAsRead);
        recyclerView.setAdapter(adapter);
        fetchUserAndNotifications();
    }

    private void fetchUserAndNotifications() {
        if (repo.getCurrentUser() == null) {
            redirectToLogin(); return;
        }
        String uid = repo.getCurrentUser().getUid();
        repo.getUserDoc(uid)
            .addOnSuccessListener(userDoc -> {
                if (!userDoc.exists()) { redirectToLogin(); return; }
                String role = userDoc.getString("role");
                if (role != null && role.equals("parent")) {
                    listenForNotifications(uid);
                } else redirectToHome();
            })
            .addOnFailureListener(e -> redirectToLogin());
    }

    //function listens for notifications given uid for parent
    private void listenForNotifications(String uid) {
        // show loading
        progressBar.setVisibility(View.VISIBLE);
        notificationListener = notifRepo.listenForNotifications(uid, (value, error) -> {
            progressBar.setVisibility(View.GONE); //done loading
            if (error != null || value == null) return;
            notificationList.clear();
            //get all documents
            for (DocumentSnapshot doc : value.getDocuments()) {
                Notification notif = doc.toObject(Notification.class);
                if (notif != null) {
                    notif.setNotifUid(doc.getId());
                    notif.setChildName("N/A"); //default (just in case)
                    notificationList.add(notif); // add immediately
                }
            }

            updateEmptyState();
            sortAndUpdate();
            adapter.notifyDataSetChanged(); // update ui

            // fetch child names async
            for (Notification notif : notificationList) {
                String childUid = notif.getChildUid();
                if (childUid == null || childUid.isEmpty()) continue;

                if (childNameCache.containsKey(childUid)) { //if in cache, all good
                    notif.setChildName(childNameCache.get(childUid));
                    continue;
                }
                //fetch if not in cache
                notifRepo.fetchNotifChildName(childUid)
                        .addOnSuccessListener(childDoc -> {
                            if (childDoc.exists()) {
                                String name = childDoc.getString("name");
                                childNameCache.put(childUid, name);
                                //update all notifs with that uid with that name
                                for (Notification n : notificationList) {
                                    if (childUid.equals(n.getChildUid())) { n.setChildName(name); }
                                }
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .addOnFailureListener(e -> Log.e("Notif fragment", "Failed to fetch child name", e));
            }
        });
    }

    //sort list
    private void sortAndUpdate() {
        notificationList.sort((a, b) ->
                Long.compare(
                        Notification.convertTimestampToMillis(b.getTimestamp()),
                        Notification.convertTimestampToMillis(a.getTimestamp())
                )
        );
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    //update state if no notifs
    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            view.findViewById(R.id.noNotificationsText).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.noNotificationsText).setVisibility(View.GONE);
        }
    }

    //mark notif as read (delete)
    private void markNotificationAsRead(Notification notification) {
        String uid = repo.getCurrentUser().getUid();
        String notifId = notification.getNotifUid();

        notificationList.remove(notification);
        sortAndUpdate();

        notifRepo.markNotificationAsRead(uid, notifId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Notification marked as read!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to mark notification as read.", Toast.LENGTH_SHORT).show();
                });
    }

    //redirect if user unauth, invalid
    private void redirectToLogin() {
        if (getActivity() == null) return;
        Toast.makeText(getContext(), "Please sign in again", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), LandingPageActivity.class));
        getActivity().finish();
    }

    private void redirectToHome() {
        if (getActivity() == null) return;
        Toast.makeText(getContext(), "Unauthorized page", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), MainActivity.class));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) { notificationListener.remove(); }
    }

}
