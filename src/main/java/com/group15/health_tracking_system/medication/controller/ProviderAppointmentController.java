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

@Controller
@RequestMapping("/provider/appointments")
public class ProviderAppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    public ProviderAppointmentController(
            AppointmentService appointmentService,
            UserRepository userRepository) {

        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
    }

    private User getLoggedInProvider(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user was not found."));

        if (user.getRole() != Role.PROVIDER) {

            throw new IllegalStateException(
                    "Only healthcare providers can access this page.");
        }

        return user;
    }

    @GetMapping
    public String viewAppointments(
            Model model,
            Authentication authentication) {

        User provider =
                getLoggedInProvider(authentication);

        List<Appointment> requestedAppointments =
                appointmentService
                        .getRequestedByProviderId(
                                provider.getId());

        List<Appointment> allAppointments =
                appointmentService
                        .getByProviderId(
                                provider.getId());

        model.addAttribute(
                "requestedAppointments",
                requestedAppointments);

        model.addAttribute(
                "allAppointments",
                allAppointments);

        return "provider-appointments";
    }

    @PostMapping("/accept/{id}")
    public String acceptAppointment(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {

            User provider =
                    getLoggedInProvider(authentication);

            Appointment appointment =
                    appointmentService
                            .getById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Appointment not found."));

            validateProviderOwnsAppointment(
                    appointment,
                    provider);

            if (appointment.getStatus()
                    != AppointmentStatus.REQUESTED) {

                throw new IllegalStateException(
                        "Only requested appointments can be accepted.");
            }

            appointment.setStatus(
                    AppointmentStatus.CONFIRMED);

            appointmentService.save(
                    appointment);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Appointment confirmed successfully.");

        } catch (IllegalArgumentException | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/provider/appointments";
    }

    @PostMapping("/reject/{id}")
    public String rejectAppointment(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {

            User provider =
                    getLoggedInProvider(authentication);

            Appointment appointment =
                    appointmentService
                            .getById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Appointment not found."));

            validateProviderOwnsAppointment(
                    appointment,
                    provider);

            if (appointment.getStatus()
                    != AppointmentStatus.REQUESTED) {

                throw new IllegalStateException(
                        "Only requested appointments can be rejected.");
            }

            appointment.setStatus(
                    AppointmentStatus.REJECTED);

            appointmentService.save(
                    appointment);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Appointment rejected.");

        } catch (IllegalArgumentException | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/provider/appointments";
    }

    @PostMapping("/complete/{id}")
    public String completeAppointment(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {

            User provider =
                    getLoggedInProvider(authentication);

            Appointment appointment =
                    appointmentService
                            .getById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Appointment not found."));

            validateProviderOwnsAppointment(
                    appointment,
                    provider);

            if (appointment.getStatus()
                    != AppointmentStatus.CONFIRMED) {

                throw new IllegalStateException(
                        "Only confirmed appointments can be completed.");
            }

            validateAppointmentTimeReached(
                    appointment);

            appointment.setStatus(
                    AppointmentStatus.COMPLETED);

            appointmentService.save(
                    appointment);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Appointment marked as completed.");

        } catch (IllegalArgumentException | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/provider/appointments";
    }

    @PostMapping("/missed/{id}")
    public String markAppointmentMissed(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {

            User provider =
                    getLoggedInProvider(authentication);

            Appointment appointment =
                    appointmentService
                            .getById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Appointment not found."));

            validateProviderOwnsAppointment(
                    appointment,
                    provider);

            if (appointment.getStatus()
                    != AppointmentStatus.CONFIRMED) {

                throw new IllegalStateException(
                        "Only confirmed appointments can be marked as missed.");
            }

            validateAppointmentTimeReached(
                    appointment);

            appointment.setStatus(
                    AppointmentStatus.MISSED);

            appointmentService.save(
                    appointment);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Appointment marked as missed.");

        } catch (IllegalArgumentException | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/provider/appointments";
    }

    private void validateProviderOwnsAppointment(
            Appointment appointment,
            User provider) {

        if (appointment.getProvider() == null
                || !appointment.getProvider()
                        .getId()
                        .equals(provider.getId())) {

            throw new IllegalStateException(
                    "This appointment does not belong to the logged-in provider.");
        }
    }

    private void validateAppointmentTimeReached(
            Appointment appointment) {

        LocalDateTime appointmentDateTime =
                LocalDateTime.of(
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime());

        if (appointmentDateTime.isAfter(
                LocalDateTime.now())) {

            throw new IllegalStateException(
                    "This appointment has not occurred yet.");
        }
    }
}