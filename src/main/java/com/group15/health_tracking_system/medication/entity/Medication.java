package com.group15.health_tracking_system.medication.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import com.group15.health_tracking_system.user.entity.User;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "medications")
public class Medication extends BaseEntity {

    @Column(nullable = false)
    private String medicationName;

    @Column(nullable = false)
    private String dosage;

    private String instructions;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL)
    private List<MedicationSchedule> schedules;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL)
    private List<DoseRecord> doseRecords;

    public Medication() {
    }

    public Medication(String medicationName,
                      String dosage,
                      String instructions,
                      User user) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.instructions = instructions;
        this.user = user;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<MedicationSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<MedicationSchedule> schedules) {
        this.schedules = schedules;
    }

    public List<DoseRecord> getDoseRecords() {
        return doseRecords;
    }

    public void setDoseRecords(List<DoseRecord> doseRecords) {
        this.doseRecords = doseRecords;
    }
}