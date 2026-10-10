package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Appointment;
import com.group15.health_tracking_system.medication.service.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    private final List<String> statuses =
            List.of("SCHEDULED", "COMPLETED", "CANCELLED");

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.getAll());
        return "appointments";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("appointment", new Appointment());
        model.addAttribute("statuses", statuses);
        model.addAttribute("appointmentDateTime", "");
        return "add-appointment";
    }

    @PostMapping("/save")
    public String saveAppointment(
            @RequestParam String doctorName,
            @RequestParam String location,
            @RequestParam String appointmentDateTime,
            @RequestParam String status) {

        LocalDateTime dateTime =
                LocalDateTime.parse(appointmentDateTime);

        Appointment appointment = new Appointment();
        appointment.setDoctorName(doctorName.trim());
        appointment.setLocation(location.trim());
        appointment.setAppointmentDate(dateTime.toLocalDate());
        appointment.setAppointmentTime(dateTime.toLocalTime());
        appointment.setStatus(validateStatus(status));

        appointmentService.save(appointment);
        return "redirect:/appointments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id, Model model) {

        Appointment appointment = appointmentService.getById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Appointment not found"));

        model.addAttribute("appointment", appointment);
        model.addAttribute("statuses", statuses);
        model.addAttribute(
                "appointmentDateTime",
                LocalDateTime.of(
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime()
                ).toString()
        );

        return "edit-appointment";
    }

    @PostMapping("/update/{id}")
    public String updateAppointment(
            @PathVariable Long id,
            @RequestParam String doctorName,
            @RequestParam String location,
            @RequestParam String appointmentDateTime,
            @RequestParam String status) {

        Appointment appointment = appointmentService.getById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Appointment not found"));

        LocalDateTime dateTime =
                LocalDateTime.parse(appointmentDateTime);

        appointment.setDoctorName(doctorName.trim());
        appointment.setLocation(location.trim());
        appointment.setAppointmentDate(dateTime.toLocalDate());
        appointment.setAppointmentTime(dateTime.toLocalTime());
        appointment.setStatus(validateStatus(status));

        appointmentService.save(appointment);
        return "redirect:/appointments";
    }

    @PostMapping("/delete/{id}")
    public String deleteAppointment(@PathVariable Long id) {
        appointmentService.delete(id);
        return "redirect:/appointments";
    }

    private String validateStatus(String status) {
        if (status == null || !statuses.contains(status)) {
            throw new IllegalArgumentException("Invalid appointment status");
        }
        return status;
    }
}
