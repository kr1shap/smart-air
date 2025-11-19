package com.example.smart_air.modelClasses;

import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class Child {

    private String childUid;
    private String parentUid;
    private String name;
    private Date dob;
    private String extraNotes;
    private int personalBest;
    private Map<String, Boolean> sharing;

    // Sharing keys constants
    public static final String SHARE_RESCUE_LOGS = "rescue";
    public static final String SHARE_CONTROLLER_ADHERENCE = "controller_as";
    public static final String SHARE_SYMPTOMS = "symptoms";
    public static final String SHARE_TRIGGERS = "triggers";
    public static final String SHARE_PEAK_FLOW = "pef";
    public static final String SHARE_TRIAGE_INCIDENTS = "triage";
    public static final String SHARE_SUMMARY_CHARTS = "charts";

    // Empty constructor required for Firestore
    public Child() {
        this.sharing = getDefaultSharingSettings();
    }

    public Child(String childUid,
                 String parentUid,
                 Date dob,
                 String extraNotes,
                 int personalBest,
                 Map<String, Boolean> sharing) {
        this.childUid = childUid;
        this.parentUid = parentUid;
        this.dob = dob;
        this.extraNotes = extraNotes;
        this.personalBest = personalBest;
        this.sharing = sharing != null ? sharing : getDefaultSharingSettings();
    }

    // Constructor with name
    public Child(String childUid,
                 String parentUid,
                 String name,
                 Date dob,
                 String extraNotes,
                 int personalBest,
                 Map<String, Boolean> sharing) {
        this.childUid = childUid;
        this.parentUid = parentUid;
        this.name = name;
        this.dob = dob;
        this.extraNotes = extraNotes;
        this.personalBest = personalBest;
        this.sharing = sharing != null ? sharing : getDefaultSharingSettings();
    }

    // Helper method to get default sharing settings (all false by default)
    public static Map<String, Boolean> getDefaultSharingSettings() {
        Map<String, Boolean> defaultSharing = new HashMap<>();
        defaultSharing.put(SHARE_RESCUE_LOGS, false);
        defaultSharing.put(SHARE_CONTROLLER_ADHERENCE, false);
        defaultSharing.put(SHARE_SYMPTOMS, false);
        defaultSharing.put(SHARE_TRIGGERS, false);
        defaultSharing.put(SHARE_PEAK_FLOW, false);
        defaultSharing.put(SHARE_TRIAGE_INCIDENTS, false);
        defaultSharing.put(SHARE_SUMMARY_CHARTS, false);
        return defaultSharing;
    }

    // Check if any data is being shared
    public boolean isAnySharingEnabled() {
        if (sharing == null) return false;
        for (Boolean value : sharing.values()) {
            if (value != null && value) return true;
        }
        return false;
    }

    // Getters and Setters
    public String getChildUid() {
        return childUid;
    }

    public void setChildUid(String childUid) {
        this.childUid = childUid;
    }

    public String getParentUid() {
        return parentUid;
    }

    public void setParentUid(String parentUid) {
        this.parentUid = parentUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public String getExtraNotes() {
        return extraNotes;
    }

    public void setExtraNotes(String extraNotes) {
        this.extraNotes = extraNotes;
    }

    public int getPersonalBest() {
        return personalBest;
    }

    public void setPersonalBest(int personalBest) {
        this.personalBest = personalBest;
    }

    public Map<String, Boolean> getSharing() {
        return sharing;
    }

    public void setSharing(Map<String, Boolean> sharing) {
        this.sharing = sharing;
    }
}