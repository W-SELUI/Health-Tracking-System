package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Handles shared navigation and role-specific dashboard routing.

@Controller
public class HomeController {

    private final CurrentUserService currentUserService;

    public HomeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        User currentUser = currentUserService.getCurrentUser();

        return switch (currentUser.getRole()) {
            case PATIENT -> "redirect:/patient/dashboard";
            case PROVIDER -> "redirect:/provider/dashboard";
            case ADMIN -> "redirect:/admin/dashboard";
        };
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(Model model) {
        model.addAttribute("currentUser", currentUserService.getCurrentUser());
        return "dashboard/patient";
    }

    @GetMapping("/provider/dashboard")
    public String providerDashboard(Model model) {
        model.addAttribute("currentUser", currentUserService.getCurrentUser());
        return "dashboard/provider";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("currentUser", currentUserService.getCurrentUser());
        return "dashboard/admin";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}