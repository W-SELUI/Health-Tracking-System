package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.user.entity.ProviderVerification;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.entity.VerificationStatus;
import com.group15.health_tracking_system.user.service.AdminService;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import com.group15.health_tracking_system.user.service.ProviderVerificationService;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Handles shared navigation and role-specific dashboard routing.

@Controller
public class HomeController {

    private final CurrentUserService currentUserService;
    private final AdminService adminService;
    private final ProviderVerificationService verificationService;

    public HomeController(
            CurrentUserService currentUserService,
            AdminService adminService,
            ProviderVerificationService verificationService) {

        this.currentUserService = currentUserService;
        this.adminService = adminService;
        this.verificationService = verificationService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {

        User currentUser =
                currentUserService.getCurrentUser();

        return switch (currentUser.getRole()) {

            case PATIENT ->
                    "redirect:/patient/dashboard";

            case PROVIDER ->
                    getProviderRedirect(currentUser);

            case ADMIN ->
                    "redirect:/admin/dashboard";
        };
    }

    /*
     * Determines where a provider should go after login.
     */
    private String getProviderRedirect(
            User provider) {

        Optional<ProviderVerification> verification =
                verificationService
                        .getByProviderId(
                                provider.getId());

        /*
         * No verification record means the provider
         * cannot access the normal provider dashboard.
         */
        if (verification.isEmpty()) {

            return "redirect:/provider/verification";
        }

        VerificationStatus status =
                verification
                        .get()
                        .getStatus();

        /*
         * Only verified providers can enter
         * the normal provider dashboard.
         */
        if (status == VerificationStatus.VERIFIED) {

            return "redirect:/provider/dashboard";
        }

        /*
         * Pending and rejected providers are sent
         * to the verification status page.
         */
        return "redirect:/provider/verification";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(
            Model model) {

        model.addAttribute(
                "currentUser",
                currentUserService.getCurrentUser());

        return "dashboard/patient";
    }

    @GetMapping("/provider/dashboard")
    public String providerDashboard(
            Model model) {

        User currentUser =
                currentUserService.getCurrentUser();

        Optional<ProviderVerification> verification =
                verificationService
                        .getByProviderId(
                                currentUser.getId());

        /*
         * Prevent pending or rejected providers
         * from opening the dashboard directly.
         */
        if (verification.isEmpty()
                || verification.get().getStatus()
                != VerificationStatus.VERIFIED) {

            return "redirect:/provider/verification";
        }

        model.addAttribute(
                "currentUser",
                currentUser);

        return "dashboard/provider";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            Model model) {

        model.addAttribute(
                "currentUser",
                currentUserService.getCurrentUser());

        model.addAttribute(
                "totalUsers",
                adminService.countUsers());

        model.addAttribute(
                "totalPatients",
                adminService.countPatients());

        model.addAttribute(
                "totalProviders",
                adminService.countProviders());

        model.addAttribute(
                "enabledUsers",
                adminService.countEnabledUsers());

        model.addAttribute(
                "disabledUsers",
                adminService.countDisabledUsers());

        model.addAttribute(
                "pendingVerifications",
                verificationService
                        .getPending()
                        .size());

        return "dashboard/admin";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}