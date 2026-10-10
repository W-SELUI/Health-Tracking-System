package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Appointment;
import com.group15.health_tracking_system.medication.service.AppointmentService;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    private final List<String> statuses =
            List.of("SCHEDULED", "COMPLETED", "CANCELLED", "MISSED");

    public AppointmentController(
            AppointmentService appointmentService,
            UserRepository userRepository) {
        this.appointmentService = appointmentService;
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

    @GetMapping
    public String listAppointments(
            Model model,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        LocalDate today = LocalDate.now();

        List<Appointment> allAppointments =
                appointmentService.getByPatientId(user.getId());

        for (Appointment appointment : allAppointments) {
            if (appointment.getAppointmentDate().isBefore(today)
                    && "SCHEDULED".equals(appointment.getStatus())) {

                appointment.setStatus("MISSED");
                appointmentService.save(appointment);
            }
        }

        List<Appointment> upcomingAppointments =
                allAppointments.stream()
                        .filter(a ->
                                !a.getAppointmentDate().isBefore(today))
                        .filter(a ->
                                "SCHEDULED".equals(a.getStatus()))
                        .toList();

        List<Appointment> previousAppointments =
                allAppointments.stream()
                        .filter(a ->
                                a.getAppointmentDate().isBefore(today)
                                || "COMPLETED".equals(a.getStatus())
                                || "MISSED".equals(a.getStatus())
                                || "CANCELLED".equals(a.getStatus()))
                        .toList();

        model.addAttribute(
                "appointments",
                allAppointments);

        model.addAttribute(
                "upcomingAppointments",
                upcomingAppointments);

        model.addAttribute(
                "previousAppointments",
                previousAppointments);

        model.addAttribute(
                "statuses",
                statuses);

        return "appointments";
    }

    @GetMapping("/add")
    public String showAddForm(
            Model model,
            Authentication authentication) {

        getLoggedInUser(authentication);

        model.addAttribute(
                "appointment",
                new Appointment());

        model.addAttribute(
                "doctors",
                getRegisteredProviders());

        model.addAttribute(
                "statuses",
                statuses);

        model.addAttribute(
                "appointmentDateTime",
                "");

        return "add-appointment";
    }

    @PostMapping("/save")
    public String saveAppointment(
            @RequestParam Long providerId,
            @RequestParam String location,
            @RequestParam String appointmentDateTime,
            @RequestParam String status,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        User provider =
                getRegisteredProvider(providerId);

        LocalDateTime dateTime =
                LocalDateTime.parse(
                        appointmentDateTime);

        Appointment appointment =
                new Appointment();

        appointment.setPatient(user);

        appointment.setDoctorName(
                provider.getFullName());

        appointment.setLocation(
                location.trim());

        appointment.setAppointmentDate(
                dateTime.toLocalDate());

        appointment.setAppointmentTime(
                dateTime.toLocalTime());

        appointment.setStatus(
                validateStatus(status));

        appointmentService.save(
                appointment);

        return "redirect:/appointments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        Appointment appointment =
                appointmentService
                        .getByIdAndPatientId(
                                id,
                                user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found for this patient."));

        model.addAttribute(
                "appointment",
                appointment);

        model.addAttribute(
                "doctors",
                getRegisteredProviders());

        model.addAttribute(
                "statuses",
                statuses);

        model.addAttribute(
                "appointmentDateTime",
                LocalDateTime.of(
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime())
                        .toString());

        return "edit-appointment";
    }

    @PostMapping("/update/{id}")
    public String updateAppointment(
            @PathVariable Long id,
            @RequestParam Long providerId,
            @RequestParam String location,
            @RequestParam String appointmentDateTime,
            @RequestParam String status,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        User provider =
                getRegisteredProvider(providerId);

        Appointment appointment =
                appointmentService
                        .getByIdAndPatientId(
                                id,
                                user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found for this patient."));

        LocalDateTime dateTime =
                LocalDateTime.parse(
                        appointmentDateTime);

        appointment.setDoctorName(
                provider.getFullName());

        appointment.setLocation(
                location.trim());

        appointment.setAppointmentDate(
                dateTime.toLocalDate());

        appointment.setAppointmentTime(
                dateTime.toLocalTime());

        appointment.setStatus(
                validateStatus(status));

        appointmentService.save(
                appointment);

        return "redirect:/appointments";
    }

    @PostMapping("/cancel/{id}")
    public String cancelAppointment(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        Appointment appointment =
                appointmentService
                        .getByIdAndPatientId(
                                id,
                                user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found for this patient."));

        if (!"SCHEDULED".equals(
                appointment.getStatus())) {

            throw new IllegalArgumentException(
                    "Only scheduled appointments can be cancelled.");
        }

        appointment.setStatus(
                "CANCELLED");

        appointmentService.save(
                appointment);

        return "redirect:/appointments";
    }

    @PostMapping("/delete/{id}")
    public String deleteAppointment(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                getLoggedInUser(authentication);

        appointmentService.delete(
                id,
                user.getId());

        return "redirect:/appointments";
    }

    private List<User> getRegisteredProviders() {

        return userRepository
                .findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(
                        Role.PROVIDER);
    }

    private User getRegisteredProvider(
            Long providerId) {

        User provider =
                userRepository
                        .findById(providerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Selected doctor was not found."));

        if (provider.getRole() != Role.PROVIDER
                || !provider.isEnabled()) {

            throw new IllegalArgumentException(
                    "Selected user is not an available doctor.");
        }

        return provider;
    }

    private String validateStatus(
            String status) {

        if (status == null
                || !statuses.contains(status)) {

            throw new IllegalArgumentException(
                    "Invalid appointment status");
        }

        return status;
    }
}