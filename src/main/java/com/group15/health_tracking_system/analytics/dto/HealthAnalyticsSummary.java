package com.group15.health_tracking_system.analytics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents the complete health-measurement analytics result for a patient
 * during a selected reporting period.
 *
 * <p>
 * The summary combines analytics for scalar health metrics with the
 * separately calculated blood pressure analytics. It also provides overall
 * counts that can later be displayed on the health analytics dashboard.
 * </p>
 *
 * <p>
 * This class is an analytics DTO only and is not persisted in the database.
 * </p>
 */
public class HealthAnalyticsSummary {

    private final LocalDate startDate;
    private final LocalDate endDate;

    private final List<MetricAnalyticsSummary> metricSummaries;
    private final BloodPressureAnalyticsSummary bloodPressureSummary;

    private final long totalReadingCount;
    private final int metricTypesWithData;
    private final boolean hasData;

    /**
     * Creates the complete health analytics summary.
     *
     * @param startDate beginning of the selected reporting period
     * @param endDate end of the selected reporting period
     * @param metricSummaries analytics for scalar health metrics that contain
     *                        data during the reporting period
     * @param bloodPressureSummary blood pressure analytics for the reporting
     *                             period, or null when no blood pressure data
     *                             exists
     */
    public HealthAnalyticsSummary(
            LocalDate startDate,
            LocalDate endDate,
            List<MetricAnalyticsSummary> metricSummaries,
            BloodPressureAnalyticsSummary bloodPressureSummary) {

        this.startDate = startDate;
        this.endDate = endDate;
        this.metricSummaries = List.copyOf(metricSummaries);
        this.bloodPressureSummary = bloodPressureSummary;

        long scalarReadingCount = this.metricSummaries.stream()
                .mapToLong(MetricAnalyticsSummary::getReadingCount)
                .sum();

        long bloodPressureReadingCount = bloodPressureSummary == null
                ? 0
                : bloodPressureSummary.getReadingCount();

        this.totalReadingCount =
                scalarReadingCount + bloodPressureReadingCount;

        this.metricTypesWithData =
                this.metricSummaries.size()
                + (bloodPressureSummary == null ? 0 : 1);

        this.hasData = totalReadingCount > 0;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<MetricAnalyticsSummary> getMetricSummaries() {
        return metricSummaries;
    }

    public BloodPressureAnalyticsSummary getBloodPressureSummary() {
        return bloodPressureSummary;
    }

    public long getTotalReadingCount() {
        return totalReadingCount;
    }

    public int getMetricTypesWithData() {
        return metricTypesWithData;
    }

    public boolean isHasData() {
        return hasData;
    }
}