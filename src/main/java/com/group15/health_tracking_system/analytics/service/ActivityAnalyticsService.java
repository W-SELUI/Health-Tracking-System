package com.group15.health_tracking_system.analytics.service;

import com.group15.health_tracking_system.activity.ActivityLog;
import com.group15.health_tracking_system.activity.ActivityService;
import com.group15.health_tracking_system.activity.ActivitySummary;
import com.group15.health_tracking_system.activity.ExerciseType;
import com.group15.health_tracking_system.analytics.dto.ActivityAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.DailyActivityDataPoint;
import com.group15.health_tracking_system.analytics.dto.ExerciseTypeAnalytics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates descriptive activity analytics for a patient.
 *
 * The normal patient-facing method derives ownership from the authenticated
 * patient through ActivityService. An explicit patient-ID overload is also
 * available for higher-level, consent-protected workflows such as Provider
 * Analytics.
 *
 * This service does not itself authorize Providers. Provider authorization
 * must be performed by the calling Provider service before the explicit
 * patient-ID overload is invoked.
 *
 * Analytics are calculated on demand and are not persisted in the database.
 * The results are descriptive only and do not provide medical or fitness
 * recommendations.
 */
@Service
public class ActivityAnalyticsService {

    private static final int DECIMAL_SCALE = 2;

    private static final RoundingMode ROUNDING_MODE =
            RoundingMode.HALF_UP;

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private final ActivityService activityService;

    public ActivityAnalyticsService(
            ActivityService activityService) {

        this.activityService = activityService;
    }

    /**
     * Generates activity analytics for the authenticated patient during
     * the selected reporting period.
     *
     * Existing Patient functionality continues to use this method.
     * Ownership is resolved through ActivityService and CurrentPatient.
     *
     * @param startDate first date in the reporting period
     * @param endDate last date in the reporting period
     * @return complete activity analytics summary
     */
    @Transactional(readOnly = true)
    public ActivityAnalyticsSummary getPatientAnalytics(
            LocalDate startDate,
            LocalDate endDate) {

        validateReportingPeriod(
                startDate,
                endDate
        );

        ActivitySummary baseSummary =
                activityService.summary(
                        startDate,
                        endDate
                );

        List<ActivityLog> logs =
                activityService.history(
                        startDate,
                        endDate
                );

        return buildAnalytics(
                baseSummary,
                logs,
                startDate,
                endDate
        );
    }

    /**
     * Generates activity analytics for an explicitly selected patient.
     *
     * This method is intended for higher-level workflows in which
     * authorization has already been established, such as a healthcare
     * Provider viewing analytics for a patient who has granted active
     * Consent.
     *
     * No Provider authorization is performed here. The calling service must
     * first verify that the authenticated Provider is allowed to access the
     * specified patient.
     *
     * @param patientId target patient's identifier
     * @param startDate first date in the reporting period
     * @param endDate last date in the reporting period
     * @return complete activity analytics summary
     */
    @Transactional(readOnly = true)
    public ActivityAnalyticsSummary getPatientAnalytics(
            Long patientId,
            LocalDate startDate,
            LocalDate endDate) {

        if (patientId == null) {

            throw new IllegalArgumentException(
                    "Patient is required"
            );
        }

        validateReportingPeriod(
                startDate,
                endDate
        );

        ActivitySummary baseSummary =
                activityService.summaryForPatient(
                        patientId,
                        startDate,
                        endDate
                );

        List<ActivityLog> logs =
                activityService.historyForPatient(
                        patientId,
                        startDate,
                        endDate
                );

        return buildAnalytics(
                baseSummary,
                logs,
                startDate,
                endDate
        );
    }

    /**
     * Builds the shared analytics result from the Activity module's
     * patient-scoped data.
     *
     * Both Patient and Provider views therefore use exactly the same
     * aggregation, averages, coverage calculations and chart data.
     */
    private ActivityAnalyticsSummary buildAnalytics(
            ActivitySummary baseSummary,
            List<ActivityLog> logs,
            LocalDate startDate,
            LocalDate endDate) {

        List<DailyActivityDataPoint> dailyDataPoints =
                buildDailyDataPoints(
                        logs,
                        startDate,
                        endDate
                );

        List<ExerciseTypeAnalytics> exerciseTypeAnalytics =
                buildExerciseTypeAnalytics(
                        logs
                );

        BigDecimal trackingCoveragePercentage =
                calculateTrackingCoverage(
                        baseSummary.daysWithEntries(),
                        baseSummary.calendarDays()
                );

        BigDecimal averageCaloriesPerCalendarDay =
                divideByCalendarDays(
                        baseSummary.totalCaloriesBurned(),
                        baseSummary.calendarDays()
                );

        BigDecimal averageDurationMinutesPerCalendarDay =
                divideByCalendarDays(
                        BigDecimal.valueOf(
                                baseSummary.totalDurationMinutes()
                        ),
                        baseSummary.calendarDays()
                );

        DailyActivityDataPoint highestStepDay =
                findHighestStepDay(
                        dailyDataPoints
                );

        long highestDailySteps =
                highestStepDay == null
                        ? 0
                        : highestStepDay.getSteps();

        LocalDate highestStepDate =
                highestStepDay == null
                        ? null
                        : highestStepDay.getDate();

        return new ActivityAnalyticsSummary(
                startDate,
                endDate,

                baseSummary.logCount(),
                baseSummary.daysWithEntries(),
                baseSummary.calendarDays(),

                trackingCoveragePercentage,

                baseSummary.totalSteps(),
                baseSummary.averageStepsPerCalendarDay(),

                highestDailySteps,
                highestStepDate,

                baseSummary.totalCaloriesBurned(),
                averageCaloriesPerCalendarDay,

                baseSummary.totalDurationMinutes(),
                averageDurationMinutesPerCalendarDay,

                dailyDataPoints,
                exerciseTypeAnalytics
        );
    }

    /**
     * Builds one aggregated activity point for every calendar date in the
     * selected reporting period.
     *
     * When multiple activity logs occur on the same date, their steps,
     * exercise duration and calories are added together.
     *
     * Dates without activity logs are still represented. Their numerical
     * totals are zero and their log count is zero, allowing the presentation
     * layer to distinguish missing activity data from an explicitly recorded
     * zero-value activity log.
     */
    private List<DailyActivityDataPoint> buildDailyDataPoints(
            List<ActivityLog> logs,
            LocalDate startDate,
            LocalDate endDate) {

        Map<LocalDate, List<ActivityLog>> logsByDate =
                logs.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ActivityLog::getActivityDate
                                )
                        );

        List<DailyActivityDataPoint> dataPoints =
                new ArrayList<>();

        LocalDate currentDate =
                startDate;

        while (!currentDate.isAfter(endDate)) {

            List<ActivityLog> dateLogs =
                    logsByDate.getOrDefault(
                            currentDate,
                            List.of()
                    );

            long steps =
                    dateLogs.stream()
                            .mapToLong(
                                    ActivityLog::getSteps
                            )
                            .sum();

            long durationMinutes =
                    dateLogs.stream()
                            .mapToLong(log ->
                                    log.getDurationMinutes() == null
                                            ? 0
                                            : log.getDurationMinutes()
                            )
                            .sum();

            BigDecimal caloriesBurned =
                    dateLogs.stream()
                            .map(
                                    ActivityLog::getCaloriesBurned
                            )
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            dataPoints.add(
                    new DailyActivityDataPoint(
                            currentDate,
                            steps,
                            durationMinutes,
                            caloriesBurned,
                            dateLogs.size()
                    )
            );

            currentDate =
                    currentDate.plusDays(1);
        }

        return dataPoints;
    }

    /**
     * Builds aggregated analytics for each exercise category represented
     * during the reporting period.
     *
     * @param logs patient activity logs
     * @return exercise-type analytics
     */
    private List<ExerciseTypeAnalytics> buildExerciseTypeAnalytics(
            List<ActivityLog> logs) {

        Map<ExerciseType, List<ActivityLog>> logsByExerciseType =
                new EnumMap<>(
                        ExerciseType.class
                );

        for (ActivityLog log : logs) {

            ExerciseType exerciseType =
                    log.getExerciseType();

            if (exerciseType
                    == ExerciseType.NONE) {

                continue;
            }

            logsByExerciseType
                    .computeIfAbsent(
                            exerciseType,
                            key -> new ArrayList<>()
                    )
                    .add(log);
        }

        List<ExerciseTypeAnalytics> summaries =
                new ArrayList<>();

        /*
         * Iterating through the enum gives the analytics output a stable
         * and predictable ordering.
         */
        for (ExerciseType exerciseType
                : ExerciseType.values()) {

            if (exerciseType
                    == ExerciseType.NONE) {

                continue;
            }

            List<ActivityLog> typeLogs =
                    logsByExerciseType.get(
                            exerciseType
                    );

            if (typeLogs == null
                    || typeLogs.isEmpty()) {

                continue;
            }

            long totalDurationMinutes =
                    typeLogs.stream()
                            .mapToLong(log ->
                                    log.getDurationMinutes() == null
                                            ? 0
                                            : log.getDurationMinutes()
                            )
                            .sum();

            summaries.add(
                    new ExerciseTypeAnalytics(
                            exerciseType,
                            typeLogs.size(),
                            totalDurationMinutes
                    )
            );
        }

        return summaries;
    }

    /**
     * Finds the date with the highest aggregated daily step total.
     *
     * Only dates containing at least one Activity Log are considered.
     * Because daily points are chronological and a maximum is replaced only
     * by a strictly greater value, the earliest date is retained when
     * multiple dates share the highest step total.
     */
    private DailyActivityDataPoint findHighestStepDay(
            List<DailyActivityDataPoint> dailyDataPoints) {

        DailyActivityDataPoint highest =
                null;

        for (DailyActivityDataPoint point
                : dailyDataPoints) {

            if (!point.hasEntries()) {

                continue;
            }

            if (highest == null
                    || point.getSteps()
                    > highest.getSteps()) {

                highest =
                        point;
            }
        }

        return highest;
    }

    /**
     * Calculates the percentage of calendar days in the reporting period
     * for which at least one Activity Log exists.
     */
    private BigDecimal calculateTrackingCoverage(
            long daysWithEntries,
            long calendarDays) {

        return BigDecimal
                .valueOf(
                        daysWithEntries
                )
                .multiply(
                        ONE_HUNDRED
                )
                .divide(
                        BigDecimal.valueOf(
                                calendarDays
                        ),
                        DECIMAL_SCALE,
                        ROUNDING_MODE
                );
    }

    /**
     * Calculates a calendar-day average from a decimal total.
     */
    private BigDecimal divideByCalendarDays(
            BigDecimal total,
            long calendarDays) {

        return total.divide(
                BigDecimal.valueOf(
                        calendarDays
                ),
                DECIMAL_SCALE,
                ROUNDING_MODE
        );
    }

    /**
     * Validates the analytics reporting period before querying Activity data.
     */
    private void validateReportingPeriod(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null
                || endDate == null) {

            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(
                endDate
        )) {

            throw new IllegalArgumentException(
                    "Start date must not be after end date"
            );
        }

        if (endDate.isAfter(
                LocalDate.now()
        )) {

            throw new IllegalArgumentException(
                    "End date must not be in the future"
            );
        }
    }
}