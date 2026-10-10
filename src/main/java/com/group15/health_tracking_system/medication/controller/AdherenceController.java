package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.service.AdherenceService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdherenceController {

    private final AdherenceService adherenceService;
    private final UserRepository userRepository;

    public AdherenceController(
            AdherenceService adherenceService,
            UserRepository userRepository) {
        this.adherenceService = adherenceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/adherence")
    public String adherence(
            Model model,
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user was not found."));

        Long userId = user.getId();

        Map<String, Long> counts;
        double adherenceRate;

        boolean hasStartDate = startDate != null;
        boolean hasEndDate = endDate != null;

        if (hasStartDate != hasEndDate) {
            model.addAttribute(
                    "filterError",
                    "Please select both a start date and an end date.");

            counts = adherenceService.getDoseCounts(userId);
            adherenceRate =
                    adherenceService.calculateAdherence(userId);

        } else if (hasStartDate) {

            if (startDate.isAfter(endDate)) {
                model.addAttribute(
                        "filterError",
                        "Start date must not be after end date.");

                counts = adherenceService.getDoseCounts(userId);
                adherenceRate =
                        adherenceService.calculateAdherence(userId);
            } else {
                counts = adherenceService.getDoseCounts(
                        userId, startDate, endDate);

                adherenceRate = adherenceService.calculateAdherence(
                        userId, startDate, endDate);
            }

        } else {
            counts = adherenceService.getDoseCounts(userId);
            adherenceRate =
                    adherenceService.calculateAdherence(userId);
        }

        model.addAttribute("taken", counts.get("taken"));
        model.addAttribute("missed", counts.get("missed"));
        model.addAttribute("skipped", counts.get("skipped"));

        model.addAttribute(
                "adherenceRate",
                String.format("%.1f", adherenceRate));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "adherence";
    }
}
