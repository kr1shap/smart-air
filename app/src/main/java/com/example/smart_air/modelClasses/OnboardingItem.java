package com.example.smart_air.modelClasses;

public class OnboardingItem {
    private final int imageRes;
    private final String title;
    private final String description;
    private final String backgroundColor;

    public OnboardingItem(int imageRes, String title, String description, String backgroundColor) {
        this.imageRes = imageRes;
        this.title = title;
        this.description = description;
        this.backgroundColor = backgroundColor;
    }

    public int getImageRes() {
        return imageRes;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }
}