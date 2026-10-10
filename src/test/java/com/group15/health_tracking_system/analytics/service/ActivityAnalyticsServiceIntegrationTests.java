package com.group15.health_tracking_system.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.activity.ActivityForm;
import com.group15.health_tracking_system.activity.ActivityLogRepository;
import com.group15.health_tracking_system.activity.ActivityService;
import com.group15.health_tracking_system.activity.ExerciseType;
import com.group15.health_tracking_system.analytics.dto.ActivityAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.DailyActivityDataPoint;
import com.group15.health_tracking_system.analytics.dto.ExerciseTypeAnalytics;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for ActivityAnalyticsService.
 *
 * 
 * These tests verify that activity analytics correctly integrate with
 * Shaniza's existing ActivityService and preserve patient ownership,
 * calendar-day semantics and activity aggregation rules.
 * 
 */
@SpringBootTest
@Transactional
@WithMockUser(username = "alice", roles = "PATIENT")
class ActivityAnalyticsServiceIntegrationTests {

    @Autowired
    private ActivityAnalyticsService analyticsService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    private LocalDate baseDate;

    @BeforeEach
    void setUp() {

        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(
                new User(
                        "Alice",
                        "Patient",
                        "alice",
                        "unused-test-hash",
                        Role.PATIENT
                )
        );

        userRepository.save(
                new User(
                        "Bob",
                        "Patient",
                        "bob",
                        "unused-test-hash",
                        Role.PATIENT
                )
        );

        userRepository.save(
                new User(
                        "Provider",
                        "One",
                        "provider",
                        "unused-test-hash",
                        Role.PROVIDER
                )
        );

        /*
         * Keep all test activity safely in the past so tests remain
         * valid regardless of the date on which they are executed.
         */
        baseDate = LocalDate.now().minusDays(20);
    }

    @Test
    void sameDayLogsAreAggregatedIntoOneChronologicalDailyPoint() {

        createActivity(
                baseDate,
                3000,
                ExerciseType.WALKING,
                20,
                "150.25"
        );

        createActivity(
                baseDate,
                2500,
                ExerciseType.RUNNING,
                30,
                "220.75"
        );

        createActivity(
                baseDate.plusDays(2),
                7000,
                ExerciseType.WALKING,
                40,
                "300.00"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(3)
                );

        List<DailyActivityDataPoint> points =
                result.getDailyDataPoints();

        assertEquals(4, points.size());

        DailyActivityDataPoint firstDay =
                points.get(0);

        assertEquals(
                baseDate,
                firstDay.getDate()
        );

        assertEquals(
                5500,
                firstDay.getSteps()
        );

        assertEquals(
                50,
                firstDay.getDurationMinutes()
        );

        assertBigDecimalEquals(
                "371.00",
                firstDay.getCaloriesBurned()
        );

        assertEquals(
                2,
                firstDay.getLogCount()
        );

        assertTrue(
                firstDay.hasEntries()
        );

        DailyActivityDataPoint secondDay =
                points.get(1);

        assertEquals(
                baseDate.plusDays(1),
                secondDay.getDate()
        );

        assertEquals(
                0,
                secondDay.getSteps()
        );

        assertEquals(
                0,
                secondDay.getLogCount()
        );

        assertFalse(
                secondDay.hasEntries()
        );

        DailyActivityDataPoint thirdDay =
                points.get(2);

        assertEquals(
                baseDate.plusDays(2),
                thirdDay.getDate()
        );

        assertEquals(
                7000,
                thirdDay.getSteps()
        );

        assertEquals(
                1,
                thirdDay.getLogCount()
        );
    }

    @Test
    void activityTotalsAndCalendarDayAverageAreCalculatedCorrectly() {

        createActivity(
                baseDate,
                3000,
                ExerciseType.WALKING,
                20,
                "100"
        );

        createActivity(
                baseDate,
                2500,
                ExerciseType.RUNNING,
                30,
                "200"
        );

        createActivity(
                baseDate.plusDays(2),
                7000,
                ExerciseType.CYCLING,
                40,
                "300"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(3)
                );

        assertEquals(
                3,
                result.getLogCount()
        );

        assertEquals(
                2,
                result.getDaysWithEntries()
        );

        assertEquals(
                4,
                result.getCalendarDays()
        );

        assertEquals(
                12500,
                result.getTotalSteps()
        );

        assertBigDecimalEquals(
                "3125.00",
                result.getAverageStepsPerCalendarDay()
        );

        assertTrue(
                result.isHasData()
        );
    }

    @Test
    void trackingCoverageUsesDistinctRecordedDaysRatherThanLogCount() {

        /*
         * Two logs on the same date still count as only one tracked day.
         */
        createActivity(
                baseDate,
                1000,
                ExerciseType.WALKING,
                10,
                "50"
        );

        createActivity(
                baseDate,
                2000,
                ExerciseType.RUNNING,
                20,
                "75"
        );

        createActivity(
                baseDate.plusDays(2),
                3000,
                ExerciseType.CYCLING,
                30,
                "100"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(3)
                );

        assertEquals(
                3,
                result.getLogCount()
        );

        assertEquals(
                2,
                result.getDaysWithEntries()
        );

        assertEquals(
                4,
                result.getCalendarDays()
        );

        assertBigDecimalEquals(
                "50.00",
                result.getTrackingCoveragePercentage()
        );
    }

    @Test
    void caloriesAndExerciseDurationUseCalendarDayAverages() {

        createActivity(
                baseDate,
                1000,
                ExerciseType.WALKING,
                30,
                "100.00"
        );

        createActivity(
                baseDate.plusDays(1),
                2000,
                ExerciseType.RUNNING,
                null,
                "200.00"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(2)
                );

        assertBigDecimalEquals(
                "300.00",
                result.getTotalCaloriesBurned()
        );

        /*
         * 300 calories / 3 calendar days.
         */
        assertBigDecimalEquals(
                "100.00",
                result.getAverageCaloriesPerCalendarDay()
        );

        assertEquals(
                30,
                result.getTotalDurationMinutes()
        );

        /*
         * 30 minutes / 3 calendar days.
         */
        assertBigDecimalEquals(
                "10.00",
                result.getAverageDurationMinutesPerCalendarDay()
        );
    }

    @Test
    void nullExerciseDurationIsTreatedAsZero() {

        createActivity(
                baseDate,
                1500,
                ExerciseType.WALKING,
                null,
                "75.00"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate
                );

        assertEquals(
                0,
                result.getTotalDurationMinutes()
        );

        assertBigDecimalEquals(
                "0.00",
                result.getAverageDurationMinutesPerCalendarDay()
        );

        assertEquals(
                0,
                result.getDailyDataPoints()
                        .get(0)
                        .getDurationMinutes()
        );
    }

    @Test
    void highestStepDayUsesAggregatedDailyStepTotal() {

        /*
         * First day:
         * 4000 + 5000 = 9000 steps.
         */
        createActivity(
                baseDate,
                4000,
                ExerciseType.WALKING,
                20,
                "100"
        );

        createActivity(
                baseDate,
                5000,
                ExerciseType.RUNNING,
                20,
                "100"
        );

        /*
         * Second day has one large log, but its daily total is still
         * lower than the first day's aggregated value.
         */
        createActivity(
                baseDate.plusDays(1),
                8500,
                ExerciseType.CYCLING,
                30,
                "200"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(1)
                );

        assertEquals(
                9000,
                result.getHighestDailySteps()
        );

        assertEquals(
                baseDate,
                result.getHighestStepDate()
        );
    }

    @Test
    void highestStepTieKeepsEarliestMatchingDate() {

        createActivity(
                baseDate,
                9000,
                ExerciseType.WALKING,
                30,
                "200"
        );

        createActivity(
                baseDate.plusDays(2),
                9000,
                ExerciseType.RUNNING,
                30,
                "200"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(2)
                );

        assertEquals(
                9000,
                result.getHighestDailySteps()
        );

        assertEquals(
                baseDate,
                result.getHighestStepDate()
        );
    }

    @Test
    void exerciseTypeBreakdownAggregatesLogsAndDuration() {

        createActivity(
                baseDate,
                1000,
                ExerciseType.WALKING,
                20,
                "50"
        );

        createActivity(
                baseDate.plusDays(1),
                2000,
                ExerciseType.WALKING,
                30,
                "70"
        );

        createActivity(
                baseDate.plusDays(2),
                3000,
                ExerciseType.RUNNING,
                40,
                "100"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(2)
                );

        ExerciseTypeAnalytics walking =
                findExerciseType(
                        result,
                        ExerciseType.WALKING
                );

        assertEquals(
                2,
                walking.getLogCount()
        );

        assertEquals(
                50,
                walking.getTotalDurationMinutes()
        );

        ExerciseTypeAnalytics running =
                findExerciseType(
                        result,
                        ExerciseType.RUNNING
                );

        assertEquals(
                1,
                running.getLogCount()
        );

        assertEquals(
                40,
                running.getTotalDurationMinutes()
        );
    }

    @Test
    void exerciseTypeNoneIsExcludedFromBreakdownButStillCountsAsActivityData() {

        createActivity(
                baseDate,
                0,
                ExerciseType.NONE,
                null,
                "0"
        );

        createActivity(
                baseDate.plusDays(1),
                1500,
                ExerciseType.WALKING,
                20,
                "50"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(1)
                );

        assertEquals(
                2,
                result.getLogCount()
        );

        assertTrue(
                result.isHasData()
        );

        assertEquals(
                1,
                result.getExerciseTypeAnalytics().size()
        );

        assertEquals(
                ExerciseType.WALKING,
                result.getExerciseTypeAnalytics()
                        .get(0)
                        .getExerciseType()
        );
    }

    @Test
    void recordedZeroActivityIsDifferentFromMissingActivityDay() {

        /*
         * An actual activity record containing zero values.
         */
        createActivity(
                baseDate,
                0,
                ExerciseType.NONE,
                null,
                "0"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(1)
                );

        DailyActivityDataPoint recordedZeroDay =
                result.getDailyDataPoints()
                        .get(0);

        DailyActivityDataPoint missingDay =
                result.getDailyDataPoints()
                        .get(1);

        assertEquals(
                0,
                recordedZeroDay.getSteps()
        );

        assertTrue(
                recordedZeroDay.hasEntries()
        );

        assertEquals(
                1,
                recordedZeroDay.getLogCount()
        );

        assertEquals(
                0,
                missingDay.getSteps()
        );

        assertFalse(
                missingDay.hasEntries()
        );

        assertEquals(
                0,
                missingDay.getLogCount()
        );

        /*
         * One of the two selected dates contains an actual log.
         */
        assertBigDecimalEquals(
                "50.00",
                result.getTrackingCoveragePercentage()
        );
    }

    @Test
    void emptyReportingPeriodReturnsNoDataSummary() {

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(2)
                );

        assertFalse(
                result.isHasData()
        );

        assertEquals(
                0,
                result.getLogCount()
        );

        assertEquals(
                0,
                result.getDaysWithEntries()
        );

        assertEquals(
                3,
                result.getCalendarDays()
        );

        assertEquals(
                0,
                result.getTotalSteps()
        );

        assertBigDecimalEquals(
                "0.00",
                result.getAverageStepsPerCalendarDay()
        );

        assertBigDecimalEquals(
                "0.00",
                result.getTrackingCoveragePercentage()
        );

        assertBigDecimalEquals(
                "0.00",
                result.getAverageCaloriesPerCalendarDay()
        );

        assertBigDecimalEquals(
                "0.00",
                result.getAverageDurationMinutesPerCalendarDay()
        );

        assertEquals(
                0,
                result.getHighestDailySteps()
        );

        assertNull(
                result.getHighestStepDate()
        );

        assertEquals(
                3,
                result.getDailyDataPoints().size()
        );

        assertTrue(
                result.getDailyDataPoints()
                        .stream()
                        .noneMatch(
                                DailyActivityDataPoint::hasEntries
                        )
        );

        assertTrue(
                result.getExerciseTypeAnalytics()
                        .isEmpty()
        );
    }

    @Test
    void invalidMissingAndFutureReportingPeriodsAreRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        baseDate.plusDays(2),
                        baseDate
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        null,
                        baseDate
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        baseDate,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getPatientAnalytics(
                        LocalDate.now(),
                        LocalDate.now().plusDays(1)
                )
        );
    }

    @Test
    void patientAnalyticsNeverContainAnotherPatientsActivity() {

        /*
         * Alice's activity.
         */
        createActivity(
                baseDate,
                2000,
                ExerciseType.WALKING,
                20,
                "100"
        );

        /*
         * Temporarily authenticate as Bob and create Bob's own data.
         */
        authenticate(
                "bob",
                "PATIENT"
        );

        createActivity(
                baseDate,
                99999,
                ExerciseType.RUNNING,
                500,
                "9999"
        );

        /*
         * Return to Alice before generating analytics.
         */
        authenticate(
                "alice",
                "PATIENT"
        );

        ActivityAnalyticsSummary result =
                analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate
                );

        assertEquals(
                1,
                result.getLogCount()
        );

        assertEquals(
                2000,
                result.getTotalSteps()
        );

        assertEquals(
                20,
                result.getTotalDurationMinutes()
        );

        assertBigDecimalEquals(
                "100",
                result.getTotalCaloriesBurned()
        );
    }

    @Test
    @WithMockUser(
            username = "provider",
            roles = "PROVIDER"
    )
    void providerCannotGeneratePatientActivityAnalytics() {

        assertThrows(
                AccessDeniedException.class,
                () -> analyticsService.getPatientAnalytics(
                        baseDate,
                        baseDate.plusDays(2)
                )
        );
    }

    /**
     * Creates one activity record using the real ActivityService.
     *
     * 
     * Ownership is therefore derived from the authenticated user rather
     * than supplied directly by the test.
     *
     */
    private void createActivity(
            LocalDate date,
            int steps,
            ExerciseType exerciseType,
            Integer durationMinutes,
            String calories) {

        ActivityForm form =
                new ActivityForm();

        form.setActivityDate(date);
        form.setSteps(steps);
        form.setExerciseType(exerciseType);
        form.setDurationMinutes(durationMinutes);
        form.setCaloriesBurned(
                new BigDecimal(calories)
        );

        activityService.create(form);
    }

    /**
     * Finds analytics for a particular exercise type.
     */
    private ExerciseTypeAnalytics findExerciseType(
            ActivityAnalyticsSummary summary,
            ExerciseType exerciseType) {

        return summary
                .getExerciseTypeAnalytics()
                .stream()
                .filter(item ->
                        item.getExerciseType()
                                == exerciseType
                )
                .findFirst()
                .orElseThrow();
    }

    /**
     * Replaces the currently authenticated test user.
     */
    private void authenticate(
            String username,
            String role) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        "unused-test-password",
                        AuthorityUtils.createAuthorityList(
                                "ROLE_" + role
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }

    /**
     * Compares BigDecimal values numerically rather than by scale.
     *
     * 
     * For example, 100, 100.0 and 100.00 are considered equal.
     * 
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