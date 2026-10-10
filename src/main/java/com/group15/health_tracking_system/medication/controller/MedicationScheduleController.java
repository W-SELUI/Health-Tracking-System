package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.service.MedicationScheduleService;
import com.group15.health_tracking_system.medication.service.MedicationService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import java.time.LocalTime;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        this.medicationScheduleService = medicationScheduleService;
        this.medicationService = medicationService;
        this.userRepository = userRepository;
    }

    private User getLoggedInUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user was not found."));
    }

    private Medication getOwnedMedication(
            Long medicationId, Long userId) {
        return medicationService
                .getMedicationByIdAndUserId(medicationId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found for this user."));
    }

    // Display schedules belonging to the logged-in user
    @GetMapping
    public String listSchedules(
            Model model, Authentication authentication) {

        User user = getLoggedInUser(authentication);

        model.addAttribute(
                "schedules",
                medicationScheduleService.getByUserId(user.getId()));

        return "schedules";
    }

    // Show the add form
    @GetMapping("/add")
    public String showAddForm(
            Model model, Authentication authentication) {

        User user = getLoggedInUser(authentication);

        model.addAttribute("schedule", new MedicationSchedule());
        model.addAttribute(
                "medications",
                medicationService.getMedicationsByUserId(user.getId()));

        return "add-schedule";
    }

    // Save a schedule for the user's own medication
    @PostMapping("/save")
    public String saveSchedule(
            @RequestParam("medicationId") Long medicationId,
            @RequestParam("scheduledTime") LocalTime scheduledTime,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        Medication medication =
                getOwnedMedication(medicationId, user.getId());

        MedicationSchedule schedule = new MedicationSchedule();
        schedule.setScheduledTime(scheduledTime);
        schedule.setMedication(medication);

        medicationScheduleService.save(schedule);

        return "redirect:/schedules";
    }

    // Show the edit form for an owned schedule
    @GetMapping("/edit/{id}")
    public String editSchedule(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        MedicationSchedule schedule =
                medicationScheduleService.getById(id)
                        .filter(s -> s.getMedication().getUser().getId()
                                .equals(user.getId()))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Schedule not found for this user."));

        model.addAttribute("schedule", schedule);
        model.addAttribute(
                "medications",
                medicationService.getMedicationsByUserId(user.getId()));

        return "edit-schedule";
    }

    // Update an owned schedule and use an owned medication
    @PostMapping("/update")
    public String updateSchedule(
            @RequestParam("id") Long scheduleId,
            @RequestParam("medicationId") Long medicationId,
            @RequestParam("scheduledTime") LocalTime scheduledTime,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        MedicationSchedule existing =
                medicationScheduleService.getById(scheduleId)
                        .filter(s -> s.getMedication().getUser().getId()
                                .equals(user.getId()))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Schedule not found for this user."));

        Medication medication =
                getOwnedMedication(medicationId, user.getId());

        existing.setScheduledTime(scheduledTime);
        existing.setMedication(medication);

        medicationScheduleService.save(existing);

        return "redirect:/schedules";
    }

    // Delete an owned schedule
    @PostMapping("/delete/{id}")
    public String deleteSchedule(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        MedicationSchedule schedule =
                medicationScheduleService.getById(id)
                        .filter(s -> s.getMedication().getUser().getId()
                                .equals(user.getId()))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Schedule not found for this user."));

        medicationScheduleService.delete(schedule.getId());

        return "redirect:/schedules";
    }
}
