package com.group15.health_tracking_system.health.controller;

import com.group15.health_tracking_system.health.dto.HealthRecordRequest;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.service.HealthRecordCommandService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/patient/health-records")
public class HealthRecordCommandController {

    private final HealthRecordCommandService commandService;
    private final CurrentUserService currentUserService;

    public HealthRecordCommandController(
            HealthRecordCommandService commandService,
            CurrentUserService currentUserService) {

        this.commandService = commandService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {

        model.addAttribute(
                "healthRecordRequest",
                new HealthRecordRequest()
        );

        addMetricTypes(model);

        return "health-records/create";
    }

    @PostMapping
    public String createRecord(
            @Valid
            @ModelAttribute("healthRecordRequest")
            HealthRecordRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            addMetricTypes(model);
            return "health-records/create";
        }

        User currentUser = currentUserService.getCurrentUser();

        commandService.createPatientRecord(
                currentUser,
                request
        );

        return "redirect:/patient/health-records?created";
    }

    @GetMapping("/{recordId}/edit")
    public String showEditForm(
            @PathVariable Long recordId,
            Model model) {

        User currentUser = currentUserService.getCurrentUser();

        HealthRecordRequest request =
                commandService.createUpdateRequest(
                        recordId,
                        currentUser
                );

        model.addAttribute(
                "healthRecordRequest",
                request
        );

        model.addAttribute(
                "recordId",
                recordId
        );

        addMetricTypes(model);

        return "health-records/edit";
    }

    @PostMapping("/{recordId}/edit")
    public String updateRecord(
            @PathVariable Long recordId,
            @Valid
            @ModelAttribute("healthRecordRequest")
            HealthRecordRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "recordId",
                    recordId
            );

            addMetricTypes(model);

            return "health-records/edit";
        }

        User currentUser = currentUserService.getCurrentUser();

        commandService.updatePatientRecord(
                recordId,
                currentUser,
                request
        );

        return "redirect:/patient/health-records?updated";
    }

    @PostMapping("/{recordId}/delete")
    public String deleteRecord(
            @PathVariable Long recordId) {

        User currentUser = currentUserService.getCurrentUser();

        commandService.deletePatientRecord(
                recordId,
                currentUser
        );

        return "redirect:/patient/health-records?deleted";
    }

    private void addMetricTypes(Model model) {
        model.addAttribute(
                "metricTypes",
                HealthMetricType.values()
        );
    }
}