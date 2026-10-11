package com.group15.health_tracking_system.health.controller;

import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequest;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequestStatus;
import com.group15.health_tracking_system.health.service.HealthRecordChangeRequestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Handles Patient review of Provider-proposed Health Record changes.
 *
 * Patients can review pending proposals, approve them, reject them,
 * and view the resulting request history.
 */
@Controller
@RequestMapping("/patient/health-record-change-requests")
@PreAuthorize("hasRole('PATIENT')")
public class PatientHealthRecordChangeRequestController {

    private final HealthRecordChangeRequestService changeRequestService;

    public PatientHealthRecordChangeRequestController(
            HealthRecordChangeRequestService changeRequestService) {

        this.changeRequestService =
                changeRequestService;
    }

    /**
     * Displays pending Provider proposals and the Patient's proposal history.
     */
    @GetMapping
    public String showRequests(
            Model model) {

        model.addAttribute(
                "pendingRequests",
                changeRequestService
                        .getPendingRequestsForCurrentPatient()
        );

        model.addAttribute(
                "requestHistory",
                changeRequestService
                        .getRequestHistoryForCurrentPatient()
        );

        return "health-records/change-requests";
    }

    /**
     * Approves a pending Provider proposal.
     *
     * Approval may result in APPROVED, STALE, or CANCELLED depending
     * on the current state of the target record and Provider Consent.
     */
    @PostMapping("/{requestId}/approve")
    public String approveRequest(
            @PathVariable Long requestId) {

        HealthRecordChangeRequest result =
                changeRequestService
                        .approveRequest(
                                requestId
                        );

        if (result.getStatus()
                == HealthRecordChangeRequestStatus.APPROVED) {

            return "redirect:/patient/health-record-change-requests?approved";
        }

        if (result.getStatus()
                == HealthRecordChangeRequestStatus.STALE) {

            return "redirect:/patient/health-record-change-requests?stale";
        }

        if (result.getStatus()
                == HealthRecordChangeRequestStatus.CANCELLED) {

            return "redirect:/patient/health-record-change-requests?cancelled";
        }

        return "redirect:/patient/health-record-change-requests";
    }

    /**
     * Rejects a pending Provider proposal.
     */
    @PostMapping("/{requestId}/reject")
    public String rejectRequest(
            @PathVariable Long requestId) {

        changeRequestService
                .rejectRequest(
                        requestId
                );

        return "redirect:/patient/health-record-change-requests?rejected";
    }
}