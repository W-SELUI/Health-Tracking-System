package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.service.MedicationService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/medications")
public class MedicationController {

    private final MedicationService medicationService;
    private final UserRepository userRepository;

    public MedicationController(
            MedicationService medicationService,
            UserRepository userRepository) {
        this.medicationService = medicationService;
        this.userRepository = userRepository;
    }

    private User getLoggedInUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        String email = authentication.getName();

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user was not found."));
    }

    // Display medications belonging to the logged-in user
    @GetMapping
    public String viewMedications(
            Model model,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        model.addAttribute(
                "medications",
                medicationService.getMedicationsByUserId(user.getId())
        );

        return "medications";
    }

    // Display the add medication form
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("medication", new Medication());
        return "add-medication";
    }

    // Save a new medication for the logged-in user
    @PostMapping("/save")
    public String saveMedication(
            @ModelAttribute("medication") Medication medication,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        // Always assign the medication to the logged-in user
        medication.setUser(user);

        medicationService.saveMedication(medication);

        return "redirect:/medications";
    }

    // Display the edit form for a medication owned by this user
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Medication medication = medicationService
                .getMedicationByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found or access denied."));

        model.addAttribute("medication", medication);

        return "edit-medication";
    }

    // Update an existing medication owned by this user
    @PostMapping("/update")
    public String updateMedication(
            @RequestParam("id") Long medicationId,
            @RequestParam("medicationName") String medicationName,
            @RequestParam("dosage") String dosage,
            @RequestParam("instructions") String instructions,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Medication existing = medicationService
                .getMedicationByIdAndUserId(
                        medicationId, user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found or access denied."));

        existing.setMedicationName(medicationName);
        existing.setDosage(dosage);
        existing.setInstructions(instructions);

        medicationService.saveMedication(existing);

        return "redirect:/medications";
    }

    // Delete a medication only if it belongs to the logged-in user
    @PostMapping("/delete/{id}")
    public String deleteMedication(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Medication medication = medicationService
                .getMedicationByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medication not found or access denied."));

        medicationService.deleteMedication(
                medication.getId(),
                user.getId()
        );

        return "redirect:/medications";
    }

    // Redirect when a medication is missing or belongs to another user
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleMedicationAccessError(
            IllegalArgumentException exception,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Medication not found or access denied."
        );

        return "redirect:/medications";
    }
}
