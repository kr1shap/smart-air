package com.example.smart_air;


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
import java.util.List;
import java.util.Locale;


public class ManageChildFragment extends Fragment implements ChildAdapter.OnChildClickListener {


    private static final String TAG = "ManageChildFragment";
    private ChildRepository childRepo;
    private AuthRepository authRepo;
    private RecyclerView rvChildren;
    private ChildAdapter childAdapter;
    private List<Child> childrenList;
    private TextView tvNoChildren, tvInviteCode, tvExpiryDate, tvCodeStatus;
    private Button btnGenerateCode;
    private LinearLayout inviteCodeSection;
    private Child selectedChild;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_child, container, false);


        childRepo = new ChildRepository();
        authRepo = new AuthRepository();
        childrenList = new ArrayList<>();


        rvChildren = view.findViewById(R.id.rv_children);
        tvNoChildren = view.findViewById(R.id.tv_no_children);
        inviteCodeSection = view.findViewById(R.id.invite_code_section);
        tvInviteCode = view.findViewById(R.id.tv_invite_code);
        tvExpiryDate = view.findViewById(R.id.tv_expiry_date);
        tvCodeStatus = view.findViewById(R.id.tv_code_status);
        btnGenerateCode = view.findViewById(R.id.btn_generate_code);


        setupRecyclerView();
        loadChildren();


        btnGenerateCode.setOnClickListener(v -> generateInviteCode());


        return view;
    }


    private void setupRecyclerView() {
        childAdapter = new ChildAdapter(childrenList, this);
        rvChildren.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChildren.setAdapter(childAdapter);
    }


    private void loadChildren() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No current user found");
            return;
        }


        Log.d(TAG, "Loading children for parent: " + currentUser.getUid());


        childRepo.getChildrenByParent(currentUser.getUid(),
                children -> {
                    childrenList.clear();
                    childrenList.addAll(children);
                    childAdapter.notifyDataSetChanged();


                    Log.d(TAG, "Loaded " + children.size() + " children");


                    if (children.isEmpty()) {
                        tvNoChildren.setVisibility(View.VISIBLE);
                        tvNoChildren.setText("No children registered yet.\n\nGenerate an invite code below and share it with your child to get started.");
                        rvChildren.setVisibility(View.GONE);
                    } else {
                        tvNoChildren.setVisibility(View.GONE);
                        rvChildren.setVisibility(View.VISIBLE);


                        // Select first child by default
                        if (selectedChild == null && !children.isEmpty()) {
                            selectedChild = children.get(0);
                        }
                    }


                    // Always check for existing invite (whether or not there are children)
                    checkExistingInvite();
                },
                e -> {
                    Log.e(TAG, "Error loading children", e);
                    Toast.makeText(getContext(), "Error loading children: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
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
                    Log.e(TAG, "Error checking invite", e);
                    Toast.makeText(getContext(), "Error checking invite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }


    private void generateInviteCode() {
        FirebaseUser currentUser = authRepo.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }


        btnGenerateCode.setEnabled(false);
        btnGenerateCode.setText("Generating...");


        Log.d(TAG, "Generating invite code for parent: " + currentUser.getUid());


        childRepo.generateInviteCode(currentUser.getUid(), "child",
                invite -> {
                    Log.d(TAG, "Invite code generated: " + invite.getCode());
                    displayInviteCode(invite);
                    Toast.makeText(getContext(), "Invite code generated!", Toast.LENGTH_SHORT).show();
                    btnGenerateCode.setEnabled(true);
                    btnGenerateCode.setText("Generate New Code");
                },
                e -> {
                    Log.e(TAG, "Error generating invite code", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGenerateCode.setEnabled(true);
                    btnGenerateCode.setText("Generate Code");
                });
    }


    private void displayInviteCode(Invite invite) {
        tvInviteCode.setText("Code: " + invite.getCode());
        tvInviteCode.setVisibility(View.VISIBLE);


        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String expiryDateStr = sdf.format(new Date(invite.getExpiresAt()));
        tvExpiryDate.setText("Expires: " + expiryDateStr);
        tvExpiryDate.setVisibility(View.VISIBLE);


        if (invite.isUsed()) {
            tvCodeStatus.setText("⚠ Code has been used. Generate a new one to add another child.");
            tvCodeStatus.setVisibility(View.VISIBLE);
            btnGenerateCode.setText("Generate New Code");
        } else {
            tvCodeStatus.setText("Share this code with your child to link their account.");
            tvCodeStatus.setVisibility(View.VISIBLE);
            btnGenerateCode.setText("Generate New Code");
        }
    }


    private void showNoCodeMessage() {
        tvInviteCode.setVisibility(View.GONE);
        tvExpiryDate.setVisibility(View.GONE);
        tvCodeStatus.setText("No active invite code.\n\nGenerate one to share with your child during their signup.");
        tvCodeStatus.setVisibility(View.VISIBLE);
        btnGenerateCode.setText("Generate Code");
    }


    @Override
    public void onChildClick(Child child) {
        // No-op: only edit button opens dialog
    }


    @Override
    public void onChildEdit(Child child) {
        Log.d(TAG, "Editing child: " + child.getName());
        EditChildDialogFragment editDialog = EditChildDialogFragment.newInstance(child);
        editDialog.setOnChildUpdatedListener(this::loadChildren);
        editDialog.show(getParentFragmentManager(), "EditChildDialog");
    }


    @Override
    public void onChildDelete(Child child) {
        // Show confirmation dialog before deleting
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Child")
                .setMessage("Are you sure you want to remove " + child.getName() + "? This will not delete their account, only unlink them from your parent account.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.d(TAG, "Deleting child: " + child.getName());
                    childRepo.deleteChild(child.getChildUid(),
                            aVoid -> {
                                Toast.makeText(getContext(), child.getName() + " removed", Toast.LENGTH_SHORT).show();
                                loadChildren();
                            },
                            e -> {
                                Log.e(TAG, "Error deleting child", e);
                                Toast.makeText(getContext(), "Error removing child: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadChildren();
    }
}

