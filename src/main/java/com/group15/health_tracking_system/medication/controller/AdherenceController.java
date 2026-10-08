package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdherenceController {

    private final DoseRecordRepository doseRecordRepository;

    public AdherenceController(DoseRecordRepository doseRecordRepository) {
        this.doseRecordRepository = doseRecordRepository;
    }

    @GetMapping("/adherence")
    public String adherence(Model model) {

        long taken =
                doseRecordRepository.countByStatus(DoseStatus.TAKEN);

        long missed =
                doseRecordRepository.countByStatus(DoseStatus.MISSED);

        long skipped =
                doseRecordRepository.countByStatus(DoseStatus.SKIPPED);

        long total = taken + missed + skipped;

        double adherenceRate = 0;

        if (total > 0) {
            adherenceRate =
                    ((double) taken / total) * 100;
        }

        model.addAttribute("taken", taken);
        model.addAttribute("missed", missed);
        model.addAttribute("skipped", skipped);
        model.addAttribute("adherenceRate",
                String.format("%.1f", adherenceRate));

        return "adherence";
    }
}