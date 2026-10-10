package com.group15.health_tracking_system.activity;
import java.math.BigDecimal;
/** Averages use calendar days in the inclusive selected period, including days with no entries. */
public record ActivitySummary(long logCount,long daysWithEntries,long calendarDays,long totalSteps,long totalDurationMinutes,BigDecimal totalCaloriesBurned,BigDecimal averageStepsPerCalendarDay) {}
