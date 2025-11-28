package com.example.smart_air.modelClasses;
public class BadgeData {
    private boolean controllerBadge;
    private boolean techniqueBadge;
    private boolean lowRescueBadge;
    private int techniqueStreak;
    private int controllerStreak;

    public BadgeData(boolean controllerBadge, boolean techniqueBadge, boolean rescueBadge,
                     int techniqueStreak, int controllerStreak) {
        this.controllerBadge = controllerBadge;
        this.techniqueBadge = techniqueBadge;
        this.lowRescueBadge = rescueBadge;
        this.techniqueStreak = techniqueStreak;
        this.controllerStreak = controllerStreak;
    }

    //get and set methods
    public boolean isControllerBadge() { return controllerBadge; }

    public void setControllerBadge(boolean controllerBadge) { this.controllerBadge = controllerBadge; }

    public boolean isTechniqueBadge() { return techniqueBadge; }

    public void setTechniqueBadge(boolean techniqueBadge) { this.techniqueBadge = techniqueBadge; }

    public boolean isLowRescueBadge() { return lowRescueBadge; }

    public void setLowRescueBadge(boolean rescueBadge) { this.lowRescueBadge = rescueBadge; }

    public int getTechniqueStreak() { return techniqueStreak; }

    public void setTechniqueStreak(int techniqueStreak) { this.techniqueStreak = techniqueStreak; }

    public int getControllerStreak() { return controllerStreak; }

    public void setControllerStreak(int controllerStreak) { this.controllerStreak = controllerStreak; }

}