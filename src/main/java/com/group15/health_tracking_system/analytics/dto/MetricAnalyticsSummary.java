package com.group15.health_tracking_system.analytics.dto;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Contains the calculated analytics for one scalar health metric
 * during a selected reporting period.
 *
 * <p>
 * This DTO stores descriptive statistics and the chronological
 * measurement points required by the analytics dashboard.
 * It is not persisted in the database.
 * </p>
 */
public class MetricAnalyticsSummary {

    private final HealthMetricType metricType;

    private final BigDecimal earliestValue;
    private final BigDecimal latestValue;

    private final BigDecimal averageValue;
    private final BigDecimal medianValue;
    private final BigDecimal minimumValue;
    private final BigDecimal maximumValue;

    private final long readingCount;

    private final BigDecimal absoluteChange;
    private final BigDecimal percentageChange;

    private final TrendDirection trendDirection;

    private final List<MetricDataPoint> dataPoints;

    /**
     * Creates an analytics summary for one health metric.
     *
     * @param metricType type of health metric being analysed
     * @param earliestValue earliest recorded value in the reporting period
     * @param latestValue latest recorded value in the reporting period
     * @param averageValue arithmetic mean of the recorded values
     * @param medianValue median of the recorded values
     * @param minimumValue lowest recorded value
     * @param maximumValue highest recorded value
     * @param readingCount number of readings in the reporting period
     * @param absoluteChange latest value minus earliest value
     * @param percentageChange percentage change from earliest to latest;
     *                         null when it cannot be calculated
     * @param trendDirection descriptive earliest-to-latest direction
     * @param dataPoints exact chronological measurements used for charts
     */
    public MetricAnalyticsSummary(
            HealthMetricType metricType,
            BigDecimal earliestValue,
            BigDecimal latestValue,
            BigDecimal averageValue,
            BigDecimal medianValue,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            long readingCount,
            BigDecimal absoluteChange,
            BigDecimal percentageChange,
            TrendDirection trendDirection,
            List<MetricDataPoint> dataPoints) {

        this.metricType = metricType;
        this.earliestValue = earliestValue;
        this.latestValue = latestValue;
        this.averageValue = averageValue;
        this.medianValue = medianValue;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.readingCount = readingCount;
        this.absoluteChange = absoluteChange;
        this.percentageChange = percentageChange;
        this.trendDirection = trendDirection;
        this.dataPoints = List.copyOf(dataPoints);
    }

    public HealthMetricType getMetricType() {
        return metricType;
    }

    public BigDecimal getEarliestValue() {
        return earliestValue;
    }

    public BigDecimal getLatestValue() {
        return latestValue;
    }

    public BigDecimal getAverageValue() {
        return averageValue;
    }

    public BigDecimal getMedianValue() {
        return medianValue;
    }

    public BigDecimal getMinimumValue() {
        return minimumValue;
    }

    public BigDecimal getMaximumValue() {
        return maximumValue;
    }

    public long getReadingCount() {
        return readingCount;
    }

    public BigDecimal getAbsoluteChange() {
        return absoluteChange;
    }

    public BigDecimal getPercentageChange() {
        return percentageChange;
    }

    public TrendDirection getTrendDirection() {
        return trendDirection;
    }

    public List<MetricDataPoint> getDataPoints() {
        return dataPoints;
    }
}