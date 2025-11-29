package com.example.smart_air.modelClasses;

import java.io.Serializable;
import java.util.List;

public class User implements Serializable {
    private String uid;
    private String email;
    private String username;
    private String role; // "parent", "provider", "child"
    private List<String> parentUid; // provider and child linkage
    private List<String> childrenUid; //for parents
    private long createdAt;

    //constructor
    public User() {}
    public User(String uid, String email, String username, String role,
                List<String> parentUid, List<String> childrenUid) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.role = role;
        this.parentUid = parentUid;
        this.childrenUid = childrenUid;
        this.createdAt = System.currentTimeMillis();
    }

    // get and set
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getParentUid() { return parentUid; }
    public void setParentUid(List<String> parentUid) { this.parentUid = parentUid; }

    public List<String> getChildrenUid() { return childrenUid; }
    public void setChildrenUid(List<String> childrenUid) { this.childrenUid = childrenUid; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

}