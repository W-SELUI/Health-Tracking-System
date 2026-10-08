package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.service.MedicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public String viewMedications(Model model) {
        model.addAttribute("medications",
                medicationService.getAllMedications());
        return "medications";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("medication", new Medication());
        return "add-medication";
    }

    @PostMapping("/save")
    public String saveMedication(@ModelAttribute Medication medication) {
        medicationService.saveMedication(medication);
        return "redirect:/medications";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {

        Medication medication = medicationService
                .getMedicationById(id)
                .orElseThrow(() -> new RuntimeException("Medication not found"));

        model.addAttribute("medication", medication);

        return "edit-medication";
    }

    @PostMapping("/update")
    public String updateMedication(@ModelAttribute Medication medication) {
        medicationService.saveMedication(medication);
        return "redirect:/medications";
    }

    @GetMapping("/delete/{id}")
    public String deleteMedication(@PathVariable Long id) {
        medicationService.deleteMedication(id);
        return "redirect:/medications";
    }
}