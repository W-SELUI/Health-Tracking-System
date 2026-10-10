package com.group15.health_tracking_system.medication.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "medication_schedules")
public class MedicationSchedule extends BaseEntity {

    @Column(nullable = false)
    private LocalTime scheduledTime;

    @ManyToOne
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    public MedicationSchedule() {
    }

    public MedicationSchedule(LocalTime scheduledTime, Medication medication) {
        this.scheduledTime = scheduledTime;
        this.medication = medication;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(Medication medication) {
        this.medication = medication;
    }
}