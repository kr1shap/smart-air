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

    private final List<Child> childList;
    private final OnChildClickListener listener;

    public interface OnChildClickListener {
        void onChildClick(Child child);

        void onChildEdit(Child child);

        void onChildDelete(Child child);
    }

    public ChildAdapter(List<Child> childList, OnChildClickListener listener) {
        this.childList = childList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child, parent,
                false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Child child = childList.get(position);
        holder.bind(child, listener);
    }

    @Override
    public int getItemCount() {
        return childList.size();
    }

    public static class ChildViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvChildName;
        private final TextView tvChildDob;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tv_child_name);
            tvChildDob = itemView.findViewById(R.id.tv_child_dob);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Child child, OnChildClickListener listener) {

            // child label
            String name = child.getName();

            // fallback if name is missing
            if (name == null || name.trim().isEmpty()) {
                name = "Child " + child.getChildUid();
            }

            tvChildName.setText(
                    itemView.getContext().getString(R.string.child_label, child.getName())
            );

            // dob
            if (child.getDob() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy",
                        Locale.getDefault());
                tvChildDob.setText(sdf.format(child.getDob()));
            } else {
                tvChildDob.setText(R.string.dob_not_set);
            }

            // click listeners
            itemView.setOnClickListener(v -> listener.onChildClick(child));
            btnEdit.setOnClickListener(v -> listener.onChildEdit(child));
            btnDelete.setOnClickListener(v -> listener.onChildDelete(child));
        }

    }
}