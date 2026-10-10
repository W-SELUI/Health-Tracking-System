package com.group15.health_tracking_system.activity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="activity_log",indexes=@Index(name="ix_activity_patient_date",columnList="patient_id,activity_date"))
public class ActivityLog extends com.group15.health_tracking_system.common.model.BaseEntity {
 // Shared identity: activity records belong to an existing group user.
 @ManyToOne(fetch=FetchType.LAZY,optional=false)
 @JoinColumn(name="patient_id",nullable=false,updatable=false,foreignKey=@ForeignKey(name="fk_activity_patient"))
 private com.group15.health_tracking_system.user.entity.User patient;
 @Column(name="activity_date",nullable=false) private LocalDate activityDate;
 @Column(nullable=false) private Integer steps;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ExerciseType exerciseType;
 private Integer durationMinutes;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal caloriesBurned;
 @Column(length=100) private String customExercise;
 @Column(length=1000) private String notes;
 @Version private Long version;
 protected ActivityLog(){}
 ActivityLog(com.group15.health_tracking_system.user.entity.User patient,ActivityForm form){this.patient=patient;apply(form);}
 void apply(ActivityForm f){activityDate=f.getActivityDate();steps=f.getSteps();exerciseType=f.getExerciseType();durationMinutes=f.getDurationMinutes();caloriesBurned=f.getCaloriesBurned();customExercise=exerciseType==ExerciseType.OTHER?f.getCustomExercise().trim():null;notes=f.getNotes();}
 public Long getPatientId(){return patient.getId();}
 public LocalDate getActivityDate(){return activityDate;} public Integer getSteps(){return steps;}
 public ExerciseType getExerciseType(){return exerciseType;} public Integer getDurationMinutes(){return durationMinutes;}
 public BigDecimal getCaloriesBurned(){return caloriesBurned;} public String getCustomExercise(){return customExercise;}
 public String getNotes(){return notes;} public LocalDateTime getRecordedAt(){return getCreatedAt();}
 public ActivityForm toForm(){ActivityForm f=new ActivityForm();f.setActivityDate(activityDate);f.setSteps(steps);f.setExerciseType(exerciseType);f.setDurationMinutes(durationMinutes);f.setCaloriesBurned(caloriesBurned);f.setCustomExercise(customExercise);f.setNotes(notes);return f;}
}
