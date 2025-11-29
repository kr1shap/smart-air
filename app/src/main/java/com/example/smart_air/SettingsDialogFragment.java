package com.example.smart_air;


import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;


/**
 * SettingsDialogFragment
 * This fragment displays a small popup dialog (modal) containing navigation options
 * for managing Provider access and Child accounts. It is opened from the main UI
 * when user taps the "Manage Settings" button.
 * Its a quick shortcut instead of routing the user through multiple screens.
 */
public class SettingsDialogFragment extends DialogFragment {


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_settings, null);


        Button btnManageProvider = view.findViewById(R.id.btn_manage_provider);
        Button btnManageChild = view.findViewById(R.id.btn_manage_child);


        btnManageProvider.setOnClickListener(v -> {
            navigateToFragment(new ManageProviderFragment());
            dismiss();
        });


        btnManageChild.setOnClickListener(v -> {
            navigateToFragment(new ManageChildFragment());
            dismiss();
        });


        builder.setView(view);
        return builder.create();
    }


    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}

