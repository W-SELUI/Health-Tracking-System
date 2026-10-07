package com.group15.health_tracking_system.consent.controller;

import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @PostMapping("/{providerId}/grant")
    public String grantConsent(@PathVariable Long providerId) {
        Long patientId = currentUserService.getCurrentUser().getId();

        consentService.grantConsent(patientId, providerId);

        return "redirect:/patient/dashboard";
    }

    @PostMapping("/{providerId}/revoke")
    public String revokeConsent(@PathVariable Long providerId) {
        Long patientId = currentUserService.getCurrentUser().getId();

        consentService.revokeConsent(patientId, providerId);

        return "redirect:/patient/dashboard";
    }
}