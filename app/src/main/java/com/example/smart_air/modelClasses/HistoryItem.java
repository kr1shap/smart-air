package com.example.smart_air.modelClasses;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryItem {
    public enum typeOfCard {
        both,
        childOnly,
        parentOnly
    }
    public typeOfCard cardType;
    public String date;
    public Date accDate;
    public int pef;
    public String pefText;
    public boolean nightChild;
    public boolean nightParent;
    public int activityChild;
    public int activityParent;
    public int coughingChild;
    public int coughingParent;
    public List<String> triggers;

    public String activityStatus;
    public String coughingStatus;
    public String nightStatus;
    public String nightChildText;
    public String nightParentText;
    public String zone;
    public HistoryItem (String date, boolean nightChild, boolean nightParent, int activityChild, int activityParent, int coughingChild, int coughingParent, List<String> childTriggers, List<String> parentTriggers, int pef, String zone, Date accDate){
        this.pef = pef;
        if(pef != -5){
            this.pefText = Integer.toString(pef) + "L/min";
        }
        else{
            this.pefText = "NOT ENTERED";
        }


        this.date = date;
        this.nightChild = nightChild;
        this.nightParent = nightParent;
        this.activityChild = activityChild*10;
        this.activityParent = activityParent*10;
        this.coughingChild = coughingChild*33;
        this.coughingParent = coughingParent*33;

        int coughingAvg = 0;
        int activityAvg = 0;

        if(coughingChild == -5 && coughingParent != -5){
            cardType = typeOfCard.parentOnly;
            this.activityChild = 0;
            coughingAvg = this.coughingParent / 33;
            activityAvg = this.activityParent / 10;
        }
        else if(coughingChild != -5 && coughingParent == -5){
            cardType = typeOfCard.childOnly;
            this.activityParent = 0;
            coughingAvg = this.coughingChild / 33;
            activityAvg = this.activityChild / 10;
        }
        else{
            cardType = typeOfCard.both;
            coughingAvg = (int) Math.round((this.coughingChild + this.coughingParent) / 66.0) ;
            activityAvg = (int) Math.round((this.activityChild + this.activityParent) / 20.0) ;
        }

        triggers = new ArrayList<String>();
        for(int i = 0; i < childTriggers.size(); i++){
            if(!(triggers.contains(childTriggers.get(i)))){
                triggers.add(childTriggers.get(i));
            }
        }
        for(int i = 0; i < parentTriggers.size(); i++){
            if(!(triggers.contains(parentTriggers.get(i)))){
                triggers.add(parentTriggers.get(i));
            }
        }

        switch(activityAvg){
            case 0:
            case 1:
            case 2:
                activityStatus = "NONE";
                break;
            case 3:
            case 4:
            case 5:
                activityStatus = "OKAY";
                break;
            case 6:
            case 7:
            case 8:
                activityStatus = "MINIMAL";
                break;
            case 9:
            case 10:
                activityStatus = "LIMITED";
                break;
        }

        switch(coughingAvg){
            case 0:
                this.coughingStatus = "NONE";
                break;
            case 1:
                this.coughingStatus = "WHEEZING";
                break;
            case 2:
                this.coughingStatus = "COUGHING";
                break;
            case 3:
                this.coughingStatus = "SEVERE";
                break;
        }

        if(cardType == typeOfCard.childOnly){
            nightStatus = nightChild ? "YES" : "NO";
        }
        else if(cardType == typeOfCard.parentOnly){
            nightStatus = nightParent ? "YES" : "NO";
        }
        else if(nightChild && nightParent){
            nightStatus = "YES";
        }
        else if(!nightChild && !nightParent){
            nightStatus = "NO";
        }
        else{
            nightStatus = "SOMEWHAT";
        }

        if(nightChild){
            nightChildText = "YES";
        }
        else{
            nightChildText = "NO";
        }

        if(nightParent){
            nightParentText = "YES";
        }
        else{
            nightParentText = "NO";
        }

        this.zone = zone;

        this.accDate = accDate;

    }

    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HistoryItem that = (HistoryItem) o;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        return true;
    }
}
