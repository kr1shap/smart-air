package com.example.smart_air.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smart_air.R;

public class MedicinesPageFragment extends Fragment {

    public MedicinesPageFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // This links the fragment to fragment_medicines_page.xml
        return inflater.inflate(R.layout.fragment_medicines_page, container, false);
    }
}