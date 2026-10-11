package com.group15.health_tracking_system.medication.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "dose_records",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_dose_schedule_date_time",
                    columnNames = {
                        "schedule_id",
                        "dose_date",
                        "scheduled_time"
                    }
            )
        }
)
public class DoseRecord extends BaseEntity {

    /*
     * Date on which this dose was expected.
     */
    @Column(
            name = "dose_date",
            nullable = false
    )
    private LocalDate doseDate;

    /*
     * Exact scheduled time for this dose.
     *
     * This allows multiple doses from the
     * same schedule on the same date.
     *
     * Example:
     * 06:00
     * 12:00
     * 18:00
     */
    @Column(
            name = "scheduled_time",
            nullable = false
    )
    private LocalTime scheduledTime;

    /*
     * Actual date/time when the patient
     * recorded taking the medication.
     *
     * Normally populated only when
     * status = TAKEN.
     */
    @Column(name = "actual_taken_at")
    private LocalDateTime actualTakenAt;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private DoseStatus status;

    /*
     * Medication is retained directly so
     * medication history/query logic can
     * continue to work.
     */
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
     * The schedule that produced this
     * expected dose.
     *
     * Nullable temporarily for older
     * database records created before
     * schedule linking was introduced.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private MedicationSchedule schedule;

    public DoseRecord() {
    }

    public LocalDate getDoseDate() {
        return doseDate;
    }

    public void setDoseDate(
            LocalDate doseDate) {

        this.doseDate = doseDate;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(
            LocalTime scheduledTime) {

        this.scheduledTime = scheduledTime;
    }

    public LocalDateTime getActualTakenAt() {
        return actualTakenAt;
    }

    public void setActualTakenAt(
            LocalDateTime actualTakenAt) {

        this.actualTakenAt = actualTakenAt;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(
            DoseStatus status) {

        this.status = status;
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(
            Medication medication) {

        this.medication = medication;
    }

    public MedicationSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(
            MedicationSchedule schedule) {

        this.schedule = schedule;
    }
}