package com.example.smart_air.modelClasses;

import com.google.firebase.Timestamp;

public class InventoryData {
    public String rescueName;
    public Long rescueAmount;
    public Timestamp rescuePurchase;
    public Timestamp rescueExpiry;

    public String controllerName;
    public Long controllerAmount;
    public Timestamp controllerPurchase;
    public Timestamp controllerExpiry;
}
