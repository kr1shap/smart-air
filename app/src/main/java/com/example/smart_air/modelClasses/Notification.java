package com.example.smart_air.modelClasses;

import com.example.smart_air.modelClasses.enums.NotifType;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.text.SimpleDateFormat;

public class Notification {
    private String childUid;
    private boolean hasRead;
    private Timestamp timestamp;
    private NotifType notifType;
    private String childName;
    private String notifUid;

    public Notification() {}

    public Notification(String childUid,boolean hasRead, Timestamp timestamp, NotifType notifType) {
        this.childName="";
        this.childUid = childUid;
        this.hasRead = hasRead;
        this.timestamp = timestamp;
        this.notifType = notifType;
    }

    //w/childName
    public Notification(String childUid, boolean hasRead, Timestamp timestamp, NotifType notifType, String childName) {
        this.childUid = childUid;
        this.hasRead = hasRead;
        this.timestamp = timestamp;
        this.notifType = notifType;
        this.childName=childName;
    }

    public static long convertTimestampToMillis(Timestamp timestamp) {
        if (timestamp == null) { return 0; }
        return timestamp.toDate().getTime();
    }

    public static String convertToDate(Timestamp timestamp) {
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);

    }

    public static String getTitle(NotifType notifType) {
        switch (notifType) {
            case INVENTORY:
                return "Your inventory is running low/expired!";
            case TRIAGE:
                return "Your child has had a triage session!";
            case WORSE_DOSE:
                return "Your child has had a worse dose.";
            case RAPID_RESCUE:
                return "Your child is in rapid rescue.";
            case RED_ZONE:
                return "Your child is in the red zone.";
        }
        return "No notification title";
    }

    public static String getDescription(NotifType notifType) {
        switch (notifType) {
            case INVENTORY:
                return "Check your child's inventory, it's running low or has expired.";
            case TRIAGE:
                return "Your child has just completed a triage session. Check out their log!";
            case WORSE_DOSE:
                return "After taking a dosage, your child has reported feeling worse.";
            case RAPID_RESCUE:
                return "Your child has taken more than 3 rescue doses in less than 3 hours";
            case RED_ZONE:
                return "Your child's personal best today is in the red zone. Check out their log!";
        }
        return "No description";
    }

    public String getChildUid() {
        return childUid;
    }

    public void setChildUid(String childUid) {
        this.childUid = childUid;
    }

    public boolean isHasRead() {
        return hasRead;
    }

    public void setHasRead(boolean hasRead) {
        this.hasRead = hasRead;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public NotifType getNotifType() {
        return notifType;
    }

    public void setNotifType(NotifType notifType) {
        this.notifType = notifType;
    }

    public void setChildName(String s) {
        this.childName=s;
    }
    public String getChildName() { return childName; }

    public void setNotifUid(String s) { this.notifUid=s; }
    public String getNotifUid() { return notifUid; }

}
