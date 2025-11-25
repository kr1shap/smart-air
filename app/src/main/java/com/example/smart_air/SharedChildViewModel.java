package com.example.smart_air;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SharedChildViewModel extends ViewModel {
    private final MutableLiveData<List<String>> allChildren = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentChild = new MutableLiveData<>(0);

    public LiveData<List<String>> getAllChildren() {
        return allChildren;
    }

    public void setChildren(List<String> list) {
        allChildren.setValue(list);
    }

    public LiveData<Integer> getCurrentChild() {
        return currentChild;
    }

    public void setCurrentChild(int index) {
        currentChild.setValue(index);
    }
}
