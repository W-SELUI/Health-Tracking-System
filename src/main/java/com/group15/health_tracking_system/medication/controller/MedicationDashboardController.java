package com.group15.health_tracking_system.medication.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MedicationDashboardController {

    @GetMapping("/medication-dashboard")
    public String medicationDashboard() {
        return "medication-dashboard";
    }
}