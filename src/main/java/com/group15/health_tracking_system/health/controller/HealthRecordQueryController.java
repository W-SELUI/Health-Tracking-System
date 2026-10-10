package com.group15.health_tracking_system.health.controller;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.service.HealthRecordQueryService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles patient-side viewing and filtering of health records.
 */
@Controller
@RequestMapping("/patient/health-records")
public class HealthRecordQueryController {

    private final HealthRecordQueryService queryService;
    private final CurrentUserService currentUserService;

    public HealthRecordQueryController(
            HealthRecordQueryService queryService,
            CurrentUserService currentUserService) {

        this.queryService = queryService;
        this.currentUserService = currentUserService;
    }

    /**
     * Shows the authenticated patient's health-record history.
     *
     * Optional filters:
     * - metric type
     * - start date
     * - end date
     */
    @GetMapping
    public String listRecords(
            @RequestParam(required = false)
            HealthMetricType metricType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model) {

        User currentUser = currentUserService.getCurrentUser();

        List<HealthRecord> records;

        try {
            records = selectRecords(
                    currentUser,
                    metricType,
                    startDate,
                    endDate
            );
        } catch (IllegalArgumentException exception) {

            model.addAttribute(
                    "filterError",
                    exception.getMessage()
            );

            /*
             * If the submitted filter is invalid, still show the patient's
             * normal history instead of failing the whole page.
             */
            records = queryService.getPatientHistory(currentUser);
        }

        model.addAttribute("records", records);
        model.addAttribute(
                "metricTypes",
                HealthMetricType.values()
        );

        model.addAttribute(
                "selectedMetricType",
                metricType
        );

        model.addAttribute(
                "selectedStartDate",
                startDate
        );

        model.addAttribute(
                "selectedEndDate",
                endDate
        );

        return "health-records/list";
    }

    /**
     * Shows one record belonging to the authenticated patient.
     */
    @GetMapping("/{recordId}")
    public String viewRecord(
            @PathVariable Long recordId,
            Model model) {

        User currentUser = currentUserService.getCurrentUser();

        HealthRecord record =
                queryService.getPatientRecord(
                        recordId,
                        currentUser
                );

        model.addAttribute(
                "record",
                record
        );

        return "health-records/view";
    }

    private List<HealthRecord> selectRecords(
            User currentUser,
            HealthMetricType metricType,
            LocalDate startDate,
            LocalDate endDate) {

        boolean hasMetric = metricType != null;
        boolean hasStart = startDate != null;
        boolean hasEnd = endDate != null;

        /*
         * Date filtering requires both dates.
         */
        if (hasStart != hasEnd) {
            throw new IllegalArgumentException(
                    "Both start date and end date are required"
            );
        }

        /*
         * No date range.
         */
        if (!hasStart) {

            if (hasMetric) {
                return queryService
                        .getPatientHistoryByMetric(
                                currentUser,
                                metricType
                        );
            }

            return queryService
                    .getPatientHistory(currentUser);
        }

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(LocalTime.MAX);

        /*
         * Metric + period.
         */
        if (hasMetric) {
            return queryService
                    .getPatientHistoryByMetricAndPeriod(
                            currentUser,
                            metricType,
                            start,
                            end
                    );
        }

        /*
         * Period only.
         */
        return queryService
                .getPatientHistoryByPeriod(
                        currentUser,
                        start,
                        end
                );
    }
}