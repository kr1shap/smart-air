package com.example.smart_air.modelClasses;

import java.util.Date;
import java.util.Map;

public class Child {

    private String childUid;
    private String parentUid;
    private Date dob;   //util date
    private String extraNotes;
    private int personalBest;
    private Map<String, Boolean> sharing; //toggle sharing
    private String name;

    // made for dropdown
    public Child(String childUid, String name){
        this.childUid = childUid;
        this.name = name;
    }
    public String getChildName(){
        return name;
    }


    public Child() {}

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
        this.sharing = sharing;
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
