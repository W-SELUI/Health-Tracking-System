package com.group15.health_tracking_system.health.controller;

import com.group15.health_tracking_system.health.validation.HealthMeasurementRules;
import com.group15.health_tracking_system.consent.service.ProviderHealthRecordService;
import com.group15.health_tracking_system.health.dto.BloodPressureRecordRequest;
import com.group15.health_tracking_system.health.dto.HealthRecordCreateRequest;
import com.group15.health_tracking_system.health.dto.HealthRecordFilterOption;
import com.group15.health_tracking_system.health.dto.HealthRecordRequest;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.service.HealthRecordChangeRequestService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Handles Provider submission of Patient Health Record change proposals.
 *
 * These routes never directly create or edit permanent Health Records.
 * Every submitted change is stored as a PENDING
 * HealthRecordChangeRequest and must later be approved by the affected
 * Patient.
 *
 * DELETE is intentionally unsupported.
 */
@Controller
@RequestMapping(
        "/provider/patients/{patientId}/health-record-proposals"
)
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderHealthRecordProposalController {

    private final HealthRecordChangeRequestService changeRequestService;

    private final ProviderHealthRecordService providerHealthRecordService;

    public ProviderHealthRecordProposalController(
            HealthRecordChangeRequestService changeRequestService,
            ProviderHealthRecordService providerHealthRecordService) {

        this.changeRequestService =
                changeRequestService;

        this.providerHealthRecordService =
                providerHealthRecordService;
    }


    /* =========================================================
       CREATE PROPOSAL
       ========================================================= */

    /**
     * Displays one unified proposal form for scalar measurements and
     * Blood Pressure.
     */
    @GetMapping("/new")
    public String showCreateProposalForm(
            @PathVariable Long patientId,
            Model model) {

        /*
         * Also performs ACTIVE Consent authorization before exposing
         * patient information.
         */
        addPatient(
                patientId,
                model
        );

        model.addAttribute(
                "healthRecordCreateRequest",
                new HealthRecordCreateRequest()
        );

        addCreateMetricOptions(
                model
        );

        return "consent/provider-health-record-proposal-create";
    }

    /**
     * Submits a CREATE proposal.
     *
     * The proposal becomes PENDING. No permanent HealthRecord or
     * BloodPressureRecord is created at this point.
     */
    @PostMapping("/new")
    public String submitCreateProposal(
            @PathVariable Long patientId,

            @Valid
            @ModelAttribute("healthRecordCreateRequest")
            HealthRecordCreateRequest request,

            BindingResult bindingResult,
            Model model) {

        validateCreateMeasurement(
                request,
                bindingResult
        );

        if (bindingResult.hasErrors()) {

            addPatient(
                    patientId,
                    model
            );

            addCreateMetricOptions(
                    model
            );

            return "consent/provider-health-record-proposal-create";
        }

        try {

            if (request.getMetricType()
                    .isBloodPressure()) {

                BloodPressureRecordRequest bloodPressureRequest =
                        new BloodPressureRecordRequest(
                                request.getSystolic(),
                                request.getDiastolic(),
                                request.getObservedAt(),
                                request.getNotes()
                        );

                changeRequestService
                        .proposeBloodPressureCreate(
                                patientId,
                                bloodPressureRequest
                        );

            } else {

                HealthRecordRequest scalarRequest =
                        new HealthRecordRequest(
                                request.getMetricType()
                                        .getScalarMetricType(),
                                request.getMetricValue(),
                                request.getObservedAt(),
                                request.getNotes()
                        );

                changeRequestService
                        .proposeScalarCreate(
                                patientId,
                                scalarRequest
                        );
            }

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            bindingResult.reject(
                    "proposalError",
                    exception.getMessage()
            );

            addPatient(
                    patientId,
                    model
            );

            addCreateMetricOptions(
                    model
            );

            return "consent/provider-health-record-proposal-create";
        }

        return "redirect:/provider/patients/"
                + patientId
                + "/health-records?proposalSubmitted";
    }


    /* =========================================================
       SCALAR UPDATE PROPOSAL
       ========================================================= */

    /**
     * Displays a proposal form pre-populated with an existing scalar
     * Health Record.
     */
    @GetMapping("/scalar/{recordId}/edit")
    public String showScalarUpdateProposalForm(
            @PathVariable Long patientId,
            @PathVariable Long recordId,
            Model model) {

        HealthRecord record =
                providerHealthRecordService
                        .getPatientRecord(
                                patientId,
                                recordId
                        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        record.getMetricType(),
                        record.getMetricValue(),
                        record.getObservedAt(),
                        record.getNotes()
                );

        addPatient(
                patientId,
                model
        );

        model.addAttribute(
                "record",
                record
        );

        model.addAttribute(
                "recordId",
                recordId
        );

        model.addAttribute(
                "healthRecordRequest",
                request
        );

        addScalarMetricTypes(
                model
        );

        return "consent/provider-health-record-proposal-scalar-edit";
    }

    /**
     * Submits a proposed correction to an existing scalar Health Record.
     */
    @PostMapping("/scalar/{recordId}/edit")
    public String submitScalarUpdateProposal(
            @PathVariable Long patientId,
            @PathVariable Long recordId,

            @Valid
            @ModelAttribute("healthRecordRequest")
            HealthRecordRequest request,

            BindingResult bindingResult,
            Model model) {

        HealthMeasurementRules.checkScalarValue(
                request.getMetricType(),
                request.getMetricValue(),
                bindingResult,
                "metricValue"
        );

        if (bindingResult.hasErrors()) {

            populateScalarEditModel(
                    patientId,
                    recordId,
                    model
            );

            return "consent/provider-health-record-proposal-scalar-edit";
        }

        try {

            changeRequestService
                    .proposeScalarUpdate(
                            patientId,
                            recordId,
                            request
                    );

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            bindingResult.reject(
                    "proposalError",
                    exception.getMessage()
            );

            populateScalarEditModel(
                    patientId,
                    recordId,
                    model
            );

            return "consent/provider-health-record-proposal-scalar-edit";
        }

        return "redirect:/provider/patients/"
                + patientId
                + "/health-records?proposalSubmitted";
    }


    /* =========================================================
       BLOOD PRESSURE UPDATE PROPOSAL
       ========================================================= */

    /**
     * Displays a proposal form for an existing paired Blood Pressure
     * observation.
     */
    @GetMapping("/blood-pressure/{recordId}/edit")
    public String showBloodPressureUpdateProposalForm(
            @PathVariable Long patientId,
            @PathVariable Long recordId,
            Model model) {

        BloodPressureRecord record =
                providerHealthRecordService
                        .getPatientBloodPressureRecord(
                                patientId,
                                recordId
                        );

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        record.getSystolic(),
                        record.getDiastolic(),
                        record.getObservedAt(),
                        record.getNotes()
                );

        addPatient(
                patientId,
                model
        );

        model.addAttribute(
                "record",
                record
        );

        model.addAttribute(
                "recordId",
                recordId
        );

        model.addAttribute(
                "bloodPressureRequest",
                request
        );

        return "consent/provider-health-record-proposal-blood-pressure-edit";
    }

    /**
     * Submits a proposed correction to an existing Blood Pressure record.
     */
    @PostMapping("/blood-pressure/{recordId}/edit")
    public String submitBloodPressureUpdateProposal(
            @PathVariable Long patientId,
            @PathVariable Long recordId,

            @Valid
            @ModelAttribute("bloodPressureRequest")
            BloodPressureRecordRequest request,

            BindingResult bindingResult,
            Model model) {

        HealthMeasurementRules.checkBloodPressure(
                request.getSystolic(),
                request.getDiastolic(),
                bindingResult
        );

        if (bindingResult.hasErrors()) {

            populateBloodPressureEditModel(
                    patientId,
                    recordId,
                    model
            );

            return "consent/provider-health-record-proposal-blood-pressure-edit";
        }

        try {

            changeRequestService
                    .proposeBloodPressureUpdate(
                            patientId,
                            recordId,
                            request
                    );

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            bindingResult.reject(
                    "proposalError",
                    exception.getMessage()
            );

            populateBloodPressureEditModel(
                    patientId,
                    recordId,
                    model
            );

            return "consent/provider-health-record-proposal-blood-pressure-edit";
        }

        return "redirect:/provider/patients/"
                + patientId
                + "/health-records?proposalSubmitted";
    }


    /* =========================================================
       MODEL HELPERS
       ========================================================= */

    private void populateScalarEditModel(
            Long patientId,
            Long recordId,
            Model model) {

        addPatient(
                patientId,
                model
        );

        HealthRecord record =
                providerHealthRecordService
                        .getPatientRecord(
                                patientId,
                                recordId
                        );

        model.addAttribute(
                "record",
                record
        );

        model.addAttribute(
                "recordId",
                recordId
        );

        addScalarMetricTypes(
                model
        );
    }

    private void populateBloodPressureEditModel(
            Long patientId,
            Long recordId,
            Model model) {

        addPatient(
                patientId,
                model
        );

        BloodPressureRecord record =
                providerHealthRecordService
                        .getPatientBloodPressureRecord(
                                patientId,
                                recordId
                        );

        model.addAttribute(
                "record",
                record
        );

        model.addAttribute(
                "recordId",
                recordId
        );
    }

    private void addPatient(
            Long patientId,
            Model model) {

        model.addAttribute(
                "patient",
                providerHealthRecordService
                        .getPatient(
                                patientId
                        )
        );
    }

    private void addCreateMetricOptions(
            Model model) {

        model.addAttribute(
                "metricOptions",
                HealthRecordFilterOption.values()
        );
    }

    private void addScalarMetricTypes(
            Model model) {

        model.addAttribute(
                "metricTypes",
                HealthMetricType.values()
        );
    }


    /* =========================================================
       CREATE VALIDATION
       ========================================================= */

    private void validateCreateMeasurement(
            HealthRecordCreateRequest request,
            BindingResult bindingResult) {

        if (request.getMetricType() == null) {
            return;
        }

        if (request.getMetricType()
                .isBloodPressure()) {

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

            HealthMeasurementRules.checkBloodPressure(
                    request.getSystolic(),
                    request.getDiastolic(),
                    bindingResult
            );

            return;
        }

        if (request.getMetricValue() == null) {

            bindingResult.rejectValue(
                    "metricValue",
                    "required.metricValue",
                    "Measurement value is required"
            );
        }

        HealthMeasurementRules.checkScalarValue(
                request.getMetricType().getScalarMetricType(),
                request.getMetricValue(),
                bindingResult,
                "metricValue"
        );
    }
}