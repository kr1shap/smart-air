package com.example.smart_air.Repository;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.modelClasses.Notification;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class NotificationRepository {
    private final FirebaseFirestore db = FirebaseInitalizer.getDb();

    //LISTEN NOTIFS IN REALTIME
    public ListenerRegistration listenForNotifications(String parentUid,
                                                       com.google.firebase.firestore.EventListener<QuerySnapshot> listener) {
        return db.collection("notifications")
                .document(parentUid)
                .collection("notificationList")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }
    //get children document
    public Task<DocumentSnapshot> fetchNotifChildName(String childUid) {
        return db.collection("children")
                .document(childUid)
                .get();
    }
    //mark notif as read
    public Task<Void> markNotificationAsRead(String parentUid, String notifId) {
        return db.collection("notifications")
                .document(parentUid)
                .collection("notificationList")
                .document(notifId)
                .delete();
    }

    //create notif to add
    public Task<Void> createNotification(String parentUid, Notification notificationObj) {

        //generate reference for uid
        DocumentReference docRef = db.collection("notifications")
                .document(parentUid)
                .collection("notificationList")
                .document();

        Map<String, Object> notification = new HashMap<>();
        notification.put("notifUid", docRef.getId());
        notification.put("childUid", notificationObj.getChildUid());
        notification.put("notifType", notificationObj.getNotifType());
        notification.put("timestamp", Timestamp.now());
        notification.put("hasRead", false);
        //write it
        return docRef.set(notification);
    }


    //get unread notif
    public Task<QuerySnapshot> getUnreadNotifications(String parentUid) {
        return db.collection("notifications")
                .document(parentUid)
                .collection("notificationList")
                .get();
    }


}
