package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.Appointment;
import com.group15.health_tracking_system.medication.entity.AppointmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    // Find appointments belonging to a specific patient
    List<Appointment> findByPatient_Id(Long patientId);

    // Find one appointment only if it belongs to that patient
    Optional<Appointment> findByIdAndPatient_Id(
            Long id, Long patientId);

    // Find all appointments belonging to a specific provider
    List<Appointment>
    findByProvider_IdOrderByAppointmentDateAscAppointmentTimeAsc(
            Long providerId);

    // Find appointments for a provider with a specific status
    List<Appointment>
    findByProvider_IdAndStatusOrderByAppointmentDateAscAppointmentTimeAsc(
            Long providerId,
            AppointmentStatus status);
}