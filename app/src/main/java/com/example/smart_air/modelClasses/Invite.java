package com.example.smart_air.modelClasses;

public class Invite {
    private String code;
    private String parentUid;
    private String targetRole; // provider/child
    private long expiresAt;
    private boolean used;
    private long createdAt;

    public Invite() {}

    public Invite(String code, String parentUid, String targetRole, long expiresAt) {
        this.code = code;
        this.parentUid = parentUid;
        this.targetRole = targetRole;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = System.currentTimeMillis();
    }

    // get and set
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getParentUid() { return parentUid; }
    public void setParentUid(String parentUid) { this.parentUid = parentUid; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // helper method to check if invite is expired
    public boolean isExpired() {
        return expiresAt < System.currentTimeMillis();
    }

    // hlper method to check if invite is valid
    public boolean isValid() {
        return !used && !isExpired();
    }

    // helper method to get remaining days until expiration
    public long getDaysUntilExpiry() {
        long diff;
        diff = expiresAt - System.currentTimeMillis();
        return diff / (1000 * 60 * 60 * 24);
    }
}