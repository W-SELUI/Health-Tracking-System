package com.group15.health_tracking_system.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents the complete activity analytics result for a patient during
 * a selected reporting period.
 *
 *
 * The summary contains activity totals, calendar-day averages, tracking
 * coverage, the highest-step day, daily chart data and exercise-type
 * analytics.
 *
 *  * This class is an analytics DTO only and is not persisted in the database.
 * 
 */
public class ActivityAnalyticsSummary {

    private final LocalDate startDate;
    private final LocalDate endDate;

    private final long logCount;
    private final long daysWithEntries;
    private final long calendarDays;

    private final BigDecimal trackingCoveragePercentage;

    private final long totalSteps;
    private final BigDecimal averageStepsPerCalendarDay;

    private final long highestDailySteps;
    private final LocalDate highestStepDate;

    private final BigDecimal totalCaloriesBurned;
    private final BigDecimal averageCaloriesPerCalendarDay;

    private final long totalDurationMinutes;
    private final BigDecimal averageDurationMinutesPerCalendarDay;

    private final List<DailyActivityDataPoint> dailyDataPoints;
    private final List<ExerciseTypeAnalytics> exerciseTypeAnalytics;

    private final boolean hasData;

    /**
     * Creates an activity analytics summary.
     *
     * @param startDate first date in the reporting period
     * @param endDate last date in the reporting period
     * @param logCount number of activity logs in the period
     * @param daysWithEntries number of calendar dates containing activity logs
     * @param calendarDays number of calendar days in the selected period
     * @param trackingCoveragePercentage percentage of selected days containing
     *                                   at least one activity log
     * @param totalSteps total recorded steps
     * @param averageStepsPerCalendarDay average steps across all calendar days
     * @param highestDailySteps highest aggregated daily step total
     * @param highestStepDate earliest date achieving the highest daily step total
     * @param totalCaloriesBurned total recorded calories burned
     * @param averageCaloriesPerCalendarDay average calories burned per calendar day
     * @param totalDurationMinutes total recorded exercise duration
     * @param averageDurationMinutesPerCalendarDay average exercise duration
     *                                            per calendar day
     * @param dailyDataPoints chronological daily activity data
     * @param exerciseTypeAnalytics aggregated exercise-type analytics
     */
    public ActivityAnalyticsSummary(
            LocalDate startDate,
            LocalDate endDate,
            long logCount,
            long daysWithEntries,
            long calendarDays,
            BigDecimal trackingCoveragePercentage,
            long totalSteps,
            BigDecimal averageStepsPerCalendarDay,
            long highestDailySteps,
            LocalDate highestStepDate,
            BigDecimal totalCaloriesBurned,
            BigDecimal averageCaloriesPerCalendarDay,
            long totalDurationMinutes,
            BigDecimal averageDurationMinutesPerCalendarDay,
            List<DailyActivityDataPoint> dailyDataPoints,
            List<ExerciseTypeAnalytics> exerciseTypeAnalytics) {

        this.startDate = startDate;
        this.endDate = endDate;

        this.logCount = logCount;
        this.daysWithEntries = daysWithEntries;
        this.calendarDays = calendarDays;

        this.trackingCoveragePercentage =
                trackingCoveragePercentage;

        this.totalSteps = totalSteps;
        this.averageStepsPerCalendarDay =
                averageStepsPerCalendarDay;

        this.highestDailySteps = highestDailySteps;
        this.highestStepDate = highestStepDate;

        this.totalCaloriesBurned = totalCaloriesBurned;
        this.averageCaloriesPerCalendarDay =
                averageCaloriesPerCalendarDay;

        this.totalDurationMinutes = totalDurationMinutes;
        this.averageDurationMinutesPerCalendarDay =
                averageDurationMinutesPerCalendarDay;

        this.dailyDataPoints =
                List.copyOf(dailyDataPoints);

        this.exerciseTypeAnalytics =
                List.copyOf(exerciseTypeAnalytics);

        this.hasData = logCount > 0;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public long getLogCount() {
        return logCount;
    }

    public long getDaysWithEntries() {
        return daysWithEntries;
    }

    public long getCalendarDays() {
        return calendarDays;
    }

    public BigDecimal getTrackingCoveragePercentage() {
        return trackingCoveragePercentage;
    }

    public long getTotalSteps() {
        return totalSteps;
    }

    public BigDecimal getAverageStepsPerCalendarDay() {
        return averageStepsPerCalendarDay;
    }

    public long getHighestDailySteps() {
        return highestDailySteps;
    }

    public LocalDate getHighestStepDate() {
        return highestStepDate;
    }

    public BigDecimal getTotalCaloriesBurned() {
        return totalCaloriesBurned;
    }

    public BigDecimal getAverageCaloriesPerCalendarDay() {
        return averageCaloriesPerCalendarDay;
    }

    public long getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public BigDecimal getAverageDurationMinutesPerCalendarDay() {
        return averageDurationMinutesPerCalendarDay;
    }

    public List<DailyActivityDataPoint> getDailyDataPoints() {
        return dailyDataPoints;
    }

    public List<ExerciseTypeAnalytics> getExerciseTypeAnalytics() {
        return exerciseTypeAnalytics;
    }

    public boolean isHasData() {
        return hasData;
    }
}