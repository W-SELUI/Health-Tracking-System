package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.service.DoseRecordService;
import com.group15.health_tracking_system.medication.service.MedicationScheduleService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dose-records")
public class DoseRecordController {

    private final DoseRecordService doseRecordService;
    private final MedicationScheduleService medicationScheduleService;
    private final UserRepository userRepository;

    public DoseRecordController(
            DoseRecordService doseRecordService,
            MedicationScheduleService medicationScheduleService,
            UserRepository userRepository) {

        this.doseRecordService =
                doseRecordService;

        this.medicationScheduleService =
                medicationScheduleService;

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

    /*
     * Display all dose records belonging
     * to the logged-in patient.
     */
    @GetMapping
    public String listDoseRecords(
            Model model,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        model.addAttribute(
                "doseRecords",
                doseRecordService
                        .getByUserId(
                                user.getId()));

        return "dose-records";
    }

    /*
     * Display the add dose record page.
     */
    @GetMapping("/add")
    public String showAddDoseRecordPage(
            Model model,
            Authentication authentication,
            @RequestParam(
                    value = "scheduleId",
                    required = false)
            Long scheduleId,
            @RequestParam(
                    value = "doseDate",
                    required = false)
            LocalDate doseDate) {

        User user =
                getLoggedInUser(authentication);

        List<MedicationSchedule> schedules =
                medicationScheduleService
                        .getByUserId(
                                user.getId());

        model.addAttribute(
                "schedules",
                schedules);

        model.addAttribute(
                "statuses",
                DoseStatus.values());

        LocalDate selectedDate =
                doseDate != null
                        ? doseDate
                        : LocalDate.now();

        model.addAttribute(
                "today",
                LocalDate.now());

        model.addAttribute(
                "selectedDate",
                selectedDate);

        model.addAttribute(
                "selectedScheduleId",
                scheduleId);

        /*
         * If a schedule has been selected,
         * calculate its valid dose times
         * for the selected date.
         */
        if (scheduleId != null) {

            MedicationSchedule schedule =
                    medicationScheduleService
                            .getByIdAndUserId(
                                    scheduleId,
                                    user.getId())
                            .orElse(null);

            if (schedule != null) {

                model.addAttribute(
                        "doseTimes",
                        doseRecordService
                                .getScheduledTimesForDate(
                                        schedule,
                                        selectedDate));

                model.addAttribute(
                        "selectedSchedule",
                        schedule);
            }
        }

        return "add-dose-record";
    }

    /*
     * Save one exact scheduled dose.
     */
    @PostMapping("/save")
    public String saveDoseRecord(
            @RequestParam("scheduleId")
            Long scheduleId,

            @RequestParam("doseDate")
            LocalDate doseDate,

            @RequestParam("scheduledTime")
            LocalTime scheduledTime,

            @RequestParam("status")
            DoseStatus status,

            Authentication authentication,

            RedirectAttributes redirectAttributes) {

        try {

            User user =
                    getLoggedInUser(authentication);

            MedicationSchedule schedule =
                    medicationScheduleService
                            .getByIdAndUserId(
                                    scheduleId,
                                    user.getId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Medication schedule not found for this user."));

            doseRecordService
                    .recordScheduledDose(
                            schedule,
                            doseDate,
                            scheduledTime,
                            status);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Dose record saved successfully.");

            return "redirect:/dose-records";

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

            redirectAttributes.addAttribute(
                    "scheduleId",
                    scheduleId);

            redirectAttributes.addAttribute(
                    "doseDate",
                    doseDate);

            return "redirect:/dose-records/add";
        }
    }

    /*
     * Display the edit page for
     * one patient-owned dose record.
     */
    @GetMapping("/edit/{id}")
    public String showEditDoseRecordPage(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        DoseRecord doseRecord =
                doseRecordService
                        .getByIdAndUserId(
                                id,
                                user.getId());

        model.addAttribute(
                "doseRecord",
                doseRecord);

        model.addAttribute(
                "statuses",
                DoseStatus.values());

        return "edit-dose-record";
    }

    /*
     * Update only the status of an
     * existing dose record.
     */
    @PostMapping("/update/{id}")
    public String updateDoseRecord(
            @PathVariable Long id,

            @RequestParam("status")
            DoseStatus status,

            Authentication authentication,

            RedirectAttributes redirectAttributes) {

        try {

            User user =
                    getLoggedInUser(authentication);

            DoseRecord existing =
                    doseRecordService
                            .getByIdAndUserId(
                                    id,
                                    user.getId());

            existing.setStatus(
                    status);

            if (status == DoseStatus.TAKEN) {

                existing.setActualTakenAt(
                        LocalDateTime.now());

            } else {

                existing.setActualTakenAt(
                        null);
            }

            doseRecordService.save(
                    existing);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Dose record updated successfully.");

            return "redirect:/dose-records";

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "redirect:/dose-records";
        }
    }

    /*
     * Delete a dose record belonging
     * only to the logged-in patient.
     */
    @PostMapping("/delete/{id}")
    public String deleteDoseRecord(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {

            User user =
                    getLoggedInUser(authentication);

            doseRecordService
                    .deleteByIdAndUserId(
                            id,
                            user.getId());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Dose record deleted successfully.");

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/dose-records";
    }
}