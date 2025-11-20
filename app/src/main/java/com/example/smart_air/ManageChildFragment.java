package com.example.smart_air;

import android.annotation.SuppressLint;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.adapter.ChildAdapter;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.Invite;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
//import java.util.HashMap;
import java.util.List;
import java.util.Locale;
//import java.util.Map;

/**
 * ManageChildFragment
 * This screen allows a parent user to:
 *  - view all of their registered children
 *  - select a child to manage permissions (opens ChildDetailFragment)
 *  - delete a child
 *  - generate an invite code for a child account to join the parent
 * It displays a RecyclerView of children and manages the state of invite codes.
 */
public class ManageChildFragment extends Fragment implements ChildAdapter.OnChildClickListener {

    private ChildRepository childRepo;
    private AuthRepository authRepo;
    private RecyclerView rvChildren;
    private ChildAdapter childAdapter;
    private List<Child> childrenList;
    private TextView tvNoChildren, tvInviteCode, tvExpiryDate, tvCodeStatus;
    private Button btnGenerateCode;
    private LinearLayout inviteCodeSection;
    private Child selectedChild;


    /**
     * Called when the fragment's UI is being created.
     * Initializes repositories, RecyclerView, and loads the user's children.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_child, container, false);

        childRepo = new ChildRepository();
        authRepo = new AuthRepository();
        childrenList = new ArrayList<>();

        rvChildren = view.findViewById(R.id.rv_children);
        tvNoChildren = view.findViewById(R.id.tv_no_children);

        Button btnAddChild = view.findViewById(R.id.btn_add_child);

        inviteCodeSection = view.findViewById(R.id.invite_code_section);
        tvInviteCode = view.findViewById(R.id.tv_invite_code);
        tvExpiryDate = view.findViewById(R.id.tv_expiry_date);
        tvCodeStatus = view.findViewById(R.id.tv_code_status);
        btnGenerateCode = view.findViewById(R.id.btn_generate_code);

        setupRecyclerView();
        loadChildren();

        btnAddChild.setOnClickListener(v -> {
            // TODO: navigate to add child screen or show dialog
            Toast.makeText(getContext(), "Add child feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnGenerateCode.setOnClickListener(v -> generateInviteCode());

        return view;
    }

    private void setupRecyclerView() {
        childAdapter = new ChildAdapter(childrenList, this);
        rvChildren.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChildren.setAdapter(childAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadChildren() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) return;

        childRepo.getChildrenByParent(currentUser.getUid(),
                children -> {
                    childrenList.clear();
                    childrenList.addAll(children);
                    childAdapter.notifyDataSetChanged();

                    if (children.isEmpty()) {
                        tvNoChildren.setVisibility(View.VISIBLE);
                        rvChildren.setVisibility(View.GONE);
                        inviteCodeSection.setVisibility(View.GONE);
                    } else {
                        tvNoChildren.setVisibility(View.GONE);
                        rvChildren.setVisibility(View.VISIBLE);
                        // Select first child by default
                        if (selectedChild == null && !children.isEmpty()) {
                            selectedChild = children.get(0);
                            checkExistingInvite();
                        }
                    }
                },
                e -> {
                    Toast.makeText(getContext(), "Error loading children: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkExistingInvite() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) return;

        childRepo.getActiveInviteForParent(currentUser.getUid(), "child",
                invite -> {
                    if (invite != null) {
                        displayInviteCode(invite);
                    } else {
                        showNoCodeMessage();
                    }
                    inviteCodeSection.setVisibility(View.VISIBLE);
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

        childRepo.generateInviteCode(currentUser.getUid(), "child",
                invite -> {
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
        tvCodeStatus.setText("No active invite code. Generate one to share with a child.");
        tvCodeStatus.setVisibility(View.VISIBLE);
        btnGenerateCode.setText("Generate Code");
    }

    @Override
    public void onChildClick(Child child) {
        selectedChild = child;
        // Navigate to child detail fragment with sharing toggles
        ChildDetailFragment detailFragment = ChildDetailFragment.newInstance(child);
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onChildEdit(Child child) {
        // Handle edit
        Toast.makeText(getContext(), "Edit: " + child.getChildUid(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChildDelete(Child child) {
        childRepo.deleteChild(child.getChildUid(),
                aVoid -> {
                    Toast.makeText(getContext(), "Child deleted", Toast.LENGTH_SHORT).show();
                    loadChildren();
                },
                e -> {
                    Toast.makeText(getContext(), "Error deleting child: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}