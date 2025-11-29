package com.example.smart_air.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smart_air.modelClasses.Child;

import java.util.List;

public class SharedChildViewModel extends ViewModel {
    private final MutableLiveData<List<Child>> allChildren = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentChild = new MutableLiveData<>(0);
    private final MutableLiveData<String> role = new MutableLiveData<>();

    public LiveData<List<Child>> getAllChildren() {
        return allChildren;
    }

    public void setChildren(List<Child> list) {
        allChildren.setValue(list);
    }

    public LiveData<Integer> getCurrentChild() {
        return currentChild;
    }

    public void setCurrentChild(int index) {
        currentChild.setValue(index);
    }

    public String getCurrentChildName() {
        List<Child> children = allChildren.getValue();
        Integer index = currentChild.getValue();

        if (children == null || children.isEmpty() || index == null || index < 0 || index >= children.size()) {
            return "";
        }
        return children.get(index).getName();
    }

    public LiveData<String> getCurrentRole(){
        return role;
    }

    public void setCurrentRole(String role){
        this.role.setValue(role);
    }
}
