package com.group15.health_tracking_system.analytics.dto;

import com.group15.health_tracking_system.activity.ExerciseType;

/**
 * Represents aggregated analytics for one exercise type during a selected
 * reporting period.
 *
 * 
 * The summary records how many activity logs used the exercise type and
 * the total exercise duration associated with those logs.
 * 
 *
 * 
 * This class is an analytics DTO only and is not persisted in the database.
 * 
 */
public class ExerciseTypeAnalytics {

    private final ExerciseType exerciseType;
    private final long logCount;
    private final long totalDurationMinutes;

    /**
     * Creates an exercise-type analytics summary.
     *
     * @param exerciseType exercise category being summarized
     * @param logCount number of activity logs using this exercise type
     * @param totalDurationMinutes total recorded exercise duration for the type
     */
    public ExerciseTypeAnalytics(
            ExerciseType exerciseType,
            long logCount,
            long totalDurationMinutes) {

        this.exerciseType = exerciseType;
        this.logCount = logCount;
        this.totalDurationMinutes = totalDurationMinutes;
    }

    public ExerciseType getExerciseType() {
        return exerciseType;
    }

    public long getLogCount() {
        return logCount;
    }

    public long getTotalDurationMinutes() {
        return totalDurationMinutes;
    }
}