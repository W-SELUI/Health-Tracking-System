package com.group15.health_tracking_system.health.controller;

import com.group15.health_tracking_system.health.dto.BloodPressureRecordRequest;
import com.group15.health_tracking_system.health.dto.HealthRecordCreateRequest;
import com.group15.health_tracking_system.health.dto.HealthRecordFilterOption;
import com.group15.health_tracking_system.health.dto.HealthRecordRequest;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.service.BloodPressureCommandService;
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

/**
 * Handles patient-side creation of all health-record types and modification
 * of scalar health records.
 *
 * The create screen is unified for the patient. Internally, scalar
 * measurements are stored as HealthRecord and blood-pressure observations
 * are stored as BloodPressureRecord.
 */
@Controller
@RequestMapping("/patient/health-records")
public class HealthRecordCommandController {

    private final HealthRecordCommandService commandService;
    private final BloodPressureCommandService bloodPressureCommandService;
    private final CurrentUserService currentUserService;

    public HealthRecordCommandController(
            HealthRecordCommandService commandService,
            BloodPressureCommandService bloodPressureCommandService,
            CurrentUserService currentUserService) {

        this.commandService = commandService;
        this.bloodPressureCommandService = bloodPressureCommandService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {

        model.addAttribute(
                "healthRecordCreateRequest",
                new HealthRecordCreateRequest()
        );

        addCreateMetricOptions(model);

        return "health-records/create";
    }

    @PostMapping
    public String createRecord(
            @Valid
            @ModelAttribute("healthRecordCreateRequest")
            HealthRecordCreateRequest request,
            BindingResult bindingResult,
            Model model) {

        validateCreateMeasurement(request, bindingResult);

        if (bindingResult.hasErrors()) {
            addCreateMetricOptions(model);
            return "health-records/create";
        }

        User currentUser = currentUserService.getCurrentUser();

        if (request.getMetricType().isBloodPressure()) {

            BloodPressureRecordRequest bloodPressureRequest =
                    new BloodPressureRecordRequest(
                            request.getSystolic(),
                            request.getDiastolic(),
                            request.getObservedAt(),
                            request.getNotes()
                    );

            bloodPressureCommandService.createPatientRecord(
                    currentUser,
                    bloodPressureRequest
            );

        } else {

            HealthRecordRequest scalarRequest =
                    new HealthRecordRequest(
                            request.getMetricType().getScalarMetricType(),
                            request.getMetricValue(),
                            request.getObservedAt(),
                            request.getNotes()
                    );

            commandService.createPatientRecord(
                    currentUser,
                    scalarRequest
            );
        }

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

        addScalarMetricTypes(model);

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

            addScalarMetricTypes(model);

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

    private void validateCreateMeasurement(
            HealthRecordCreateRequest request,
            BindingResult bindingResult) {

        if (request.getMetricType() == null) {
            return;
        }

        if (request.getMetricType().isBloodPressure()) {

            if (request.getSystolic() == null) {
                bindingResult.rejectValue(
                        "systolic",
                        "required.systolic",
                        "Systolic value is required"
                );
            }

            if (request.getDiastolic() == null) {
                bindingResult.rejectValue(
                        "diastolic",
                        "required.diastolic",
                        "Diastolic value is required"
                );
            }

            return;
        }

        if (request.getMetricValue() == null) {
            bindingResult.rejectValue(
                    "metricValue",
                    "required.metricValue",
                    "Measurement value is required"
            );
        }
    }

    private void addCreateMetricOptions(Model model) {
        model.addAttribute(
                "metricOptions",
                HealthRecordFilterOption.values()
        );
    }

    private void addScalarMetricTypes(Model model) {
        model.addAttribute(
                "metricTypes",
                HealthMetricType.values()
        );
    }
}
