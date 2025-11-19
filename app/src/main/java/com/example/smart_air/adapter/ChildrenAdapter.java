package com.example.smart_air.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.Child;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChildrenAdapter extends RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder> {

    private List<Child> childrenList;
    private OnChildClickListener listener;

    public interface OnChildClickListener {
        void onChildClick(Child child);
    }

    public ChildrenAdapter(List<Child> childrenList, OnChildClickListener listener) {
        this.childrenList = childrenList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Child child = childrenList.get(position);
        holder.bind(child, listener);
    }

    @Override
    public int getItemCount() {
        return childrenList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        private TextView tvChildName;
        private TextView tvChildAge;
        private TextView tvSharingStatus;
        private ImageView ivSharingIndicator;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvChildAge = itemView.findViewById(R.id.tvChildAge);
            tvSharingStatus = itemView.findViewById(R.id.tvSharingStatus);
            ivSharingIndicator = itemView.findViewById(R.id.ivSharingIndicator);
        }

        public void bind(Child child, OnChildClickListener listener) {
            tvChildName.setText(child.getName());

            // Calculate age from DOB
            if (child.getDob() != null) {
                int age = calculateAge(child.getDob());
                tvChildAge.setText(age + " years old");
            } else {
                tvChildAge.setText("Age not set");
            }

            // Show sharing status
            if (child.isAnySharingEnabled()) {
                tvSharingStatus.setText("Shared with Provider");
                tvSharingStatus.setVisibility(View.VISIBLE);
                ivSharingIndicator.setVisibility(View.VISIBLE);
            } else {
                tvSharingStatus.setVisibility(View.GONE);
                ivSharingIndicator.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onChildClick(child));
        }

        private int calculateAge(java.util.Date dob) {
            java.util.Calendar dobCalendar = java.util.Calendar.getInstance();
            dobCalendar.setTime(dob);
            java.util.Calendar today = java.util.Calendar.getInstance();

            int age = today.get(java.util.Calendar.YEAR) - dobCalendar.get(java.util.Calendar.YEAR);

            if (today.get(java.util.Calendar.DAY_OF_YEAR) < dobCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age;
        }
    }
}