package com.example.smart_air.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.fragments.LogDoseFragment;


import com.example.smart_air.R;
import com.example.smart_air.viewmodel.SharedChildViewModel;

public class MedicineTabFragment extends Fragment {
    //Instantiate view model
    SharedChildViewModel childVM;

    public MedicineTabFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_medicine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // 1. Find the Log Dose button
        Button logDoseButton = view.findViewById(R.id.btn_log_dose);
        Button btn_streaks = view.findViewById(R.id.btn_streaks);
        Button inventoryButton = view.findViewById(R.id.btn_inventory);
        //child vm
        childVM = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        //observe role in VM
        childVM.getCurrentRole().observe(
                getViewLifecycleOwner(),
                role -> {
                    if(role!= null) {
                        if ("provider".equals(role) || "child".equals(role)) {
                            inventoryButton.setVisibility(View.GONE);
                        } else {
                            inventoryButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
        // 2. Wire click to open LogDoseFragment
        if (logDoseButton != null) {
            logDoseButton.setOnClickListener(v -> openLogDoseFragment());
        }
        if(btn_streaks != null) {
            btn_streaks.setOnClickListener(v -> openBadgeFragment());
        }
        if (inventoryButton != null) {
            inventoryButton.setOnClickListener(v -> { openInventoryFragment();});
        }

    }

    private void openInventoryFragment() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new InventoryFragment())
                .addToBackStack(null)
                .commit();
    }

    private void openLogDoseFragment() {
        // This uses the same container as MainActivity (fragment_container)
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new LogDoseFragment())
                .addToBackStack(null)  // so back button returns to Medicine tab
                .commit();
    }

    private void openBadgeFragment() {
        // This uses the same container as MainActivity (fragment_container)
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BadgeFragment())
                .addToBackStack(null)  // so back button returns to Medicine tab
                .commit();
    }
}