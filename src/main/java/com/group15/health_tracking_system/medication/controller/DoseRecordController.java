
package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.service.DoseRecordService;
import com.group15.health_tracking_system.medication.service.MedicationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dose-records")
public class DoseRecordController {

    private final DoseRecordService doseRecordService;
    private final MedicationService medicationService;

    public DoseRecordController(
            DoseRecordService doseRecordService,
            MedicationService medicationService) {
        this.doseRecordService = doseRecordService;
        this.medicationService = medicationService;
    }

    // Display all dose records
    @GetMapping
    public String listDoseRecords(Model model) {
        model.addAttribute("doseRecords", doseRecordService.getAll());
        return "dose-records";
    }

    // Display the add form
    @GetMapping("/add")
    public String showAddDoseRecordPage(Model model) {
        model.addAttribute("doseRecord", new DoseRecord());
        model.addAttribute("statuses", DoseStatus.values());
        model.addAttribute("medications", medicationService.getAllMedications());

        return "add-dose-record";
    }

    // Save a new dose record
    @PostMapping("/save")
    public String saveDoseRecord(
            @ModelAttribute("doseRecord") DoseRecord doseRecord,
            @RequestParam("medicationId") Long medicationId) {

        Medication medication = medicationService.getMedicationById(medicationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found: " + medicationId));

        doseRecord.setMedication(medication);
        doseRecordService.save(doseRecord);

        return "redirect:/dose-records";
    }

    // Display the edit form
    @GetMapping("/edit/{id}")
    public String showEditDoseRecordPage(
            @PathVariable Long id,
            Model model) {

        DoseRecord doseRecord = doseRecordService.getById(id);

        model.addAttribute("doseRecord", doseRecord);
        model.addAttribute("statuses", DoseStatus.values());
        model.addAttribute("medications", medicationService.getAllMedications());

        return "edit-dose-record";
    }

    // Update an existing dose record
    @PostMapping("/update/{id}")
    public String updateDoseRecord(
            @PathVariable Long id,
            @ModelAttribute("doseRecord") DoseRecord formRecord,
            @RequestParam("medicationId") Long medicationId) {

        DoseRecord existing = doseRecordService.getById(id);

        Medication medication = medicationService.getMedicationById(medicationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found: " + medicationId));

        existing.setDoseDate(formRecord.getDoseDate());
        existing.setStatus(formRecord.getStatus());
        existing.setMedication(medication);

        doseRecordService.save(existing);

        return "redirect:/dose-records";
    }

    // Delete a dose record
    @PostMapping("/delete/{id}")
    public String deleteDoseRecord(@PathVariable Long id) {
        doseRecordService.deleteById(id);

        return "redirect:/dose-records";
    }
}
