
package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.service.MedicationScheduleService;
import com.group15.health_tracking_system.medication.service.MedicationService;

import java.time.LocalTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/schedules")
public class MedicationScheduleController {

    private final MedicationScheduleService medicationScheduleService;
    private final MedicationService medicationService;

    public MedicationScheduleController(
            MedicationScheduleService medicationScheduleService,
            MedicationService medicationService) {
        this.medicationScheduleService = medicationScheduleService;
        this.medicationService = medicationService;
    }

    // Display all schedules
    @GetMapping
    public String listSchedules(Model model) {
        model.addAttribute(
                "schedules",
                medicationScheduleService.getAll()
        );

        return "schedules";
    }

    // Show the form to add a schedule
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("schedule", new MedicationSchedule());
        model.addAttribute(
                "medications",
                medicationService.getAllMedications()
        );

        return "add-schedule";
    }

    // Save a new schedule
    @PostMapping("/save")
    public String saveSchedule(
            @RequestParam("medicationId") Long medicationId,
            @RequestParam("scheduledTime") LocalTime scheduledTime) {

        Medication medication = medicationService
                .getMedicationById(medicationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found: " + medicationId));

        MedicationSchedule schedule = new MedicationSchedule();
        schedule.setScheduledTime(scheduledTime);
        schedule.setMedication(medication);

        medicationScheduleService.save(schedule);

        return "redirect:/schedules";
    }

    // Show the form to edit a schedule
    @GetMapping("/edit/{id}")
    public String editSchedule(
            @PathVariable Long id,
            Model model) {

        MedicationSchedule schedule = medicationScheduleService
                .getById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Schedule not found: " + id));

        model.addAttribute("schedule", schedule);
        model.addAttribute(
                "medications",
                medicationService.getAllMedications()
        );

        return "edit-schedule";
    }

    // Update an existing schedule
    @PostMapping("/update")
    public String updateSchedule(
            @RequestParam("id") Long scheduleId,
            @RequestParam("medicationId") Long medicationId,
            @RequestParam("scheduledTime") LocalTime scheduledTime) {

        MedicationSchedule existing = medicationScheduleService
                .getById(scheduleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Schedule not found: " + scheduleId));

        Medication medication = medicationService
                .getMedicationById(medicationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found: " + medicationId));

        existing.setScheduledTime(scheduledTime);
        existing.setMedication(medication);

        medicationScheduleService.save(existing);

        return "redirect:/schedules";
    }

    // Delete a schedule
    @GetMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        medicationScheduleService.delete(id);
        return "redirect:/schedules";
    }
}
