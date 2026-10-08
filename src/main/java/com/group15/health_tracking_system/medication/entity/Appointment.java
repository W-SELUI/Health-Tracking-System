package com.group15.health_tracking_system.medication.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Column(nullable = false)
    private String doctorName;

    private String purpose;

    public Appointment() {
    }

    public Appointment(LocalDate appointmentDate,
                       LocalTime appointmentTime,
                       String doctorName,
                       String purpose) {
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.doctorName = doctorName;
        this.purpose = purpose;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}