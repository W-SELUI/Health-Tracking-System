package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.Appointment;
import com.group15.health_tracking_system.medication.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    public Appointment save(Appointment appointment) {
        return repository.save(appointment);
    }

    // Get appointments belonging to one patient
    public List<Appointment> getByPatientId(Long patientId) {
        return repository.findByPatient_Id(patientId);
    }

    // Get one appointment only if it belongs to the patient
    public Optional<Appointment> getByIdAndPatientId(
            Long id, Long patientId) {
        return repository.findByIdAndPatient_Id(id, patientId);
    }

    // Delete an appointment only if it belongs to the patient
    public void delete(Long id, Long patientId) {
        Appointment appointment = repository
                .findByIdAndPatient_Id(id, patientId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Appointment not found for this patient."));

        repository.delete(appointment);
    }
}
