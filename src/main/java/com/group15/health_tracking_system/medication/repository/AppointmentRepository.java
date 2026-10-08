package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
}