package com.example.smart_air.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
        // This tells it to use res/layout/fragment_medicine.xml
        return inflater.inflate(R.layout.fragment_medicine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find the "Medicines" button from fragment_medicine.xml
        View medicinesButton = view.findViewById(R.id.btn_medicines); // use the actual id from your XML

        // 2. When user taps it, open the My Medications page
        medicinesButton.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MedicinesPageFragment())
                    .addToBackStack(null)   // so back button returns to the menu
                    .commit();
        });

    }
}
