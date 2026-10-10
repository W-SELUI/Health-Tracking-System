package com.group15.health_tracking_system.analytics.dto;

/**
 * Represents the basic descriptive direction of change in a health metric
 * during a selected reporting period.
 *
 * <p>
 * The direction compares the earliest recorded value with the latest
 * recorded value. It does not provide medical interpretation or indicate
 * whether the change is beneficial or harmful.
 * </p>
 */
public enum TrendDirection {

    INCREASED("Increased"),
    DECREASED("Decreased"),
    NO_CHANGE("No Change"),
    INSUFFICIENT_DATA("Insufficient Data");

    private final String displayName;

    TrendDirection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}