package com.group15.health_tracking_system.medication.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medication_schedules")
public class MedicationSchedule extends BaseEntity {

    /*
     * First dose time for this schedule.
     *
     * Example:
     * 06:00
     */
    @Column(
            name = "scheduled_time",
            nullable = false
    )
    private LocalTime scheduledTime;

    /*
     * Number of hours between doses.
     *
     * Examples:
     * 6  = every 6 hours
     * 8  = every 8 hours
     * 12 = every 12 hours
     * 24 = once daily
     */
    @Column(
            name = "interval_hours",
            nullable = false
    )
    private Integer intervalHours = 24;

    /*
     * First date on which this schedule applies.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /*
     * Optional final date.
     *
     * Null means the schedule continues indefinitely.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "medication_id",
            nullable = false
    )
    private Medication medication;

    /*
     * Dose records created for this schedule.
     */
    @OneToMany(
            mappedBy = "schedule",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DoseRecord> doseRecords =
            new ArrayList<>();

    public MedicationSchedule() {
    }

    public MedicationSchedule(
            LocalTime scheduledTime,
            Integer intervalHours,
            LocalDate startDate,
            LocalDate endDate,
            Medication medication) {

        this.scheduledTime = scheduledTime;
        this.intervalHours = intervalHours;
        this.startDate = startDate;
        this.endDate = endDate;
        this.medication = medication;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(
            LocalTime scheduledTime) {

        this.scheduledTime = scheduledTime;
    }

    public Integer getIntervalHours() {
        return intervalHours;
    }

    public void setIntervalHours(
            Integer intervalHours) {

        this.intervalHours = intervalHours;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(
            LocalDate startDate) {

        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(
            LocalDate endDate) {

        this.endDate = endDate;
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(
            Medication medication) {

        this.medication = medication;
    }

    public List<DoseRecord> getDoseRecords() {
        return doseRecords;
    }

    public void setDoseRecords(
            List<DoseRecord> doseRecords) {

        this.doseRecords = doseRecords;
    }

    public void addDoseRecord(
            DoseRecord doseRecord) {

        doseRecords.add(doseRecord);
        doseRecord.setSchedule(this);
        doseRecord.setMedication(this.medication);
    }
}