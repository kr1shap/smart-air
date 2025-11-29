package com.example.smart_air;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.modelClasses.enums.NotifType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ExpiryCheck extends BroadcastReceiver {

    int WARNING_DAYS = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ExpiryCheck", "Alarm fired – running expiry check");
        runexpirycheck();
    }
    /*
    checks children for any expiry dates
     */
    public void runexpirycheck() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d("ExpiryCheck", "No children found");
                        return;
                    }
                    for (DocumentSnapshot childDoc : snap.getDocuments()) {
                        String childUid = childDoc.getId();
                        String childName = childDoc.getString("name");
                        if (childName == null) {
                            childName = "Child";
                        }
                        checkexpirytimestamp(childUid, childName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ExpiryCheck", "Error loading children collection", e);
                });
    }

    /*
    gets expired timestamp from each rescue and controller document
     */
    public void checkexpirytimestamp(String childUid, String childName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String[] types = new String[] {"controller", "rescue"};
        for (String type : types) {
            db.collection("children")
                    .document(childUid)
                    .collection("inventory")
                    .document(type)
                    .get()
                    .addOnSuccessListener(invDoc -> {
                        if (invDoc.exists()==false) {
                            Log.d("ExpiryCheck", "No " + type + " inventory for " + childUid);
                            return;
                        }
                        Timestamp expiryTs = invDoc.getTimestamp("expiryDate");
                        if (expiryTs == null) {
                            Log.d("ExpiryCheck", "No expiryDate on " + type + " for " + childUid);
                            return;
                        }
                        Date expiryDate = expiryTs.toDate();
                        if (isexpired(expiryDate)) {
                            Log.d("ExpiryCheck", "Inventory " + type + " is expired/expiring soon for child " + childUid);
                            sendinventoryAlert(childUid, childName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ExpiryCheck", "Error loading inventory " + type + " for " + childUid, e);
                    });
        }
    }
    /*
    checks if expiry date has passed
     */
    public boolean isexpired(Date expiryDate) {
        // get present date
        Calendar nowCal = Calendar.getInstance();
        Date now = nowCal.getTime();
        // check if expiry date has passed
        if (expiryDate.before(now)) {
            return true;
        }
        nowCal.add(Calendar.DAY_OF_YEAR, WARNING_DAYS);
        Date warnLimit = nowCal.getTime();
        return expiryDate.before(warnLimit);
    }

   /*
   send notification of expired medicine
    */
    public void sendinventoryAlert(String childUid, String childName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()==false) {
                        Log.e("ExpiryCheck", "Child user doc missing in users collection: " + childUid);
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    List<String> parentUids = (List<String>) doc.get("parentUid");
                    if (parentUids == null || parentUids.isEmpty()) {
                        Log.e("ExpiryCheck", "No parentUid array found for child " + childUid);
                        return;
                    }
                    NotificationRepository notifRepo = new NotificationRepository();
                    for (String pUid : parentUids) {
                        if (pUid == null){
                            continue;
                        }
                        Notification notif = new Notification(childUid, false, Timestamp.now(), NotifType.INVENTORY, childName);
                        notifRepo.createNotification(pUid, notif)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("NotificationRepo", "Expiry notification created for parent " + pUid))
                                .addOnFailureListener(e ->
                                        Log.e("NotificationRepo", "Failed to notify parent " + pUid, e));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ExpiryCheck", "Failed to load child user doc " + childUid, e)
                );
    }
}
