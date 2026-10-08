package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
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

    @GetMapping
    public String listDoseRecords(Model model) {

        model.addAttribute(
                "doseRecords",
                doseRecordService.getAll()
        );

        return "dose-records";
    }

    @GetMapping("/add")
    public String showAddDoseRecordPage(Model model) {

        model.addAttribute("doseRecord", new DoseRecord());
        model.addAttribute("statuses", DoseStatus.values());
        model.addAttribute(
                "medications",
                medicationService.getAllMedications()
        );

        return "add-dose-record";
    }

    @PostMapping("/save")
    public String saveDoseRecord(
            @ModelAttribute DoseRecord doseRecord) {

        doseRecordService.save(doseRecord);

        return "redirect:/dose-records";
    }
}