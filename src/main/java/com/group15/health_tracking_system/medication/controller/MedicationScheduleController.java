package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.service.MedicationScheduleService;
import com.group15.health_tracking_system.medication.service.MedicationService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/schedules")
public class MedicationScheduleController {

    private final MedicationScheduleService medicationScheduleService;
    private final MedicationService medicationService;
    private final UserRepository userRepository;

    public MedicationScheduleController(
            MedicationScheduleService medicationScheduleService,
            MedicationService medicationService,
            UserRepository userRepository) {

        this.medicationScheduleService =
                medicationScheduleService;

        this.medicationService =
                medicationService;

        this.userRepository =
                userRepository;
    }

    private User getLoggedInUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        return userRepository
                .findByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user was not found."));
    }

    private Medication getOwnedMedication(
            Long medicationId,
            Long userId) {

        return medicationService
                .getMedicationByIdAndUserId(
                        medicationId,
                        userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found for this user."));
    }

    private void validateSchedule(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime scheduledTime,
            Integer intervalHours) {

        if (startDate == null) {
            throw new IllegalArgumentException(
                    "Start date is required.");
        }

        if (scheduledTime == null) {
            throw new IllegalArgumentException(
                    "Start time is required.");
        }

        if (intervalHours == null
                || intervalHours <= 0) {

            throw new IllegalArgumentException(
                    "Interval hours must be greater than zero.");
        }

        if (intervalHours > 24) {
            throw new IllegalArgumentException(
                    "Interval hours cannot be greater than 24.");
        }

        if (endDate != null
                && endDate.isBefore(startDate)) {

            throw new IllegalArgumentException(
                    "End date cannot be before start date.");
        }
    }

    @GetMapping
    public String listSchedules(
            Model model,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        model.addAttribute(
                "schedules",
                medicationScheduleService
                        .getByUserId(
                                user.getId()));

        return "schedules";
    }

    @GetMapping("/add")
    public String showAddForm(
            Model model,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        model.addAttribute(
                "schedule",
                new MedicationSchedule());

        model.addAttribute(
                "medications",
                medicationService
                        .getMedicationsByUserId(
                                user.getId()));

        return "add-schedule";
    }

    @PostMapping("/save")
    public String saveSchedule(
            @RequestParam("medicationId")
            Long medicationId,

            @RequestParam("startDate")
            LocalDate startDate,

            @RequestParam(
                    value = "endDate",
                    required = false)
            LocalDate endDate,

            @RequestParam("scheduledTime")
            LocalTime scheduledTime,

            @RequestParam("intervalHours")
            Integer intervalHours,

            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        Medication medication =
                getOwnedMedication(
                        medicationId,
                        user.getId());

        validateSchedule(
                startDate,
                endDate,
                scheduledTime,
                intervalHours);

        MedicationSchedule schedule =
                new MedicationSchedule();

        schedule.setMedication(
                medication);

        schedule.setStartDate(
                startDate);

        schedule.setEndDate(
                endDate);

        schedule.setScheduledTime(
                scheduledTime);

        schedule.setIntervalHours(
                intervalHours);

        medicationScheduleService.save(
                schedule);

        return "redirect:/schedules";
    }

    @GetMapping("/edit/{id}")
    public String editSchedule(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        MedicationSchedule schedule =
                medicationScheduleService
                        .getByIdAndUserId(
                                id,
                                user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Schedule not found for this user."));

        model.addAttribute(
                "schedule",
                schedule);

        model.addAttribute(
                "medications",
                medicationService
                        .getMedicationsByUserId(
                                user.getId()));

        return "edit-schedule";
    }

    @PostMapping("/update")
    public String updateSchedule(
            @RequestParam("id")
            Long scheduleId,

            @RequestParam("medicationId")
            Long medicationId,

            @RequestParam("startDate")
            LocalDate startDate,

            @RequestParam(
                    value = "endDate",
                    required = false)
            LocalDate endDate,

            @RequestParam("scheduledTime")
            LocalTime scheduledTime,

            @RequestParam("intervalHours")
            Integer intervalHours,

            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        MedicationSchedule existing =
                medicationScheduleService
                        .getByIdAndUserId(
                                scheduleId,
                                user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Schedule not found for this user."));

        Medication medication =
                getOwnedMedication(
                        medicationId,
                        user.getId());

        validateSchedule(
                startDate,
                endDate,
                scheduledTime,
                intervalHours);

        existing.setMedication(
                medication);

        existing.setStartDate(
                startDate);

        existing.setEndDate(
                endDate);

        existing.setScheduledTime(
                scheduledTime);

        existing.setIntervalHours(
                intervalHours);

        medicationScheduleService.save(
                existing);

        return "redirect:/schedules";
    }

    @PostMapping("/delete/{id}")
    public String deleteSchedule(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        medicationScheduleService.delete(
                id,
                user.getId());

        return "redirect:/schedules";
    }

    /**
     * Validation problems (for example an end date before the start date)
     * are thrown as IllegalArgumentException. Show the message on the page
     * the user came from instead of an error page.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleValidationError(
            IllegalArgumentException exception,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                exception.getMessage());

        return "redirect:" + safeReturnPath(
                request.getHeader("Referer"),
                "/schedules");
    }

    private String safeReturnPath(String referer, String fallback) {

        if (referer == null) {
            return fallback;
        }

        try {
            java.net.URI uri = java.net.URI.create(referer);
            String path = uri.getRawPath();

            if (path == null || !path.startsWith("/schedules")) {
                return fallback;
            }

            return uri.getRawQuery() == null
                    ? path
                    : path + "?" + uri.getRawQuery();

        } catch (IllegalArgumentException invalidReferer) {
            return fallback;
        }
    }
}
