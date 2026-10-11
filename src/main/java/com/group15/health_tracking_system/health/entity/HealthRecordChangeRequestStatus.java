package com.group15.health_tracking_system.health.entity;

/**
 * Represents the lifecycle state of a Provider-proposed
 * health-record change.
 */
public enum HealthRecordChangeRequestStatus {

    /**
     * Submitted by the Provider and awaiting Patient review.
     */
    PENDING("Pending"),

    /**
     * Approved by the Patient and successfully applied to the
     * permanent Health Records data.
     */
    APPROVED("Approved"),

    /**
     * Explicitly rejected by the Patient.
     */
    REJECTED("Rejected"),

    /**
     * Invalidated or withdrawn before Patient approval, for example
     * because Provider access was revoked.
     */
    CANCELLED("Cancelled"),

    /**
     * The underlying record changed after the proposal was created,
     * so applying it could overwrite newer information.
     */
    STALE("Stale");

    private final String displayName;

    HealthRecordChangeRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}