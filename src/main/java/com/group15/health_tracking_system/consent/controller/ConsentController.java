package com.group15.health_tracking_system.consent.controller;

import com.group15.health_tracking_system.consent.entity.Consent;
import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/patient/consents")
@PreAuthorize("hasRole('PATIENT')")
public class ConsentController {

    private final ConsentService consentService;
    private final CurrentUserService currentUserService;

    public ConsentController(
            ConsentService consentService,
            CurrentUserService currentUserService
    ) {
        this.consentService = consentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String showAccessPage(Model model) {
        User patient = currentUserService.getCurrentUser();

        Map<Long, String> consentStatuses = new HashMap<>();

        for (Consent consent
                : consentService.getPatientConsents(patient.getId())) {
            consentStatuses.put(
                    consent.getProvider().getId(),
                    consent.getStatus().name()
            );
        }

        model.addAttribute("currentUser", patient);
        model.addAttribute(
                "providers",
                consentService.getAvailableProviders()
        );
        model.addAttribute("consentStatuses", consentStatuses);

        return "consent/access";
    }

    @PostMapping("/{providerId}/grant")
    public String grantConsent(
            @PathVariable Long providerId,
            RedirectAttributes redirectAttributes
    ) {
        Long patientId = currentUserService.getCurrentUser().getId();

        consentService.grantConsent(patientId, providerId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Provider access granted."
        );

        return "redirect:/patient/consents";
    }

    @PostMapping("/{providerId}/revoke")
    public String revokeConsent(
            @PathVariable Long providerId,
            RedirectAttributes redirectAttributes
    ) {
        Long patientId = currentUserService.getCurrentUser().getId();

        consentService.revokeConsent(patientId, providerId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Provider access revoked."
        );

        return "redirect:/patient/consents";
    }
}