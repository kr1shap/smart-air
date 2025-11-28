
package com.example.smart_air.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
        // Use res/layout/fragment_medicine.xml
        return inflater.inflate(R.layout.fragment_medicine, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button backBtn = view.findViewById(R.id.btn_back_meds);
        if (backBtn != null) {
            backBtn.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        Button medicinesButton = view.findViewById(R.id.btn_medicines);
        if (medicinesButton != null) {
            medicinesButton.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new MedicinesPageFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        Button logDoseButton = view.findViewById(R.id.btn_log_dose);
        if (logDoseButton != null) {
            logDoseButton.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new LogDoseFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
    }


}
