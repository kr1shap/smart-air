package com.example.smart_air.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.smart_air.R;
import com.example.smart_air.modelClasses.Child;


import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {


    private final List<Child> childrenList;
    private final OnChildClickListener listener;


    // for handling clicks
    public interface OnChildClickListener {
        void onChildEdit(Child child);
        void onChildDelete(Child child);
    }


    // Constructor
    public ChildAdapter(List<Child> childrenList, OnChildClickListener listener) {
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


    // ViewHolder class
    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName;
        TextView tvChildDob;
        ImageButton btnEdit;
        ImageButton btnDelete;


        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tv_child_name);
            tvChildDob = itemView.findViewById(R.id.tv_child_dob);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }


        public void bind(Child child, OnChildClickListener listener) {
            // Set child name
            tvChildName.setText(child.getName());


            // Format and set date of birth
            if (child.getDob() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dobText = "DOB: " + sdf.format(child.getDob());


                // Calculate age
                int age = calculateAge(child.getDob());
                dobText += " (Age: " + age + ")";


                tvChildDob.setText(dobText);
            } else {
                tvChildDob.setText("DOB: Not set");
            }


            // Edit button click
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChildEdit(child);
                }
            });


            // Delete button click
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChildDelete(child);
                }
            });
        }


        private int calculateAge(java.util.Date dob) {
            java.util.Calendar dobCal = java.util.Calendar.getInstance();
            dobCal.setTime(dob);


            java.util.Calendar today = java.util.Calendar.getInstance();


            int age = today.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR);


            // if birthday hasn't occurred yet this year
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < dobCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--;
            }


            return age;
        }
    }
}

