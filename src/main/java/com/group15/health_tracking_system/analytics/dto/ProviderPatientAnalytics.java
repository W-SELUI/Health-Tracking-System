package com.group15.health_tracking_system.analytics.dto;

import com.group15.health_tracking_system.user.entity.User;

/**
 * Represents the complete analytics result shown to an authorized
 * healthcare provider for one patient.
 *
 * The contained Health and Activity analytics are the same analytics DTOs
 * used by the patient's own dashboard. This class only groups those results
 * together with the patient identity for the Provider workflow.
 *
 * This DTO is not persisted in the database.
 */
public class ProviderPatientAnalytics {

    private final User patient;

    private final HealthAnalyticsSummary healthAnalytics;
    private final ActivityAnalyticsSummary activityAnalytics;

    private final boolean hasAnyData;

    public ProviderPatientAnalytics(
            User patient,
            HealthAnalyticsSummary healthAnalytics,
            ActivityAnalyticsSummary activityAnalytics) {

        this.patient = patient;
        this.healthAnalytics = healthAnalytics;
        this.activityAnalytics = activityAnalytics;

        this.hasAnyData =
                healthAnalytics.isHasData()
                || activityAnalytics.isHasData();
    }

    public User getPatient() {
        return patient;
    }

    public HealthAnalyticsSummary getHealthAnalytics() {
        return healthAnalytics;
    }

    public ActivityAnalyticsSummary getActivityAnalytics() {
        return activityAnalytics;
    }

    public boolean isHasAnyData() {
        return hasAnyData;
    }
}