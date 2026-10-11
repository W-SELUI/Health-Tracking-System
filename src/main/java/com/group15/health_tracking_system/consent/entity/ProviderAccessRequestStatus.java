package com.group15.health_tracking_system.consent.entity;

/**
 * Represents the lifecycle state of a healthcare provider's request
 * for access to a patient's health data and analytics.
 *
 * A ProviderAccessRequest represents a request for permission.
 * It is intentionally separate from Consent, which represents
 * permission that has actually been granted.
 */
public enum ProviderAccessRequestStatus {

    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String displayName;

    ProviderAccessRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns a user-friendly name for presentation in the UI.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}