package com.example.smart_air.modelClasses;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class Child {

    private String childUid;
    private String parentUid;
    private String name;
    private Date dob;   //util date
    private String extraNotes;
    private int personalBest;
    private Map<String, Boolean> sharing; //toggle sharing

    //new fields
    private Map<String, Boolean> weeklySchedule;        // weekly schedule
    private Map<String, Boolean> badges;
    private Map<String, Object> techniqueStats;
    private Map<String, Object> controllerStats;
    private Map<String, Integer> thresholds;
    private ArrayList<String> allowedProviderUids; //list of all provider uids


    // made for dropdown
    public Child(String childUid, String name){
        this.childUid = childUid;
        this.name = name;
    }


    public Child() {  initializeDefaults(); } //for safety, will add these when u auto convert!

    public Child(String name,
                 String childUid,
                 String parentUid,
                 Date dob,
                 String extraNotes,
                 int personalBest,
                 Map<String, Boolean> sharing) {

        this.name = name;
        this.childUid = childUid;
        this.parentUid = parentUid;
        this.dob = dob;
        this.extraNotes = extraNotes;
        this.personalBest = personalBest;
        this.sharing = sharing;

        initializeDefaults();

    }

    public Child(String name,
                 String childUid,
                 String parentUid,
                 Date dob,
                 String extraNotes,
                 int personalBest) {

        this.name = name;
        this.childUid = childUid;
        this.parentUid = parentUid;
        this.dob = dob;
        this.extraNotes = extraNotes;
        this.personalBest = personalBest;
        initializeDefaults();

    }

    //initalize defaults

    private void initializeDefaults() {
        this.weeklySchedule = initalizeDays();
        // badges
        this.badges = initalizeBadges();
        //toggles
        this.sharing = initalizeSharing();

        // techniqueStats
        this.techniqueStats = new HashMap<>();
        techniqueStats.put("currentStreak", 0);
        techniqueStats.put("lastSessionDate", ""); // YYYY-MM-DD
        techniqueStats.put("totalCompletedSessions", 0);
        techniqueStats.put("totalPerfectSessions", 0);

        //array list
        this.allowedProviderUids = new ArrayList<>();

        // thresholds
        this.thresholds = new HashMap<>();
        thresholds.put("quality_thresh", 0);
        thresholds.put("rescue_thresh", 0);
    }

    public static Map<String, Boolean> initalizeSharing() {
        Map<String, Boolean> sharing = new HashMap<>();
        sharing.put("rescue", false);
        sharing.put("controller", false);
        sharing.put("symptoms", false);
        sharing.put("triggers",false);
        sharing.put("pef",false);
        sharing.put("triage",false);
        sharing.put("charts",false);
        return sharing;
    }
    public static HashMap<String, Boolean> initalizeBadges() {
        HashMap<String, Boolean> badges = new HashMap<>();
        badges.put("techniqueBadge", false);
        badges.put("controllerBadge", false);
        badges.put("lowRescueBadge", false);
        return badges;
    }

    public static HashMap<String, Boolean> initalizeDays() {
        HashMap<String, Boolean> weeklySchedule = new HashMap<>();
        weeklySchedule.put("Monday", false);
        weeklySchedule.put("Tuesday", false);
        weeklySchedule.put("Wednesday", false);
        weeklySchedule.put("Thursday", false);
        weeklySchedule.put("Friday", false);
        weeklySchedule.put("Saturday", false);
        weeklySchedule.put("Sunday", false);
        return weeklySchedule;
    }

    // get and set !

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

    public String getName(){ return name; }

    public void setName(String name){this.name = name; }
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

    public Map<String, Boolean> getBadges() { return badges; }

    public void setBadges(Map<String, Boolean> badges) { this.badges = badges; }

    public Map<String, Object> getTechniqueStats() { return techniqueStats; }

    public void setTechniqueStats(Map<String, Object> techniqueStats) { this.techniqueStats = techniqueStats; }

    public Map<String, Object> getControllerStats() { return controllerStats; }

    public void setControllerStats(Map<String, Object> controllerStats) { this.controllerStats = controllerStats; }

    public Map<String, Integer> getThresholds() { return thresholds; }

    public void setThresholds(Map<String, Integer> thresholds) { this.thresholds = thresholds; }

    public Map<String, Boolean> getWeeklySchedule() { return weeklySchedule; }

    public void setWeeklySchedule(Map<String, Boolean> weeklySchedule) { this.weeklySchedule = weeklySchedule; }

    public ArrayList<String> getAllowedProviderUids() { return allowedProviderUids; }

    public void setAllowedProviderUids(ArrayList<String> allowedProviderUids) { this.allowedProviderUids = allowedProviderUids; }
}
