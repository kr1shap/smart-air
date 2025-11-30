package com.example.smart_air.fragments;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.LandingPageActivity;
import com.example.smart_air.MainActivity;
import com.example.smart_air.R;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.adapter.NotificationsAdapter;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.viewmodel.NotificationViewModel;
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
    NotificationViewModel viewModel;

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
        //use viewmodel
        viewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        viewModel.getNotifications().observe(getViewLifecycleOwner(), list -> {
            notificationList.clear();
            notificationList.addAll(list);
            updateEmptyState();
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        });
        //listen after everything is setup
        viewModel.startListening(repo.getCurrentUser().getUid());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) { notificationListener.remove(); }
    }

}
