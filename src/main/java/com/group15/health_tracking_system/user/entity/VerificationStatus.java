package com.group15.health_tracking_system.user.entity;

public enum VerificationStatus {

    PENDING("Pending"),
    VERIFIED("Verified"),
    REJECTED("Rejected");

    private final String displayName;

    VerificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}