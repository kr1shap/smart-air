package com.example.smart_air;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.modelClasses.Invite;
import com.google.firebase.auth.FirebaseUser;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ManageProviderFragment extends Fragment {


    private static final String TAG = "ManageProviderFragment";
    private ChildRepository childRepo;
    private AuthRepository authRepo;
    private TextView tvInviteCode, tvCodeStatus, tvExpiryDate, tvNoProviders;
    private Button btnGenerateCode;
    private LinearLayout linkedProvidersContainer;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_provider, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @javax.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        childRepo = new ChildRepository();
        authRepo = new AuthRepository();

        //check if user is authenticated
        if (authRepo.getCurrentUser() == null) { redirectToLogin(); return; }

        tvInviteCode = view.findViewById(R.id.tv_invite_code);
        tvCodeStatus = view.findViewById(R.id.tv_code_status);
        tvExpiryDate = view.findViewById(R.id.tv_expiry_date);
        btnGenerateCode = view.findViewById(R.id.btn_generate_code);
        tvNoProviders = view.findViewById(R.id.tv_no_providers);
        linkedProvidersContainer = view.findViewById(R.id.linked_providers_container);

        btnGenerateCode.setOnClickListener(v -> generateInviteCode());

        checkExistingInvite();
        loadLinkedProviders();

    }


    private void checkExistingInvite() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) return;

        childRepo.getActiveInviteForParent(currentUser.getUid(), "provider",
                invite -> {
                    if (invite != null) { displayInviteCode(invite); }
                    else { showNoCodeMessage(); }
                },
                e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    @SuppressLint("SetTextI18n")
    private void generateInviteCode() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateCode.setEnabled(false);
        btnGenerateCode.setText("Generating...");


        childRepo.generateInviteCode(currentUser.getUid(), "provider",
                invite -> {
                    displayInviteCode(invite);
                    Toast.makeText(getContext(), "Code generated!", Toast.LENGTH_SHORT).show();
                    btnGenerateCode.setEnabled(true);
                    btnGenerateCode.setText("Re-generate New Code");
                },
                e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGenerateCode.setEnabled(true);
                    btnGenerateCode.setText("Re-generate New Code");
                });
    }


    @SuppressLint("SetTextI18n")
    private void displayInviteCode(Invite invite) {
        tvInviteCode.setText(invite.getCode());
        tvInviteCode.setVisibility(View.VISIBLE);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String expiryDateStr = sdf.format(new Date(invite.getExpiresAt()));
        tvExpiryDate.setText("Expires: " + expiryDateStr);
        tvExpiryDate.setVisibility(View.VISIBLE);

        if (invite.isUsed()) {
            tvCodeStatus.setText("⚠ Code has been used. Generate a new one to add another provider.");
            tvCodeStatus.setVisibility(View.VISIBLE);
            btnGenerateCode.setText("Generate New Code");
        } else if (invite.getExpiresAt() < System.currentTimeMillis()) {
            tvCodeStatus.setText("⚠ Code has been expired. Generate a new one to add a provider.");
            tvCodeStatus.setVisibility(View.VISIBLE);
            btnGenerateCode.setText("Generate New Code");
        } else {
            tvCodeStatus.setText("Share this code with your provider to link their account.");
            tvCodeStatus.setVisibility(View.VISIBLE);
            btnGenerateCode.setText("Re-generate Code");
        }
    }

    @SuppressLint("SetTextI18n")
    private void showNoCodeMessage() {
        tvInviteCode.setVisibility(View.GONE);
        tvExpiryDate.setVisibility(View.GONE);
        tvCodeStatus.setText("No active code. Generate one to share to your provider during their signup or linking.");
        tvCodeStatus.setVisibility(View.VISIBLE);
        btnGenerateCode.setText("Generate Code");
    }

    private void loadLinkedProviders() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) return;

        Log.d(TAG, "Loading linked providers for: " + currentUser.getUid());

        childRepo.getLinkedProviders(currentUser.getUid(),
                providers -> {
                    Log.d(TAG, "Received " + (providers != null ? providers.size() : 0) + " providers");

                    if (providers == null || providers.isEmpty()) {
                        tvNoProviders.setVisibility(View.VISIBLE);
                        linkedProvidersContainer.setVisibility(View.GONE);
                    } else {
                        tvNoProviders.setVisibility(View.GONE);
                        linkedProvidersContainer.setVisibility(View.VISIBLE);
                        displayLinkedProviders(providers);
                    }
                },
                e -> {
                    Log.e(TAG, "Error loading providers", e);
                    Toast.makeText(getContext(), "Error loading providers", Toast.LENGTH_SHORT).show();
                    tvNoProviders.setVisibility(View.VISIBLE);
                    linkedProvidersContainer.setVisibility(View.GONE);
                });
    }

    @SuppressLint("SetTextI18n")
    private void displayLinkedProviders(List<Invite> providers) {
        linkedProvidersContainer.removeAllViews();

        for (Invite invite : providers) {
            View item = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_linked_provider, linkedProvidersContainer, false);

            TextView tvEmail = item.findViewById(R.id.tv_provider_email);
            TextView tvDate = item.findViewById(R.id.tv_linked_date);
            Button btnUnlink = item.findViewById(R.id.btn_unlink);

            String email = invite.getUsedByEmail();
            tvEmail.setText(email != null ? email : "Unknown Provider");

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDate.setText("Linked: " + sdf.format(new Date(invite.getCreatedAt())));

            btnUnlink.setOnClickListener(v -> unlinkProvider(invite));

            linkedProvidersContainer.addView(item);
        }
    }


    private void unlinkProvider(Invite invite) {
        childRepo.unlinkProvider(invite.getCode(),
                aVoid -> {
                    Toast.makeText(getContext(), "Provider unlinked", Toast.LENGTH_SHORT).show();
                    loadLinkedProviders();
                },
                e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    @Override
    public void onResume() {
        super.onResume();
        loadLinkedProviders();
    }

    //redirect if user unauth, invalid
    private void redirectToLogin() {
        if (getActivity() == null) return;
        Toast.makeText(getContext(), "Please sign in again", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), LandingPageActivity.class));
        getActivity().finish();
    }

}

