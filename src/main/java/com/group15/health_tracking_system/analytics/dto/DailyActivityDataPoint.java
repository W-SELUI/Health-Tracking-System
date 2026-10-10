package com.group15.health_tracking_system.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the aggregated activity values for one calendar day
 * within an analytics reporting period.
 *
 * 
 * Multiple activity logs may exist for the same date. Their steps,
 * exercise duration and calories burned are combined into one daily
 * analytics point.
 * 
 *
 * 
 * The log count preserves the distinction between a day containing
 * recorded zero values and a day for which no activity data was recorded.
 * 
 *
 * This class is an analytics DTO only and is not persisted in the database.
 */
public class DailyActivityDataPoint {

    private final LocalDate date;
    private final long steps;
    private final long durationMinutes;
    private final BigDecimal caloriesBurned;
    private final long logCount;

    /**
     * Creates an aggregated daily activity data point.
     *
     * @param date calendar date represented by this point
     * @param steps total recorded steps for the date
     * @param durationMinutes total recorded exercise minutes for the date
     * @param caloriesBurned total recorded calories burned for the date
     * @param logCount number of activity logs recorded for the date
     */
    public DailyActivityDataPoint(
            LocalDate date,
            long steps,
            long durationMinutes,
            BigDecimal caloriesBurned,
            long logCount) {

        this.date = date;
        this.steps = steps;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
        this.logCount = logCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getSteps() {
        return steps;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getCaloriesBurned() {
        return caloriesBurned;
    }

    public long getLogCount() {
        return logCount;
    }

    /**
     * Indicates whether at least one activity log was recorded for this date.
     *
     * @return true when one or more activity logs exist for the date
     */
    public boolean hasEntries() {
        return logCount > 0;
    }
}