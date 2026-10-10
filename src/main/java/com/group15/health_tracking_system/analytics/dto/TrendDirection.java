package com.group15.health_tracking_system.analytics.dto;

/**
 * Represents the basic descriptive direction of change in a health metric
 * over a selected reporting period.
 *
 * <p>
 * This enum does not provide medical interpretation. It only describes
 * whether the latest recorded value is greater than, less than, or equal
 * to the earliest recorded value in the selected period.
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