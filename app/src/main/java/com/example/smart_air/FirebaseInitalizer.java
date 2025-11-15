package com.example.smart_air;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseInitalizer extends Application {
    private static FirebaseFirestore db;
    private static FirebaseAuth mAuth;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // initialize once for the whole app
    }

    public static FirebaseFirestore getDb() {
        return db;
    }

    public static FirebaseAuth getAuth() {
        return mAuth;
    }
}
