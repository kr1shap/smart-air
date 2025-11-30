package com.example.smart_air.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.smart_air.fragments.LogDoseFragment;


import com.example.smart_air.R;

public class MedicineTabFragment extends Fragment {

    public MedicineTabFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_medicine, container, false);

        // 1. Find the Log Dose button
        Button logDoseButton = view.findViewById(R.id.btn_log_dose);

        // 2. Wire click to open LogDoseFragment
        if (logDoseButton != null) {
            logDoseButton.setOnClickListener(v -> openLogDoseFragment());
        }

        // (If you have other buttons, hook them up here too)

        return view;
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find the "Medicines" button from fragment_medicine.xml
        View medicinesButton = view.findViewById(R.id.btn_medicines); // use the actual id from your XML
        Button backBtn = view.findViewById(R.id.btn_back_meds);
        if (backBtn != null) {
            backBtn.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }
        // 2. When user taps it, open the My Medications page
        medicinesButton.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MedicinesPageFragment())
                    .addToBackStack(null)   // so back button returns to the menu
                    .commit();
        });
        Button inventoryButton = view.findViewById(R.id.btn_inventory);
        if (inventoryButton != null) {
            inventoryButton.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new InventoryFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

    }
}