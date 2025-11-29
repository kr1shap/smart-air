package com.example.smart_air.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smart_air.R;

import java.util.Arrays;
import java.util.List;

public class ProviderReportFragment extends Fragment {

    private String childName;
    private String childDob;
    private String parentName;
    private String parentContact;

    public ProviderReportFragment() {
        // required empty constructor
    }

    public static ProviderReportFragment newInstance(int months) {
        ProviderReportFragment fragment = new ProviderReportFragment();
        Bundle args = new Bundle();
        args.putInt("months", months);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_provider_report, container, false);

        Spinner spinnerMonths = view.findViewById(R.id.spinnerMonths);
        Button btnGenerate = view.findViewById(R.id.btnGenerate);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        List<String> options = Arrays.asList(
                "Past 3 months",
                "Past 4 months",
                "Past 5 months",
                "Past 6 months"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                options
        );
        spinnerMonths.setAdapter(adapter);


        btnCancel.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnGenerate.setOnClickListener(v -> {
            String selected = spinnerMonths.getSelectedItem().toString();
            int months = Integer.parseInt(selected.replaceAll("\\D+", ""));

            Fragment dashboardFragment = requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag("dashboard");

            if (dashboardFragment instanceof DashboardFragment) {
                DashboardFragment dashboard = (DashboardFragment) dashboardFragment;
                dashboard.generateProviderReport(months);
            }

            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}
