package com.group15.health_tracking_system.analytics.service;

import com.group15.health_tracking_system.analytics.dto.BloodPressureAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.BloodPressureDataPoint;
import com.group15.health_tracking_system.analytics.dto.HealthAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.MetricAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.MetricDataPoint;
import com.group15.health_tracking_system.analytics.dto.TrendDirection;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.service.BloodPressureQueryService;
import com.group15.health_tracking_system.health.service.HealthRecordQueryService;
import com.group15.health_tracking_system.user.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates descriptive health analytics for an authenticated patient.
 *
 * <p>
 * Analytics are calculated on demand from existing health records and
 * blood-pressure records. No calculated analytics are persisted in the
 * database.
 * </p>
 *
 * <p>
 * The service provides descriptive statistics only. It does not classify
 * measurements as healthy or unhealthy and does not provide diagnosis or
 * treatment recommendations.
 * </p>
 */
@Service
public class HealthAnalyticsService {

    private static final int STATISTIC_SCALE = 3;
    private static final int PERCENTAGE_SCALE = 2;

    private static final RoundingMode ROUNDING_MODE =
            RoundingMode.HALF_UP;

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private final HealthRecordQueryService healthRecordQueryService;
    private final BloodPressureQueryService bloodPressureQueryService;

    public HealthAnalyticsService(
            HealthRecordQueryService healthRecordQueryService,
            BloodPressureQueryService bloodPressureQueryService) {

        this.healthRecordQueryService = healthRecordQueryService;
        this.bloodPressureQueryService = bloodPressureQueryService;
    }

    /**
     * Generates health analytics for the authenticated patient during the
     * selected reporting period.
     *
     * @param currentUser authenticated patient
     * @param startDate first date in the reporting period
     * @param endDate last date in the reporting period
     * @return complete health analytics summary
     */
    @Transactional(readOnly = true)
    public HealthAnalyticsSummary getPatientAnalytics(
            User currentUser,
            LocalDate startDate,
            LocalDate endDate) {

        validateReportingPeriod(startDate, endDate);

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(LocalTime.MAX);

        List<HealthRecord> scalarRecords =
                healthRecordQueryService
                        .getPatientHistoryByPeriod(
                                currentUser,
                                start,
                                end
                        );

        List<BloodPressureRecord> bloodPressureRecords =
                bloodPressureQueryService
                        .getPatientHistoryByPeriod(
                                currentUser,
                                start,
                                end
                        );

        List<MetricAnalyticsSummary> metricSummaries =
                buildMetricSummaries(scalarRecords);

        BloodPressureAnalyticsSummary bloodPressureSummary =
                bloodPressureRecords.isEmpty()
                        ? null
                        : buildBloodPressureSummary(
                                bloodPressureRecords
                        );

        return new HealthAnalyticsSummary(
                startDate,
                endDate,
                metricSummaries,
                bloodPressureSummary
        );
    }

    /**
     * Creates analytics summaries for every scalar metric that has data.
     */
    private List<MetricAnalyticsSummary> buildMetricSummaries(
            List<HealthRecord> records) {

        List<MetricAnalyticsSummary> summaries =
                new ArrayList<>();

        /*
         * Iterating through the enum guarantees a predictable order
         * regardless of the order returned by the database.
         */
        for (HealthMetricType metricType
                : HealthMetricType.values()) {

            List<HealthRecord> metricRecords =
                    records.stream()
                            .filter(record ->
                                    record.getMetricType()
                                            == metricType
                            )
                            .sorted(
                                    Comparator
                                            .comparing(
                                                    HealthRecord::getObservedAt
                                            )
                                            .thenComparing(
                                                    HealthRecord::getId,
                                                    Comparator.nullsLast(
                                                            Long::compareTo
                                                    )
                                            )
                            )
                            .toList();

            if (!metricRecords.isEmpty()) {
                summaries.add(
                        buildMetricSummary(
                                metricType,
                                metricRecords
                        )
                );
            }
        }

        return summaries;
    }

    /**
     * Calculates analytics for one scalar health metric.
     */
    private MetricAnalyticsSummary buildMetricSummary(
            HealthMetricType metricType,
            List<HealthRecord> records) {

        List<MetricDataPoint> dataPoints =
                records.stream()
                        .map(record ->
                                new MetricDataPoint(
                                        record.getObservedAt(),
                                        record.getMetricValue()
                                )
                        )
                        .toList();

        List<BigDecimal> values =
                records.stream()
                        .map(HealthRecord::getMetricValue)
                        .toList();

        BigDecimal earliestValue =
                values.getFirst();

        BigDecimal latestValue =
                values.getLast();

        BigDecimal averageValue =
                calculateAverage(values);

        BigDecimal medianValue =
                calculateMedian(values);

        BigDecimal minimumValue =
                values.stream()
                        .min(BigDecimal::compareTo)
                        .orElseThrow();

        BigDecimal maximumValue =
                values.stream()
                        .max(BigDecimal::compareTo)
                        .orElseThrow();

        long readingCount =
                values.size();

        BigDecimal absoluteChange = null;
        BigDecimal percentageChange = null;

        TrendDirection trendDirection =
                TrendDirection.INSUFFICIENT_DATA;

        if (readingCount >= 2) {

            absoluteChange =
                    latestValue.subtract(earliestValue);

            percentageChange =
                    calculatePercentageChange(
                            earliestValue,
                            latestValue
                    );

            trendDirection =
                    determineTrend(
                            earliestValue,
                            latestValue
                    );
        }

        return new MetricAnalyticsSummary(
                metricType,
                earliestValue,
                latestValue,
                averageValue,
                medianValue,
                minimumValue,
                maximumValue,
                readingCount,
                absoluteChange,
                percentageChange,
                trendDirection,
                dataPoints
        );
    }

    /**
     * Calculates blood-pressure analytics while preserving every
     * systolic/diastolic observation as a real pair.
     */
    private BloodPressureAnalyticsSummary buildBloodPressureSummary(
            List<BloodPressureRecord> records) {

        List<BloodPressureRecord> sortedRecords =
                records.stream()
                        .sorted(
                                Comparator
                                        .comparing(
                                                BloodPressureRecord::getObservedAt
                                        )
                                        .thenComparing(
                                                BloodPressureRecord::getId,
                                                Comparator.nullsLast(
                                                        Long::compareTo
                                                )
                                        )
                        )
                        .toList();

        List<BloodPressureDataPoint> dataPoints =
                sortedRecords.stream()
                        .map(record ->
                                new BloodPressureDataPoint(
                                        record.getObservedAt(),
                                        record.getSystolic(),
                                        record.getDiastolic()
                                )
                        )
                        .toList();

        List<BigDecimal> systolicValues =
                sortedRecords.stream()
                        .map(BloodPressureRecord::getSystolic)
                        .toList();

        List<BigDecimal> diastolicValues =
                sortedRecords.stream()
                        .map(BloodPressureRecord::getDiastolic)
                        .toList();

        BloodPressureDataPoint earliestReading =
                dataPoints.getFirst();

        BloodPressureDataPoint latestReading =
                dataPoints.getLast();

        long readingCount =
                dataPoints.size();

        BigDecimal systolicAbsoluteChange = null;
        BigDecimal systolicPercentageChange = null;

        BigDecimal diastolicAbsoluteChange = null;
        BigDecimal diastolicPercentageChange = null;

        TrendDirection systolicTrendDirection =
                TrendDirection.INSUFFICIENT_DATA;

        TrendDirection diastolicTrendDirection =
                TrendDirection.INSUFFICIENT_DATA;

        if (readingCount >= 2) {

            systolicAbsoluteChange =
                    latestReading.getSystolic()
                            .subtract(
                                    earliestReading.getSystolic()
                            );

            systolicPercentageChange =
                    calculatePercentageChange(
                            earliestReading.getSystolic(),
                            latestReading.getSystolic()
                    );

            systolicTrendDirection =
                    determineTrend(
                            earliestReading.getSystolic(),
                            latestReading.getSystolic()
                    );

            diastolicAbsoluteChange =
                    latestReading.getDiastolic()
                            .subtract(
                                    earliestReading.getDiastolic()
                            );

            diastolicPercentageChange =
                    calculatePercentageChange(
                            earliestReading.getDiastolic(),
                            latestReading.getDiastolic()
                    );

            diastolicTrendDirection =
                    determineTrend(
                            earliestReading.getDiastolic(),
                            latestReading.getDiastolic()
                    );
        }

        return new BloodPressureAnalyticsSummary(
                earliestReading,
                latestReading,

                calculateAverage(systolicValues),
                calculateMedian(systolicValues),
                minimum(systolicValues),
                maximum(systolicValues),

                calculateAverage(diastolicValues),
                calculateMedian(diastolicValues),
                minimum(diastolicValues),
                maximum(diastolicValues),

                systolicAbsoluteChange,
                systolicPercentageChange,
                systolicTrendDirection,

                diastolicAbsoluteChange,
                diastolicPercentageChange,
                diastolicTrendDirection,

                readingCount,
                dataPoints
        );
    }

    /**
     * Calculates the arithmetic mean of a non-empty list.
     */
    private BigDecimal calculateAverage(
            List<BigDecimal> values) {

        BigDecimal total =
                values.stream()
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return total.divide(
                BigDecimal.valueOf(values.size()),
                STATISTIC_SCALE,
                ROUNDING_MODE
        );
    }

    /**
     * Calculates the median of a non-empty list.
     */
    private BigDecimal calculateMedian(
            List<BigDecimal> values) {

        List<BigDecimal> sortedValues =
                values.stream()
                        .sorted()
                        .toList();

        int size =
                sortedValues.size();

        int middle =
                size / 2;

        if (size % 2 != 0) {
            return sortedValues.get(middle);
        }

        return sortedValues
                .get(middle - 1)
                .add(sortedValues.get(middle))
                .divide(
                        BigDecimal.valueOf(2),
                        STATISTIC_SCALE,
                        ROUNDING_MODE
                );
    }

    private BigDecimal minimum(
            List<BigDecimal> values) {

        return values.stream()
                .min(BigDecimal::compareTo)
                .orElseThrow();
    }

    private BigDecimal maximum(
            List<BigDecimal> values) {

        return values.stream()
                .max(BigDecimal::compareTo)
                .orElseThrow();
    }

    /**
     * Calculates percentage change from the earliest value to the latest.
     *
     * <p>
     * Percentage change is unavailable when the earliest value is zero,
     * because division by zero is undefined.
     * </p>
     */
    private BigDecimal calculatePercentageChange(
            BigDecimal earliestValue,
            BigDecimal latestValue) {

        if (earliestValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal difference =
                latestValue.subtract(earliestValue);

        return difference
                .multiply(ONE_HUNDRED)
                .divide(
                        earliestValue,
                        PERCENTAGE_SCALE,
                        ROUNDING_MODE
                );
    }

    /**
     * Describes only the numerical earliest-to-latest direction.
     */
    private TrendDirection determineTrend(
            BigDecimal earliestValue,
            BigDecimal latestValue) {

        int comparison =
                latestValue.compareTo(earliestValue);

        if (comparison > 0) {
            return TrendDirection.INCREASED;
        }

        if (comparison < 0) {
            return TrendDirection.DECREASED;
        }

        return TrendDirection.NO_CHANGE;
    }

    /**
     * Validates the reporting period used for analytics.
     */
    private void validateReportingPeriod(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must not be after end date"
            );
        }

        if (endDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "End date must not be in the future"
            );
        }
    }
}