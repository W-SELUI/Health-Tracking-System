package com.group15.health_tracking_system.health.controller;

import com.group15.health_tracking_system.health.dto.BloodPressureRecordRequest;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.service.BloodPressureCommandService;
import com.group15.health_tracking_system.health.service.BloodPressureQueryService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Handles patient-side viewing, editing and deleting of blood-pressure
 * records. Creation is handled by the unified Add Health Record flow.
 */
@Controller
@RequestMapping("/patient/health-records/blood-pressure")
public class BloodPressureController {

    private final BloodPressureCommandService commandService;
    private final BloodPressureQueryService queryService;
    private final CurrentUserService currentUserService;

    public BloodPressureController(
            BloodPressureCommandService commandService,
            BloodPressureQueryService queryService,
            CurrentUserService currentUserService) {

        this.commandService = commandService;
        this.queryService = queryService;
        this.currentUserService = currentUserService;
    }

    /**
     * Displays one blood-pressure record belonging to the patient.
     */
    @GetMapping("/{recordId}")
    public String viewRecord(
            @PathVariable Long recordId,
            Model model) {

        User currentUser =
                currentUserService.getCurrentUser();

        BloodPressureRecord record =
                queryService.getPatientRecord(
                        recordId,
                        currentUser
                );

        model.addAttribute(
                "record",
                record
        );

        return "health-records/blood-pressure-view";
    }

    /**
     * Displays the edit form for a patient-editable
     * blood-pressure record.
     */
    @GetMapping("/{recordId}/edit")
    public String showEditForm(
            @PathVariable Long recordId,
            Model model) {

        User currentUser =
                currentUserService.getCurrentUser();

        BloodPressureRecordRequest request =
                commandService.createUpdateRequest(
                        recordId,
                        currentUser
                );

        model.addAttribute(
                "bloodPressureRequest",
                request
        );

        model.addAttribute(
                "recordId",
                recordId
        );

        return "health-records/blood-pressure-edit";
    }

    /**
     * Updates one patient-created blood-pressure observation.
     */
    @PostMapping("/{recordId}/edit")
    public String updateRecord(
            @PathVariable Long recordId,

            @Valid
            @ModelAttribute("bloodPressureRequest")
            BloodPressureRecordRequest request,

            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "recordId",
                    recordId
            );

            return "health-records/blood-pressure-edit";
        }

        User currentUser =
                currentUserService.getCurrentUser();

        commandService.updatePatientRecord(
                recordId,
                currentUser,
                request
        );

        return "redirect:/patient/health-records?updated";
    }

    /**
     * Deletes one patient-created blood-pressure observation.
     */
    @PostMapping("/{recordId}/delete")
    public String deleteRecord(
            @PathVariable Long recordId) {

        User currentUser =
                currentUserService.getCurrentUser();

        commandService.deletePatientRecord(
                recordId,
                currentUser
        );

        return "redirect:/patient/health-records?deleted";
    }

    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbiddenModification() {

        return "error/access-denied";
    }
}