package com.group15.health_tracking_system.medication.controller;

import com.group15.health_tracking_system.medication.entity.Appointment;
import com.group15.health_tracking_system.medication.service.AppointmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String listAppointments(Model model) {
        model.addAttribute("appointments",
                appointmentService.getAll());

        return "appointments";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {

        model.addAttribute("appointment",
                new Appointment());

        return "add-appointment";
    }

    @PostMapping("/save")
    public String saveAppointment(
            @ModelAttribute Appointment appointment) {

        appointmentService.save(appointment);

        return "redirect:/appointments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        Appointment appointment =
                appointmentService.getById(id).orElseThrow();

        model.addAttribute("appointment",
                appointment);

        return "edit-appointment";
    }

    @PostMapping("/update/{id}")
    public String updateAppointment(
            @PathVariable Long id,
            @ModelAttribute Appointment appointment) {

        appointmentService.save(appointment);

        return "redirect:/appointments";
    }

    @GetMapping("/delete/{id}")
    public String deleteAppointment(
            @PathVariable Long id) {

        appointmentService.delete(id);

        return "redirect:/appointments";
    }
}