package com.group15.health_tracking_system.analytics.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Represents the reporting period selected by the patient for the
 * analytics dashboard.
 *
 * The same reporting period is used for both health analytics and
 * activity analytics so that all dashboard sections describe the
 * same time range.
 *
 * This class is a web-request DTO only and is not persisted in the database.
 */
public class AnalyticsPeriodRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public AnalyticsPeriodRequest() {
    }

    public AnalyticsPeriodRequest(
            LocalDate startDate,
            LocalDate endDate) {

        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Indicates whether neither reporting-period date has been supplied.
     *
     * This is used to distinguish the initial analytics page from an
     * incomplete submitted reporting period.
     *
     * @return true when both dates are absent
     */
    public boolean isEmpty() {
        return startDate == null
                && endDate == null;
    }

    /**
     * Indicates whether both required reporting-period dates are present.
     *
     * @return true when both start and end dates are available
     */
    public boolean isComplete() {
        return startDate != null
                && endDate != null;
    }
}