package com.group15.health_tracking_system.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contains the calculated analytics for blood pressure observations
 * during a selected reporting period.
 *
 * <p>
 * Blood pressure observations are always kept as paired systolic and
 * diastolic readings. Statistical calculations for systolic and
 * diastolic values are stored separately so that artificial blood
 * pressure pairs are never created.
 * </p>
 *
 * <p>
 * This class is an analytics DTO only and is not persisted in the database.
 * </p>
 */
public class BloodPressureAnalyticsSummary {

    private final BloodPressureDataPoint earliestReading;
    private final BloodPressureDataPoint latestReading;

    private final BigDecimal averageSystolic;
    private final BigDecimal medianSystolic;
    private final BigDecimal minimumSystolic;
    private final BigDecimal maximumSystolic;

    private final BigDecimal averageDiastolic;
    private final BigDecimal medianDiastolic;
    private final BigDecimal minimumDiastolic;
    private final BigDecimal maximumDiastolic;

    private final BigDecimal systolicAbsoluteChange;
    private final BigDecimal systolicPercentageChange;
    private final TrendDirection systolicTrendDirection;

    private final BigDecimal diastolicAbsoluteChange;
    private final BigDecimal diastolicPercentageChange;
    private final TrendDirection diastolicTrendDirection;

    private final long readingCount;

    private final List<BloodPressureDataPoint> dataPoints;

    /**
     * Creates a blood pressure analytics summary.
     *
     * @param earliestReading earliest actual paired reading in the period
     * @param latestReading latest actual paired reading in the period
     * @param averageSystolic arithmetic mean of systolic values
     * @param medianSystolic median of systolic values
     * @param minimumSystolic minimum systolic value
     * @param maximumSystolic maximum systolic value
     * @param averageDiastolic arithmetic mean of diastolic values
     * @param medianDiastolic median of diastolic values
     * @param minimumDiastolic minimum diastolic value
     * @param maximumDiastolic maximum diastolic value
     * @param systolicAbsoluteChange latest systolic minus earliest systolic
     * @param systolicPercentageChange percentage change in systolic value;
     *                                 null when it cannot be calculated
     * @param systolicTrendDirection descriptive systolic direction
     * @param diastolicAbsoluteChange latest diastolic minus earliest diastolic
     * @param diastolicPercentageChange percentage change in diastolic value;
     *                                  null when it cannot be calculated
     * @param diastolicTrendDirection descriptive diastolic direction
     * @param readingCount number of paired blood pressure readings
     * @param dataPoints exact chronological paired readings used for charts
     */
    public BloodPressureAnalyticsSummary(
            BloodPressureDataPoint earliestReading,
            BloodPressureDataPoint latestReading,
            BigDecimal averageSystolic,
            BigDecimal medianSystolic,
            BigDecimal minimumSystolic,
            BigDecimal maximumSystolic,
            BigDecimal averageDiastolic,
            BigDecimal medianDiastolic,
            BigDecimal minimumDiastolic,
            BigDecimal maximumDiastolic,
            BigDecimal systolicAbsoluteChange,
            BigDecimal systolicPercentageChange,
            TrendDirection systolicTrendDirection,
            BigDecimal diastolicAbsoluteChange,
            BigDecimal diastolicPercentageChange,
            TrendDirection diastolicTrendDirection,
            long readingCount,
            List<BloodPressureDataPoint> dataPoints) {

        this.earliestReading = earliestReading;
        this.latestReading = latestReading;

        this.averageSystolic = averageSystolic;
        this.medianSystolic = medianSystolic;
        this.minimumSystolic = minimumSystolic;
        this.maximumSystolic = maximumSystolic;

        this.averageDiastolic = averageDiastolic;
        this.medianDiastolic = medianDiastolic;
        this.minimumDiastolic = minimumDiastolic;
        this.maximumDiastolic = maximumDiastolic;

        this.systolicAbsoluteChange = systolicAbsoluteChange;
        this.systolicPercentageChange = systolicPercentageChange;
        this.systolicTrendDirection = systolicTrendDirection;

        this.diastolicAbsoluteChange = diastolicAbsoluteChange;
        this.diastolicPercentageChange = diastolicPercentageChange;
        this.diastolicTrendDirection = diastolicTrendDirection;

        this.readingCount = readingCount;
        this.dataPoints = List.copyOf(dataPoints);
    }

    public BloodPressureDataPoint getEarliestReading() {
        return earliestReading;
    }

    public BloodPressureDataPoint getLatestReading() {
        return latestReading;
    }

    public BigDecimal getAverageSystolic() {
        return averageSystolic;
    }

    public BigDecimal getMedianSystolic() {
        return medianSystolic;
    }

    public BigDecimal getMinimumSystolic() {
        return minimumSystolic;
    }

    public BigDecimal getMaximumSystolic() {
        return maximumSystolic;
    }

    public BigDecimal getAverageDiastolic() {
        return averageDiastolic;
    }

    public BigDecimal getMedianDiastolic() {
        return medianDiastolic;
    }

    public BigDecimal getMinimumDiastolic() {
        return minimumDiastolic;
    }

    public BigDecimal getMaximumDiastolic() {
        return maximumDiastolic;
    }

    public BigDecimal getSystolicAbsoluteChange() {
        return systolicAbsoluteChange;
    }

    public BigDecimal getSystolicPercentageChange() {
        return systolicPercentageChange;
    }

    public TrendDirection getSystolicTrendDirection() {
        return systolicTrendDirection;
    }

    public BigDecimal getDiastolicAbsoluteChange() {
        return diastolicAbsoluteChange;
    }

    public BigDecimal getDiastolicPercentageChange() {
        return diastolicPercentageChange;
    }

    public TrendDirection getDiastolicTrendDirection() {
        return diastolicTrendDirection;
    }

    public long getReadingCount() {
        return readingCount;
    }

    public List<BloodPressureDataPoint> getDataPoints() {
        return dataPoints;
    }
}