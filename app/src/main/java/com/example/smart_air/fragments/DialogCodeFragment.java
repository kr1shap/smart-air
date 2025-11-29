package com.example.smart_air.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.R;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.Repository.ChildRepository;
import com.example.smart_air.viewmodel.SharedChildViewModel;

public class DialogCodeFragment extends DialogFragment {
    private Button btn_connect_provider, btn_cancel_provider;
    private EditText providerParentCode;
    private ChildRepository childRepo;
    private AuthRepository authRepo;
    private SharedChildViewModel sharedModel;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_enterprovidercode, null);

        //init repo
        authRepo = new AuthRepository();
        if(authRepo.getCurrentUser() == null) { dismiss(); }
        childRepo = new ChildRepository();

        // shared viewmodal to check for role
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        sharedModel.getCurrentRole().observe(this, role -> {
            if (role != null && !role.equals("provider")) { dismiss(); }
        });

        btn_connect_provider= view.findViewById(R.id.btn_connect_provider);
        btn_cancel_provider  = view.findViewById(R.id.btn_cancel_provider);
        providerParentCode = view.findViewById(R.id.providerParentCode);

        btn_cancel_provider.setOnClickListener(v -> { dismiss(); });

        btn_connect_provider.setOnClickListener(v -> {
            if(providerParentCode.getText() == null || providerParentCode.getText().toString().trim().isEmpty()) {
                Toast.makeText(requireActivity(), "Empty invite code!", Toast.LENGTH_SHORT).show();
                return;
            }
            childRepo.useInviteCode(providerParentCode.getText().toString().trim(), authRepo.getCurrentUser().getUid(), "provider",
                    code -> {
                        Toast.makeText(requireActivity(), "Invite used!", Toast.LENGTH_SHORT).show();
                        dismiss();
                    },
                    e -> { Toast.makeText(requireActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        builder.setView(view);
        return builder.create();
    }



}
