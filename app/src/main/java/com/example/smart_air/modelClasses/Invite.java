package com.example.smart_air.modelClasses;

public class Invite {
    private String code;
    private String parentUid;
    private String targetRole; // provider/child
    private long expiresAt;
    private boolean used;
    private long createdAt;
    private String usedByUid;
    private String usedByEmail;

    public Invite() {}

    public Invite(String code, String parentUid, String targetRole, long expiresAt) {
        this.code = code;
        this.parentUid = parentUid;
        this.targetRole = targetRole;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = System.currentTimeMillis();
        this.usedByUid = null;
        this.usedByEmail = null;
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

    public String getUsedByUid() { return usedByUid; }
    public void setUsedByUid(String usedByUid) { this.usedByUid = usedByUid; }

    public String getUsedByEmail() { return usedByEmail; }
    public void setUsedByEmail(String usedByEmail) { this.usedByEmail = usedByEmail; }
}