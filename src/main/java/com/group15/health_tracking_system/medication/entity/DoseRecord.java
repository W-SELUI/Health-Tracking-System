package com.group15.health_tracking_system.medication.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dose_records")
public class DoseRecord extends BaseEntity {

    @Column(nullable = false)
    private LocalDate doseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DoseStatus status;

    @ManyToOne
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    public DoseRecord() {
    }

    public LocalDate getDoseDate() {
        return doseDate;
    }

    public void setDoseDate(LocalDate doseDate) {
        this.doseDate = doseDate;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        this.status = status;
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(Medication medication) {
        this.medication = medication;
    }
}