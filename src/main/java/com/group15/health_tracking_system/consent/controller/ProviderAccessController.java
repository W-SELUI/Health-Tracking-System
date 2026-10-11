package com.group15.health_tracking_system.consent.controller;

import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/provider/patients")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderAccessController {

    private final ConsentService consentService;
    private final CurrentUserService currentUserService;

    public ProviderAccessController(
            ConsentService consentService,
            CurrentUserService currentUserService
    ) {
        this.consentService = consentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String showAuthorizedPatients(Model model) {
        User provider = currentUserService.getCurrentUser();

        model.addAttribute("currentUser", provider);
        model.addAttribute(
                "patients",
                consentService.getAuthorizedPatients(provider.getId())
        );

        return "consent/provider-patients";
    }
}