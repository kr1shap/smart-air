package com.example.smart_air;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.Locale;

/**
 * ManageProviderFragment
 * This fragment handles everything related to linking a healthcare provider
 * to a parent account. Providers access child health summaries,
 * so parents must explicitly share an invite code.
 * What this screen supports:
 *  - checking if a provider invite already exists (active & unused)
 *  - generating a new provider invite code
 *  - displaying the code, expiration date, and status
 * This is the provider equivalent of ManageChildFragment,
 * but simpler b/c no child list is needed.
 */
public class ManageProviderFragment extends Fragment {

    private ChildRepository childRepo;
    private AuthRepository authRepo;
    private TextView tvInviteCode, tvCodeStatus, tvExpiryDate;
    private Button btnGenerateCode;
    private Invite currentInvite;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_provider, container, false);

        childRepo = new ChildRepository();
        authRepo = new AuthRepository();

        tvInviteCode = view.findViewById(R.id.tv_invite_code);
        tvCodeStatus = view.findViewById(R.id.tv_code_status);
        tvExpiryDate = view.findViewById(R.id.tv_expiry_date);
        btnGenerateCode = view.findViewById(R.id.btn_generate_code);

        btnGenerateCode.setOnClickListener(v -> generateInviteCode());

        // Check for existing invite
        checkExistingInvite();

        return view;
    }

    private void checkExistingInvite() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) return;

        childRepo.getActiveInviteForParent(currentUser.getUid(), "provider",
                invite -> {
                    if (invite != null) {
                        currentInvite = invite;
                        displayInviteCode(invite);
                    } else {
                        showNoCodeMessage();
                    }
                },
                e -> {
                    Toast.makeText(getContext(), "Error checking invite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("SetTextI18n")
    private void generateInviteCode() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateCode.setEnabled(false);
        btnGenerateCode.setText("Generating...");

        childRepo.generateInviteCode(currentUser.getUid(), "provider",
                invite -> {
                    currentInvite = invite;
                    displayInviteCode(invite);
                    Toast.makeText(getContext(), "Invite code generated!", Toast.LENGTH_SHORT).show();
                    btnGenerateCode.setEnabled(true);
                    btnGenerateCode.setText("Generate New Code");
                },
                e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGenerateCode.setEnabled(true);
                    btnGenerateCode.setText("Generate Code");
                });
    }

    @SuppressLint("SetTextI18n")
    private void displayInviteCode(Invite invite) {
        tvInviteCode.setText("Code: " + invite.getCode());
        tvInviteCode.setVisibility(View.VISIBLE);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String expiryDateStr = sdf.format(new Date(invite.getExpiresAt()));
        tvExpiryDate.setText("Expires: " + expiryDateStr);
        tvExpiryDate.setVisibility(View.VISIBLE);

        if (invite.isUsed()) {
            tvCodeStatus.setText("⚠ Code has been used. Generate a new one.");
            tvCodeStatus.setVisibility(View.VISIBLE);
            btnGenerateCode.setText("Generate New Code");
        } else {
            tvCodeStatus.setVisibility(View.GONE);
            btnGenerateCode.setText("Generate New Code");
        }
    }

    @SuppressLint("SetTextI18n")
    private void showNoCodeMessage() {
        tvInviteCode.setVisibility(View.GONE);
        tvExpiryDate.setVisibility(View.GONE);
        tvCodeStatus.setText("No active invite code. Generate one to share with a provider.");
        tvCodeStatus.setVisibility(View.VISIBLE);
        btnGenerateCode.setText("Generate Code");
    }
}