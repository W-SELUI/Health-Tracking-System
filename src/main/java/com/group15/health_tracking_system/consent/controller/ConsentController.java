package com.group15.health_tracking_system.consent.controller;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.entity.Consent;
import com.group15.health_tracking_system.consent.entity.ConsentStatus;
import com.group15.health_tracking_system.consent.entity.ProviderAccessRequest;
import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.consent.service.ProviderAccessRequestService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the patient's provider-access management page.
 *
 * Patients can review incoming provider access requests, approve or reject
 * those requests, inspect active provider access and revoke previously
 * granted access.
 *
 * Providers can no longer be granted access directly by the patient without
 * first submitting a ProviderAccessRequest. Approval of a pending request is
 * the only normal workflow that creates or reactivates Consent.
 */
@Controller
@RequestMapping("/patient/consents")
@PreAuthorize("hasRole('PATIENT')")
public class ConsentController {

    private final ConsentService consentService;
    private final ProviderAccessRequestService requestService;
    private final CurrentUserService currentUserService;

    public ConsentController(
            ConsentService consentService,
            ProviderAccessRequestService requestService,
            CurrentUserService currentUserService) {

        this.consentService = consentService;
        this.requestService = requestService;
        this.currentUserService = currentUserService;
    }

    /**
     * Displays the patient's provider-access workspace.
     *
     * @param model Thymeleaf model
     * @return provider access template
     */
    @GetMapping
    public String showAccessPage(
            Model model) {

        User patient =
                currentUserService.getCurrentUser();

        List<Consent> consents =
                consentService.getPatientConsents(
                        patient.getId()
                );

        List<Consent> activeConsents =
                consents.stream()
                        .filter(consent ->
                                consent.getStatus()
                                        == ConsentStatus.ACTIVE
                        )
                        .toList();

        List<Consent> revokedConsents =
                consents.stream()
                        .filter(consent ->
                                consent.getStatus()
                                        == ConsentStatus.REVOKED
                        )
                        .toList();

        List<ProviderAccessRequest> pendingRequests =
                requestService
                        .getPendingRequestsForCurrentPatient();

        List<ProviderAccessRequest> requestHistory =
                requestService
                        .getRequestHistoryForCurrentPatient();

        /*
         * Useful for the new patient-access template and also keeps the
         * currently existing template compatible until it is replaced.
         */
        Map<Long, String> consentStatuses =
                new HashMap<>();

        for (Consent consent : consents) {

            consentStatuses.put(
                    consent.getProvider().getId(),
                    consent.getStatus().name()
            );
        }

        Set<Long> pendingProviderIds =
                pendingRequests.stream()
                        .map(request ->
                                request.getProvider().getId()
                        )
                        .collect(
                                Collectors.toSet()
                        );

        model.addAttribute(
                "currentUser",
                patient
        );

        /*
         * Retained temporarily for compatibility with William's original
         * access.html template. The replacement template will primarily use
         * pendingRequests, activeConsents and requestHistory.
         */
        model.addAttribute(
                "providers",
                consentService.getAvailableProviders()
        );

        model.addAttribute(
                "consentStatuses",
                consentStatuses
        );

        model.addAttribute(
                "pendingRequests",
                pendingRequests
        );

        model.addAttribute(
                "pendingProviderIds",
                pendingProviderIds
        );

        model.addAttribute(
                "activeConsents",
                activeConsents
        );

        model.addAttribute(
                "revokedConsents",
                revokedConsents
        );

        model.addAttribute(
                "requestHistory",
                requestHistory
        );

        return "consent/access";
    }

    /**
     * Approves a pending provider access request belonging to the
     * authenticated patient.
     *
     * Approval causes the request to become APPROVED and activates the
     * corresponding Consent in the same transaction inside the service
     * layer.
     *
     * @param requestId access request identifier
     * @param redirectAttributes flash-message container
     * @return redirect to patient provider-access page
     */
    @PostMapping("/access-requests/{requestId}/approve")
    public String approveAccessRequest(
            @PathVariable Long requestId,
            RedirectAttributes redirectAttributes) {

        try {

            requestService.approveRequest(
                    requestId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Provider access request approved."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException
                | ResourceNotFoundException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/patient/consents";
    }

    /**
     * Rejects a pending provider access request belonging to the
     * authenticated patient.
     *
     * @param requestId access request identifier
     * @param redirectAttributes flash-message container
     * @return redirect to patient provider-access page
     */
    @PostMapping("/access-requests/{requestId}/reject")
    public String rejectAccessRequest(
            @PathVariable Long requestId,
            RedirectAttributes redirectAttributes) {

        try {

            requestService.rejectRequest(
                    requestId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Provider access request rejected."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException
                | ResourceNotFoundException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/patient/consents";
    }

    /**
     * Revokes currently active provider access.
     *
     * Revocation affects current Consent only. Historical access requests
     * remain unchanged so that the audit history is preserved.
     *
     * @param providerId provider whose access is being revoked
     * @param redirectAttributes flash-message container
     * @return redirect to patient provider-access page
     */
    @PostMapping("/{providerId}/revoke")
    public String revokeConsent(
            @PathVariable Long providerId,
            RedirectAttributes redirectAttributes) {

        try {

            Long patientId =
                    currentUserService
                            .getCurrentUser()
                            .getId();

            consentService.revokeConsent(
                    patientId,
                    providerId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Provider access revoked."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException
                | ResourceNotFoundException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/patient/consents";
    }

    /**
     * Legacy direct-grant endpoint.
     *
     * Direct access granting is intentionally disabled. Providers must first
     * submit an access request and the patient must approve that specific
     * request. Keeping this temporary endpoint prevents the old template from
     * producing a server error while the UI is being replaced, but it never
     * grants Consent.
     *
     * @param providerId ignored provider identifier from the legacy route
     * @param redirectAttributes flash-message container
     * @return redirect to provider-access page
     */
    @PostMapping("/{providerId}/grant")
    public String legacyGrantConsent(
            @PathVariable Long providerId,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Direct access granting is no longer available. "
                + "The provider must submit an access request first."
        );

        return "redirect:/patient/consents";
    }
}