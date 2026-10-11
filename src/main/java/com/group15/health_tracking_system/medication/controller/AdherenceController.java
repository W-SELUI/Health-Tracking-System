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

        this.adherenceService =
                adherenceService;

        this.userRepository =
                userRepository;
    }

    /*
     * Show adherence information for
     * the logged-in patient.
     */
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

        /*
         * Confirm that a user is logged in.
         */
        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                authentication.getName())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Logged-in user was not found."));

        Long userId =
                user.getId();

        Map<String, Long> counts;
        double adherenceRate;

        boolean hasStartDate =
                startDate != null;

        boolean hasEndDate =
                endDate != null;

        /*
         * If only one date was selected,
         * show an error and fall back to
         * the patient's overall adherence.
         */
        if (hasStartDate != hasEndDate) {

            model.addAttribute(
                    "filterError",
                    "Please select both a start date and an end date.");

            counts =
                    adherenceService
                            .getDoseCounts(
                                    userId);

            adherenceRate =
                    adherenceService
                            .calculateAdherence(
                                    userId);
        }

        /*
         * If both dates were selected,
         * validate and use the chosen period.
         */
        else if (hasStartDate) {

            if (startDate.isAfter(endDate)) {

                model.addAttribute(
                        "filterError",
                        "Start date must not be after end date.");

                counts =
                        adherenceService
                                .getDoseCounts(
                                        userId);

                adherenceRate =
                        adherenceService
                                .calculateAdherence(
                                        userId);

            } else {

                counts =
                        adherenceService
                                .getDoseCounts(
                                        userId,
                                        startDate,
                                        endDate);

                adherenceRate =
                        adherenceService
                                .calculateAdherence(
                                        userId,
                                        startDate,
                                        endDate);
            }
        }

        /*
         * No date filter selected.
         */
        else {

            counts =
                    adherenceService
                            .getDoseCounts(
                                    userId);

            adherenceRate =
                    adherenceService
                            .calculateAdherence(
                                    userId);
        }

        /*
         * Send status counts to the page.
         */
        model.addAttribute(
                "taken",
                counts.getOrDefault(
                        "taken",
                        0L));

        model.addAttribute(
                "missed",
                counts.getOrDefault(
                        "missed",
                        0L));

        model.addAttribute(
                "skipped",
                counts.getOrDefault(
                        "skipped",
                        0L));

        /*
         * New schedule-based values.
         */
        model.addAttribute(
                "expected",
                counts.getOrDefault(
                        "expected",
                        0L));

        model.addAttribute(
                "unrecorded",
                counts.getOrDefault(
                        "unrecorded",
                        0L));

        /*
         * Format adherence to one
         * decimal place.
         */
        model.addAttribute(
                "adherenceRate",
                String.format(
                        "%.1f",
                        adherenceRate));

        /*
         * Keep selected dates visible
         * in the filter form.
         */
        model.addAttribute(
                "startDate",
                startDate);

        model.addAttribute(
                "endDate",
                endDate);

        model.addAttribute(
                "today",
                LocalDate.now());

        return "adherence";
    }
}