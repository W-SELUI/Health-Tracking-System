package com.group15.health_tracking_system.consent.controller;

import com.group15.health_tracking_system.consent.service.ProviderHealthRecordService;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Displays patient health records to providers with active consent.
 */
@Controller
@RequestMapping("/provider/patients/{patientId}/health-records")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderHealthRecordController {

    private final ProviderHealthRecordService recordService;
    private final CurrentUserService currentUserService;

    public ProviderHealthRecordController(
            ProviderHealthRecordService recordService,
            CurrentUserService currentUserService
    ) {
        this.recordService = recordService;
        this.currentUserService = currentUserService;
    }

    /**
     * Displays the selected patient's health-record history.
     */
    @GetMapping
    public String showHistory(
            @PathVariable("patientId") Long patientId,
            Model model
    ) {
        model.addAttribute(
                "patient",
                recordService.getPatient(patientId)
        );

        model.addAttribute(
                "records",
                recordService.getPatientHistory(patientId)
        );

        model.addAttribute(
                "currentUser",
                currentUserService.getCurrentUser()
        );

        return "consent/provider-health-records";
    }

    /**
     * Displays a single record belonging to the selected patient.
     */
    @GetMapping("/{recordId}")
    public String showRecord(
            @PathVariable("patientId") Long patientId,
            @PathVariable("recordId") Long recordId,
            Model model
    ) {
        model.addAttribute(
                "patient",
                recordService.getPatient(patientId)
        );

        model.addAttribute(
                "record",
                recordService.getPatientRecord(patientId, recordId)
        );

        model.addAttribute(
                "currentUser",
                currentUserService.getCurrentUser()
        );

        return "consent/provider-health-record";
    }
}