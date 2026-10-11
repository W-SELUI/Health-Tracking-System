package com.group15.health_tracking_system.consent.controller;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.entity.ProviderAccessRequest;
import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.consent.service.ProviderAccessRequestService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
 * Handles the healthcare provider's patient-access workspace.
 *
 * Providers can:
 *
 * 
 *
 * Submitting a request never grants access directly. Access exists only
 * after the affected patient approves the request and active Consent is
 * created or reactivated.
 */
@Controller
@RequestMapping("/provider/patients")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderAccessController {

    private final ConsentService consentService;
    private final ProviderAccessRequestService requestService;
    private final CurrentUserService currentUserService;

    public ProviderAccessController(
            ConsentService consentService,
            ProviderAccessRequestService requestService,
            CurrentUserService currentUserService) {

        this.consentService = consentService;
        this.requestService = requestService;
        this.currentUserService = currentUserService;
    }

    /**
     * Displays the provider's patient-access workspace.
     *
     * The existing {@code patients} model attribute is retained for
     * compatibility with the current provider-patients template until
     * that template is upgraded in the next step.
     *
     * @param model Thymeleaf model
     * @return provider patient-access template
     */
    @GetMapping
    public String showAuthorizedPatients(
            Model model) {

        User provider =
                currentUserService.getCurrentUser();

        List<User> authorizedPatients =
                consentService.getAuthorizedPatients(
                        provider.getId()
                );

        List<User> availablePatients =
                requestService.getAvailablePatients();

        List<ProviderAccessRequest> pendingRequests =
                requestService
                        .getPendingRequestsForCurrentProvider();

        List<ProviderAccessRequest> requestHistory =
                requestService
                        .getRequestHistoryForCurrentProvider();

        /*
         * Allows the template to identify patients who already have active
         * Consent without repeatedly querying the database.
         */
        Set<Long> authorizedPatientIds =
                authorizedPatients
                        .stream()
                        .map(User::getId)
                        .collect(
                                Collectors.toSet()
                        );

        /*
         * Maps:
         *
         * patientId -> pending request
         *
         * The service layer already guarantees that only one pending request
         * may exist for any provider/patient pair.
         */
        Map<Long, ProviderAccessRequest>
                pendingRequestByPatientId =
                pendingRequests
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        request ->
                                                request
                                                        .getPatient()
                                                        .getId(),
                                        Function.identity()
                                )
                        );

        model.addAttribute(
                "currentUser",
                provider
        );

        /*
         * Keep the original model name so the existing template remains
         * functional before we replace it in the next step.
         */
        model.addAttribute(
                "patients",
                authorizedPatients
        );

        model.addAttribute(
                "authorizedPatients",
                authorizedPatients
        );

        model.addAttribute(
                "availablePatients",
                availablePatients
        );

        model.addAttribute(
                "authorizedPatientIds",
                authorizedPatientIds
        );

        model.addAttribute(
                "pendingRequests",
                pendingRequests
        );

        model.addAttribute(
                "pendingRequestByPatientId",
                pendingRequestByPatientId
        );

        model.addAttribute(
                "requestHistory",
                requestHistory
        );

        return "consent/provider-patients";
    }

    /**
     * Submits a patient-access request on behalf of the authenticated
     * provider.
     *
     * @param patientId patient being requested
     * @param redirectAttributes flash-message container
     * @return redirect to provider patient-access workspace
     */
    @PostMapping("/{patientId}/request-access")
    public String requestAccess(
            @PathVariable Long patientId,
            RedirectAttributes redirectAttributes) {

        try {

            requestService.requestAccess(
                    patientId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Access request sent to the patient."
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

        return "redirect:/provider/patients";
    }

    /**
     * Cancels an unresolved request belonging to the authenticated
     * provider.
     *
     * @param requestId access-request identifier
     * @param redirectAttributes flash-message container
     * @return redirect to provider patient-access workspace
     */
    @PostMapping("/access-requests/{requestId}/cancel")
    public String cancelAccessRequest(
            @PathVariable Long requestId,
            RedirectAttributes redirectAttributes) {

        try {

            requestService.cancelRequest(
                    requestId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Access request cancelled."
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

        return "redirect:/provider/patients";
    }
}