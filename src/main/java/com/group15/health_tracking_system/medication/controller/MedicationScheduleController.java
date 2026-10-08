package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.service.MedicationScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/schedules")
public class MedicationScheduleController {

    private final MedicationScheduleService medicationScheduleService;

    public MedicationScheduleController(
            MedicationScheduleService medicationScheduleService) {
        this.medicationScheduleService = medicationScheduleService;
    }

    @GetMapping
    public String listSchedules(Model model) {

        model.addAttribute(
                "schedules",
                medicationScheduleService.getAll()
        );

        return "schedules";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {

        model.addAttribute(
                "schedule",
                new MedicationSchedule()
        );

        return "add-schedule";
    }

    @PostMapping("/save")
    public String saveSchedule(
            @ModelAttribute MedicationSchedule schedule) {

        medicationScheduleService.save(schedule);

        return "redirect:/schedules";
    }

    @GetMapping("/delete/{id}")
    public String deleteSchedule(
            @PathVariable Long id) {

        medicationScheduleService.delete(id);

        return "redirect:/schedules";
    }

    @GetMapping("/edit/{id}")
    public String editSchedule(
            @PathVariable Long id,
            Model model) {

        MedicationSchedule schedule =
                medicationScheduleService
                        .getById(id)
                        .orElseThrow();

        model.addAttribute(
                "schedule",
                schedule
        );

        return "edit-schedule";
    }

    @PostMapping("/update")
    public String updateSchedule(
            @ModelAttribute MedicationSchedule schedule) {

        medicationScheduleService.save(schedule);

        return "redirect:/schedules";
    }
}