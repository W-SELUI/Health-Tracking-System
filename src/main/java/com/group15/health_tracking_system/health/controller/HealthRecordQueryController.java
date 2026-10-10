package com.group15.health_tracking_system.health.controller;

import com.group15.health_tracking_system.health.dto.HealthRecordFilterOption;
import com.group15.health_tracking_system.health.dto.HealthRecordListItem;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.service.HealthRecordHistoryService;
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

    private final HealthRecordHistoryService historyService;
    private final HealthRecordQueryService healthRecordQueryService;
    private final CurrentUserService currentUserService;

    public HealthRecordQueryController(
            HealthRecordHistoryService historyService,
            HealthRecordQueryService healthRecordQueryService,
            CurrentUserService currentUserService) {

        this.historyService = historyService;
        this.healthRecordQueryService = healthRecordQueryService;
        this.currentUserService = currentUserService;
    }

    /**
     * Displays the unified patient health-record history.
     *
     * The history can contain both scalar HealthRecord entries and
     * BloodPressureRecord entries.
     */
    @GetMapping
    public String listRecords(
            @RequestParam(
                    name = "metricType",
                    required = false
            )
            HealthRecordFilterOption metricType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model) {

        User currentUser =
                currentUserService.getCurrentUser();

        List<HealthRecordListItem> records;

        try {

            LocalDateTime start = null;
            LocalDateTime end = null;

            if ((startDate == null) != (endDate == null)) {
                throw new IllegalArgumentException(
                        "Both start date and end date are required"
                );
            }

            if (startDate != null) {
                start = startDate.atStartOfDay();
                end = endDate.atTime(LocalTime.MAX);
            }

            records = historyService.getPatientHistory(
                    currentUser,
                    metricType,
                    start,
                    end
            );

        } catch (IllegalArgumentException exception) {

            model.addAttribute(
                    "filterError",
                    exception.getMessage()
            );

            /*
             * Invalid filters should not prevent the patient from
             * seeing their normal health-record history.
             */
            records = historyService.getPatientHistory(
                    currentUser,
                    null,
                    null,
                    null
            );
        }

        model.addAttribute(
                "records",
                records
        );

        model.addAttribute(
                "filterOptions",
                HealthRecordFilterOption.values()
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
     * Displays one scalar health record belonging to the patient.
     *
     * Blood-pressure detail viewing will use its own route because
     * BloodPressureRecord has an independent database identity.
     */
    @GetMapping("/{recordId}")
    public String viewScalarRecord(
            @PathVariable Long recordId,
            Model model) {

        User currentUser =
                currentUserService.getCurrentUser();

        HealthRecord record =
                healthRecordQueryService.getPatientRecord(
                        recordId,
                        currentUser
                );

        model.addAttribute(
                "record",
                record
        );

        return "health-records/view";
    }
}