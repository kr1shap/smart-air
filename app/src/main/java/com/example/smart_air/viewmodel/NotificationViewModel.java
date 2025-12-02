package com.example.smart_air.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationViewModel extends ViewModel {

    private SharedChildViewModel childVM; //get childvm
    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>();
    private final NotificationRepository notifRepo = new NotificationRepository();
    private ListenerRegistration registration;
    private final Map<String, String> childNameCache = new HashMap<>();
    public LiveData<List<Notification>> getNotifications() { return notifications; }
    public LiveData<Integer> getUnreadCount() { return unreadCount; }

    public void setChildVM(SharedChildViewModel childVM) { this.childVM = childVM; }

    public void startListening(String uid) {
        if (registration != null) return; // so no reattachment twice

        registration = notifRepo.listenForNotifications(uid, (value, error) -> {
            if (error != null || value == null) return;
            List<Notification> list = new ArrayList<>();

            for (DocumentSnapshot doc : value.getDocuments()) {
                Notification n = doc.toObject(Notification.class);
                if (n != null) {
                    n.setNotifUid(doc.getId());
                    if(n.getChildName() == null || n.getChildName().trim().isEmpty()) n.setChildName("N/A");
                    else childNameCache.put(n.getChildUid(), n.getChildName());
                    list.add(n);
                }
            }

            sortByTime(list);

            notifications.setValue(list);
            unreadCount.setValue(list.size());
            observeChildNames();
        });
    }

    private String resolveName(String uid) {
        if (childVM == null) return "N/A";
        List<Child> children = childVM.getAllChildren().getValue();
        if (children == null) return "N/A";
        for (Child c : children) { if (c.getChildUid().equals(uid)) return c.getName(); }
        return "N/A";
    }

    private void observeChildNames() {
        if (childVM == null) return;
        childVM.getAllChildren().observeForever(children -> {
            List<Notification> list = notifications.getValue();
            if (list == null) return;

            for (Notification n : list) { n.setChildName(resolveName(n.getChildUid())); }

            sortByTime(list);
            notifications.setValue(new ArrayList<>(list));
        });
    }

    private void sortByTime(List<Notification> list) {
        list.sort((a, b) ->
                Long.compare(
                        Notification.convertTimestampToMillis(b.getTimestamp()),
                        Notification.convertTimestampToMillis(a.getTimestamp())
                )
        );
    }

    @Override
    public void onCleared() {
        if (registration != null) registration.remove();
    }
}
