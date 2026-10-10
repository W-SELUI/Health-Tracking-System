package com.group15.health_tracking_system.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.analytics.dto.BloodPressureAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.BloodPressureDataPoint;
import com.group15.health_tracking_system.analytics.dto.HealthAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.MetricAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.TrendDirection;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HealthAnalyticsServiceIntegrationTests {

    @Autowired
    private HealthAnalyticsService analyticsService;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private User patientOne;
    private User patientTwo;
    private User provider;

    /**
     * A reporting-period date safely in the past regardless of when
     * the test suite is run.
     */
    private LocalDate baseDate;

    @BeforeEach
    void setUp() {

        bloodPressureRecordRepository.deleteAll();
        healthRecordRepository.deleteAll();
        userRepository.deleteAll();

        patientOne = saveUser(
                "Patient",
                "One",
                "patient1@example.com",
                Role.PATIENT
        );

        patientTwo = saveUser(
                "Patient",
                "Two",
                "patient2@example.com",
                Role.PATIENT
        );

        provider = saveUser(
                "Provider",
                "One",
                "provider@example.com",
                Role.PROVIDER
        );

        baseDate = LocalDate.now().minusDays(20);
    }

    @Test
    void scalarAnalyticsCalculatesDescriptiveStatisticsAndChanges() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "70",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "80",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "100",
                atDay(4, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(4)
                );

        MetricAnalyticsSummary heartRate =
                findMetric(
                        result,
                        HealthMetricType.HEART_RATE
                );

        assertBigDecimalEquals(
                "70",
                heartRate.getEarliestValue()
        );

        assertBigDecimalEquals(
                "100",
                heartRate.getLatestValue()
        );

        assertBigDecimalEquals(
                "83.333",
                heartRate.getAverageValue()
        );

        assertBigDecimalEquals(
                "80",
                heartRate.getMedianValue()
        );

        assertBigDecimalEquals(
                "70",
                heartRate.getMinimumValue()
        );

        assertBigDecimalEquals(
                "100",
                heartRate.getMaximumValue()
        );

        assertEquals(
                3,
                heartRate.getReadingCount()
        );

        assertBigDecimalEquals(
                "30",
                heartRate.getAbsoluteChange()
        );

        assertBigDecimalEquals(
                "42.86",
                heartRate.getPercentageChange()
        );

        assertEquals(
                TrendDirection.INCREASED,
                heartRate.getTrendDirection()
        );
    }

    @Test
    void evenNumberOfReadingsCalculatesMedianCorrectly() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "1",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "2",
                atDay(1, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "10",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "20",
                atDay(3, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(3)
                );

        MetricAnalyticsSummary weight =
                findMetric(
                        result,
                        HealthMetricType.WEIGHT
                );

        assertBigDecimalEquals(
                "6",
                weight.getMedianValue()
        );
    }

    @Test
    void equalEarliestAndLatestValuesProduceNoChangeTrend() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.BODY_TEMPERATURE,
                "36.5",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.BODY_TEMPERATURE,
                "37.0",
                atDay(1, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.BODY_TEMPERATURE,
                "36.5",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(2)
                );

        MetricAnalyticsSummary temperature =
                findMetric(
                        result,
                        HealthMetricType.BODY_TEMPERATURE
                );

        assertBigDecimalEquals(
                "0",
                temperature.getAbsoluteChange()
        );

        assertBigDecimalEquals(
                "0",
                temperature.getPercentageChange()
        );

        assertEquals(
                TrendDirection.NO_CHANGE,
                temperature.getTrendDirection()
        );
    }

    @Test
    void decreasingMetricProducesDecreasedTrend() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "80",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "76",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(2)
                );

        MetricAnalyticsSummary weight =
                findMetric(
                        result,
                        HealthMetricType.WEIGHT
                );

        assertBigDecimalEquals(
                "-4",
                weight.getAbsoluteChange()
        );

        assertBigDecimalEquals(
                "-5.00",
                weight.getPercentageChange()
        );

        assertEquals(
                TrendDirection.DECREASED,
                weight.getTrendDirection()
        );
    }

    @Test
    void singleReadingProducesInsufficientDataTrend() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.BLOOD_GLUCOSE,
                "5.4",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate
                );

        MetricAnalyticsSummary glucose =
                findMetric(
                        result,
                        HealthMetricType.BLOOD_GLUCOSE
                );

        assertEquals(
                1,
                glucose.getReadingCount()
        );

        assertNull(
                glucose.getAbsoluteChange()
        );

        assertNull(
                glucose.getPercentageChange()
        );

        assertEquals(
                TrendDirection.INSUFFICIENT_DATA,
                glucose.getTrendDirection()
        );
    }

    @Test
    void zeroEarliestValueMakesPercentageChangeUnavailable() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "0",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "10",
                atDay(1, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(1)
                );

        MetricAnalyticsSummary heartRate =
                findMetric(
                        result,
                        HealthMetricType.HEART_RATE
                );

        assertBigDecimalEquals(
                "10",
                heartRate.getAbsoluteChange()
        );

        assertNull(
                heartRate.getPercentageChange()
        );

        assertEquals(
                TrendDirection.INCREASED,
                heartRate.getTrendDirection()
        );
    }

    @Test
    void reportingPeriodIncludesOnlyRecordsInsideSelectedDates() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "90",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "80",
                atDay(3, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "70",
                atDay(6, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate.plusDays(2),
                        baseDate.plusDays(4)
                );

        MetricAnalyticsSummary weight =
                findMetric(
                        result,
                        HealthMetricType.WEIGHT
                );

        assertEquals(
                1,
                weight.getReadingCount()
        );

        assertBigDecimalEquals(
                "80",
                weight.getLatestValue()
        );
    }

    @Test
    void scalarChartDataPointsAreReturnedChronologically() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "90",
                atDay(4, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "70",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "80",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(4)
                );

        MetricAnalyticsSummary heartRate =
                findMetric(
                        result,
                        HealthMetricType.HEART_RATE
                );

        assertEquals(
                atDay(0, 8, 0),
                heartRate.getDataPoints()
                        .get(0)
                        .getObservedAt()
        );

        assertEquals(
                atDay(2, 8, 0),
                heartRate.getDataPoints()
                        .get(1)
                        .getObservedAt()
        );

        assertEquals(
                atDay(4, 8, 0),
                heartRate.getDataPoints()
                        .get(2)
                        .getObservedAt()
        );
    }

    @Test
    void bloodPressureAnalyticsCalculatesStatisticsWithoutBreakingPairs() {

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "120",
                "80",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "110",
                "70",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "130",
                "90",
                atDay(4, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(4)
                );

        BloodPressureAnalyticsSummary bp =
                result.getBloodPressureSummary();

        assertEquals(
                3,
                bp.getReadingCount()
        );

        assertBigDecimalEquals(
                "120",
                bp.getAverageSystolic()
        );

        assertBigDecimalEquals(
                "120",
                bp.getMedianSystolic()
        );

        assertBigDecimalEquals(
                "110",
                bp.getMinimumSystolic()
        );

        assertBigDecimalEquals(
                "130",
                bp.getMaximumSystolic()
        );

        assertBigDecimalEquals(
                "80",
                bp.getAverageDiastolic()
        );

        assertBigDecimalEquals(
                "80",
                bp.getMedianDiastolic()
        );

        assertBigDecimalEquals(
                "70",
                bp.getMinimumDiastolic()
        );

        assertBigDecimalEquals(
                "90",
                bp.getMaximumDiastolic()
        );

        assertBigDecimalEquals(
                "10",
                bp.getSystolicAbsoluteChange()
        );

        assertBigDecimalEquals(
                "8.33",
                bp.getSystolicPercentageChange()
        );

        assertBigDecimalEquals(
                "10",
                bp.getDiastolicAbsoluteChange()
        );

        assertBigDecimalEquals(
                "12.50",
                bp.getDiastolicPercentageChange()
        );

        assertEquals(
                TrendDirection.INCREASED,
                bp.getSystolicTrendDirection()
        );

        assertEquals(
                TrendDirection.INCREASED,
                bp.getDiastolicTrendDirection()
        );

        BloodPressureDataPoint earliest =
                bp.getEarliestReading();

        BloodPressureDataPoint latest =
                bp.getLatestReading();

        assertBigDecimalEquals(
                "120",
                earliest.getSystolic()
        );

        assertBigDecimalEquals(
                "80",
                earliest.getDiastolic()
        );

        assertBigDecimalEquals(
                "130",
                latest.getSystolic()
        );

        assertBigDecimalEquals(
                "90",
                latest.getDiastolic()
        );
    }

    @Test
    void bloodPressureChartPointsRemainChronologicalAndPaired() {

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "125",
                "79",
                atDay(4, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "118",
                "72",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "121",
                "76",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(4)
                );

        List<BloodPressureDataPoint> points =
                result.getBloodPressureSummary()
                        .getDataPoints();

        assertEquals(
                atDay(0, 8, 0),
                points.get(0).getObservedAt()
        );

        assertBigDecimalEquals(
                "118",
                points.get(0).getSystolic()
        );

        assertBigDecimalEquals(
                "72",
                points.get(0).getDiastolic()
        );

        assertEquals(
                atDay(2, 8, 0),
                points.get(1).getObservedAt()
        );

        assertBigDecimalEquals(
                "121",
                points.get(1).getSystolic()
        );

        assertBigDecimalEquals(
                "76",
                points.get(1).getDiastolic()
        );
    }

    @Test
    void singleBloodPressureReadingHasInsufficientTrendData() {

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "118",
                "76",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate
                );

        BloodPressureAnalyticsSummary bp =
                result.getBloodPressureSummary();

        assertEquals(
                1,
                bp.getReadingCount()
        );

        assertNull(
                bp.getSystolicAbsoluteChange()
        );

        assertNull(
                bp.getSystolicPercentageChange()
        );

        assertNull(
                bp.getDiastolicAbsoluteChange()
        );

        assertNull(
                bp.getDiastolicPercentageChange()
        );

        assertEquals(
                TrendDirection.INSUFFICIENT_DATA,
                bp.getSystolicTrendDirection()
        );

        assertEquals(
                TrendDirection.INSUFFICIENT_DATA,
                bp.getDiastolicTrendDirection()
        );
    }

    @Test
    void overallSummaryCountsReadingsAndMetricTypesCorrectly() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "75",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.WEIGHT,
                "74",
                atDay(1, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "72",
                atDay(2, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "120",
                "80",
                atDay(2, 9, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveBloodPressureRecord(
                patientOne,
                patientOne,
                "118",
                "78",
                atDay(3, 9, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(3)
                );

        assertEquals(
                5,
                result.getTotalReadingCount()
        );

        assertEquals(
                3,
                result.getMetricTypesWithData()
        );

        assertEquals(
                2,
                result.getMetricSummaries().size()
        );

        assertTrue(
                result.isHasData()
        );
    }

    @Test
    void emptyReportingPeriodReturnsNoDataSummary() {

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(5)
                );

        assertTrue(
                result.getMetricSummaries().isEmpty()
        );

        assertNull(
                result.getBloodPressureSummary()
        );

        assertEquals(
                0,
                result.getTotalReadingCount()
        );

        assertEquals(
                0,
                result.getMetricTypesWithData()
        );

        assertFalse(
                result.isHasData()
        );
    }

    @Test
    void startDateAfterEndDateIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate.plusDays(5),
                        baseDate
                )
        );
    }

    @Test
    void missingReportingPeriodDateIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        patientOne,
                        null,
                        baseDate
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        null
                )
        );
    }

    @Test
    void futureEndDateIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        patientOne,
                        LocalDate.now(),
                        LocalDate.now().plusDays(1)
                )
        );
    }

    @Test
    void patientAnalyticsNeverContainAnotherPatientsRecords() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "72",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientTwo,
                patientTwo,
                HealthMetricType.HEART_RATE,
                "200",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveBloodPressureRecord(
                patientTwo,
                patientTwo,
                "190",
                "120",
                atDay(1, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(2)
                );

        MetricAnalyticsSummary heartRate =
                findMetric(
                        result,
                        HealthMetricType.HEART_RATE
                );

        assertEquals(
                1,
                heartRate.getReadingCount()
        );

        assertBigDecimalEquals(
                "72",
                heartRate.getLatestValue()
        );

        assertNull(
                result.getBloodPressureSummary()
        );

        assertEquals(
                1,
                result.getTotalReadingCount()
        );
    }

    @Test
    void providerCannotGeneratePatientAnalytics() {

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        provider,
                        baseDate,
                        baseDate.plusDays(5)
                )
        );
    }

    @Test
    void providerRecordedHealthDataIsIncludedForOwningPatient() {

        saveScalarRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                "70",
                atDay(0, 8, 0),
                HealthRecordSource.PATIENT_REPORTED
        );

        saveScalarRecord(
                patientOne,
                provider,
                HealthMetricType.HEART_RATE,
                "80",
                atDay(1, 8, 0),
                HealthRecordSource.PROVIDER_RECORDED
        );

        saveBloodPressureRecord(
                patientOne,
                provider,
                "120",
                "80",
                atDay(2, 8, 0),
                HealthRecordSource.PROVIDER_RECORDED
        );

        HealthAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        patientOne,
                        baseDate,
                        baseDate.plusDays(2)
                );

        MetricAnalyticsSummary heartRate =
                findMetric(
                        result,
                        HealthMetricType.HEART_RATE
                );

        assertEquals(
                2,
                heartRate.getReadingCount()
        );

        assertBigDecimalEquals(
                "75",
                heartRate.getAverageValue()
        );

        assertEquals(
                1,
                result.getBloodPressureSummary()
                        .getReadingCount()
        );

        assertEquals(
                3,
                result.getTotalReadingCount()
        );
    }

    /**
     * Finds one scalar metric summary from the complete result.
     */
    private MetricAnalyticsSummary findMetric(
            HealthAnalyticsSummary summary,
            HealthMetricType metricType) {

        return summary.getMetricSummaries()
                .stream()
                .filter(metric ->
                        metric.getMetricType() == metricType
                )
                .findFirst()
                .orElseThrow();
    }

    private User saveUser(
            String firstName,
            String lastName,
            String email,
            Role role) {

        return userRepository.save(
                new User(
                        firstName,
                        lastName,
                        email,
                        "test-password-hash",
                        role
                )
        );
    }

    private HealthRecord saveScalarRecord(
            User patient,
            User recordedBy,
            HealthMetricType metricType,
            String value,
            LocalDateTime observedAt,
            HealthRecordSource source) {

        return healthRecordRepository.save(
                new HealthRecord(
                        patient,
                        recordedBy,
                        metricType,
                        new BigDecimal(value),
                        observedAt,
                        null,
                        source
                )
        );
    }

    private BloodPressureRecord saveBloodPressureRecord(
            User patient,
            User recordedBy,
            String systolic,
            String diastolic,
            LocalDateTime observedAt,
            HealthRecordSource source) {

        return bloodPressureRecordRepository.save(
                new BloodPressureRecord(
                        patient,
                        recordedBy,
                        new BigDecimal(systolic),
                        new BigDecimal(diastolic),
                        observedAt,
                        null,
                        source
                )
        );
    }

    private LocalDateTime atDay(
            int dayOffset,
            int hour,
            int minute) {

        return baseDate
                .plusDays(dayOffset)
                .atTime(hour, minute);
    }

    /**
     * Compares BigDecimal values numerically rather than by scale.
     *
     * For example, 75, 75.0 and 75.000 are treated as equal.
     */
    private void assertBigDecimalEquals(
            String expected,
            BigDecimal actual) {

        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}