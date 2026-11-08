package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Appointment;
import com.group15.health_tracking_system.medication.entity.AppointmentStatus;
import com.group15.health_tracking_system.medication.service.AppointmentService;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

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

        User user =
                getLoggedInUser(authentication);

        LocalDateTime now =
                LocalDateTime.now();

        List<Appointment> allAppointments =
                appointmentService
                        .getByPatientId(user.getId());

        List<Appointment> upcomingAppointments =
                allAppointments.stream()
                        .filter(appointment -> {

                            LocalDateTime appointmentDateTime =
                                    LocalDateTime.of(
                                            appointment.getAppointmentDate(),
                                            appointment.getAppointmentTime());

                            return !appointmentDateTime.isBefore(now)
                                    && (appointment.getStatus()
                                            == AppointmentStatus.REQUESTED
                                    || appointment.getStatus()
                                            == AppointmentStatus.CONFIRMED);
                        })
                        .toList();

        List<Appointment> previousAppointments =
                allAppointments.stream()
                        .filter(appointment -> {

                            LocalDateTime appointmentDateTime =
                                    LocalDateTime.of(
                                            appointment.getAppointmentDate(),
                                            appointment.getAppointmentTime());

                            return appointmentDateTime.isBefore(now)
                                    || appointment.getStatus()
                                            == AppointmentStatus.COMPLETED
                                    || appointment.getStatus()
                                            == AppointmentStatus.MISSED
                                    || appointment.getStatus()
                                            == AppointmentStatus.CANCELLED
                                    || appointment.getStatus()
                                            == AppointmentStatus.REJECTED;
                        })
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
                "appointmentDateTime",
                "");

        return "add-appointment";
    }

    @PostMapping("/save")
    public String saveAppointment(
            @RequestParam Long providerId,
            @RequestParam String location,
            @RequestParam String purpose,
            @RequestParam String appointmentDateTime,
            Authentication authentication) {

        User patient =
                getLoggedInUser(authentication);

        User provider =
                getRegisteredProvider(providerId);

        LocalDateTime requestedDateTime =
                parseAndValidateFutureDateTime(
                        appointmentDateTime);

        validateTextField(
                location,
                "Location");

        validateTextField(
                purpose,
                "Purpose");

        Appointment appointment =
                new Appointment();

        appointment.setPatient(
                patient);

        appointment.setProvider(
                provider);

        appointment.setDoctorName(
                provider.getFullName());

        appointment.setLocation(
                location.trim());

        appointment.setPurpose(
                purpose.trim());

        appointment.setAppointmentDate(
                requestedDateTime.toLocalDate());

        appointment.setAppointmentTime(
                requestedDateTime.toLocalTime());

        appointment.setStatus(
                AppointmentStatus.REQUESTED);

        appointmentService.save(
                appointment);

        return "redirect:/appointments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User patient =
                getLoggedInUser(authentication);

        Appointment appointment =
                appointmentService
                        .getByIdAndPatientId(
                                id,
                                patient.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found for this patient."));

        if (appointment.getStatus()
                != AppointmentStatus.REQUESTED) {

            throw new IllegalStateException(
                    "Only requested appointments can be edited.");
        }

        model.addAttribute(
                "appointment",
                appointment);

        model.addAttribute(
                "doctors",
                getRegisteredProviders());

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
            @RequestParam String purpose,
            @RequestParam String appointmentDateTime,
            Authentication authentication) {

        User patient =
                getLoggedInUser(authentication);

        Appointment appointment =
                appointmentService
                        .getByIdAndPatientId(
                                id,
                                patient.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found for this patient."));

        if (appointment.getStatus()
                != AppointmentStatus.REQUESTED) {

            throw new IllegalStateException(
                    "Only requested appointments can be edited.");
        }

        User provider =
                getRegisteredProvider(providerId);

        LocalDateTime requestedDateTime =
                parseAndValidateFutureDateTime(
                        appointmentDateTime);

        validateTextField(
                location,
                "Location");

        validateTextField(
                purpose,
                "Purpose");

        appointment.setProvider(
                provider);

        appointment.setDoctorName(
                provider.getFullName());

        appointment.setLocation(
                location.trim());

        appointment.setPurpose(
                purpose.trim());

        appointment.setAppointmentDate(
                requestedDateTime.toLocalDate());

        appointment.setAppointmentTime(
                requestedDateTime.toLocalTime());

        appointment.setStatus(
                AppointmentStatus.REQUESTED);

        appointmentService.save(
                appointment);

        return "redirect:/appointments";
    }

    @PostMapping("/cancel/{id}")
    public String cancelAppointment(
            @PathVariable Long id,
            Authentication authentication) {

        User patient =
                getLoggedInUser(authentication);

        Appointment appointment =
                appointmentService
                        .getByIdAndPatientId(
                                id,
                                patient.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found for this patient."));

        if (appointment.getStatus()
                != AppointmentStatus.REQUESTED
                && appointment.getStatus()
                != AppointmentStatus.CONFIRMED) {

            throw new IllegalArgumentException(
                    "Only requested or confirmed appointments can be cancelled.");
        }

        appointment.setStatus(
                AppointmentStatus.CANCELLED);

        appointmentService.save(
                appointment);

        return "redirect:/appointments";
    }

    @PostMapping("/delete/{id}")
    public String deleteAppointment(
            @PathVariable Long id,
            Authentication authentication) {

        User patient =
                getLoggedInUser(authentication);

        Appointment appointment =
                appointmentService
                        .getByIdAndPatientId(
                                id,
                                patient.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found for this patient."));

        if (appointment.getStatus()
                == AppointmentStatus.REQUESTED
                || appointment.getStatus()
                == AppointmentStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Active appointments must be cancelled before deletion.");
        }

        appointmentService.delete(
                id,
                patient.getId());

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

        if (provider.getRole()
                != Role.PROVIDER
                || !provider.isEnabled()) {

            throw new IllegalArgumentException(
                    "Selected user is not an available doctor.");
        }

        return provider;
    }

    private LocalDateTime parseAndValidateFutureDateTime(
            String appointmentDateTime) {

        if (appointmentDateTime == null
                || appointmentDateTime.isBlank()) {

            throw new IllegalArgumentException(
                    "Appointment date and time are required.");
        }

        LocalDateTime requestedDateTime;

        try {

            requestedDateTime =
                    LocalDateTime.parse(
                            appointmentDateTime);

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Invalid appointment date or time.");
        }

        if (!requestedDateTime.isAfter(
                LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Appointment date and time must be in the future.");
        }

        return requestedDateTime;
    }

    private void validateTextField(
            String value,
            String fieldName) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName + " is required.");
        }
    }

    /**
     * Validation problems (for example an end date before the start date)
     * are thrown as IllegalArgumentException. Show the message on the page
     * the user came from instead of an error page.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleValidationError(
            IllegalArgumentException exception,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                exception.getMessage());

        return "redirect:" + safeReturnPath(
                request.getHeader("Referer"),
                "/appointments");
    }

    private String safeReturnPath(String referer, String fallback) {

        if (referer == null) {
            return fallback;
        }

        try {
            java.net.URI uri = java.net.URI.create(referer);
            String path = uri.getRawPath();

            if (path == null || !path.startsWith("/appointments")) {
                return fallback;
            }

            return uri.getRawQuery() == null
                    ? path
                    : path + "?" + uri.getRawQuery();

        } catch (IllegalArgumentException invalidReferer) {
            return fallback;
        }
    }
}
