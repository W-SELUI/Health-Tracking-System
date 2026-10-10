package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.service.DoseRecordService;
import com.group15.health_tracking_system.medication.service.MedicationService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dose-records")
public class DoseRecordController {

    private final DoseRecordService doseRecordService;
    private final MedicationService medicationService;
    private final UserRepository userRepository;

    public DoseRecordController(
            DoseRecordService doseRecordService,
            MedicationService medicationService,
            UserRepository userRepository) {
        this.doseRecordService = doseRecordService;
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

    // Display this user's dose records
    @GetMapping
    public String listDoseRecords(
            Model model, Authentication authentication) {

        User user = getLoggedInUser(authentication);

        model.addAttribute(
                "doseRecords",
                doseRecordService.getByUserId(user.getId()));

        return "dose-records";
    }

    // Display the add form
    @GetMapping("/add")
    public String showAddDoseRecordPage(
            Model model, Authentication authentication) {

        User user = getLoggedInUser(authentication);

        model.addAttribute("doseRecord", new DoseRecord());
        model.addAttribute("statuses", DoseStatus.values());
        model.addAttribute(
                "medications",
                medicationService.getMedicationsByUserId(user.getId()));

        return "add-dose-record";
    }

    // Save a dose record for this user's medication
    @PostMapping("/save")
    public String saveDoseRecord(
            @ModelAttribute("doseRecord") DoseRecord doseRecord,
            @RequestParam("medicationId") Long medicationId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Medication medication =
                getOwnedMedication(medicationId, user.getId());

        doseRecord.setMedication(medication);
        doseRecordService.save(doseRecord);

        return "redirect:/dose-records";
    }

    // Display the edit form
    @GetMapping("/edit/{id}")
    public String showEditDoseRecordPage(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        DoseRecord doseRecord =
                doseRecordService.getByIdAndUserId(id, user.getId());

        model.addAttribute("doseRecord", doseRecord);
        model.addAttribute("statuses", DoseStatus.values());
        model.addAttribute(
                "medications",
                medicationService.getMedicationsByUserId(user.getId()));

        return "edit-dose-record";
    }

    // Update a dose record belonging to this user
    @PostMapping("/update/{id}")
    public String updateDoseRecord(
            @PathVariable Long id,
            @ModelAttribute("doseRecord") DoseRecord formRecord,
            @RequestParam("medicationId") Long medicationId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        DoseRecord existing =
                doseRecordService.getByIdAndUserId(id, user.getId());

        Medication medication =
                getOwnedMedication(medicationId, user.getId());

        existing.setDoseDate(formRecord.getDoseDate());
        existing.setStatus(formRecord.getStatus());
        existing.setMedication(medication);

        doseRecordService.save(existing);

        return "redirect:/dose-records";
    }

    // Delete a dose record belonging to this user
    @PostMapping("/delete/{id}")
    public String deleteDoseRecord(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        doseRecordService.deleteByIdAndUserId(id, user.getId());

        return "redirect:/dose-records";
    }
}
