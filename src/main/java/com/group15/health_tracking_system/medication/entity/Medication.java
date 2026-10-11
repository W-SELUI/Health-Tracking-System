package com.group15.health_tracking_system.medication.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import com.group15.health_tracking_system.user.entity.User;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medications")
public class Medication extends BaseEntity {

    @Column(
            name = "medication_name",
            nullable = false,
            length = 150
    )
    private String medicationName;

    @Column(
            nullable = false,
            length = 100
    )
    private String dosage;

    @Column(
            length = 500
    )
    private String instructions;

    /*
     * The patient who owns this medication.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    /*
     * A medication can have multiple scheduled times.
     *
     * Example:
     * Panadol
     * 08:00
     * 20:00
     */
    @OneToMany(
            mappedBy = "medication",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MedicationSchedule> schedules =
            new ArrayList<>();

    /*
     * Dose history belonging to this medication.
     */
    @OneToMany(
            mappedBy = "medication",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DoseRecord> doseRecords =
            new ArrayList<>();

    public Medication() {
    }

    public Medication(
            String medicationName,
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

    public void setMedicationName(
            String medicationName) {

        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(
            String dosage) {

        this.dosage = dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(
            String instructions) {

        this.instructions = instructions;
    }

    public User getUser() {
        return user;
    }

    public void setUser(
            User user) {

        this.user = user;
    }

    public List<MedicationSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(
            List<MedicationSchedule> schedules) {

        this.schedules = schedules;
    }

    public List<DoseRecord> getDoseRecords() {
        return doseRecords;
    }

    public void setDoseRecords(
            List<DoseRecord> doseRecords) {

        this.doseRecords = doseRecords;
    }

    /*
     * Helper method for safely attaching a schedule
     * to this medication.
     */
    public void addSchedule(
            MedicationSchedule schedule) {

        schedules.add(schedule);
        schedule.setMedication(this);
    }

    /*
     * Helper method for safely attaching a dose record
     * to this medication.
     */
    public void addDoseRecord(
            DoseRecord doseRecord) {

        doseRecords.add(doseRecord);
        doseRecord.setMedication(this);
    }
}