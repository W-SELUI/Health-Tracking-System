package com.group15.health_tracking_system.activity;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
/** User input only: ownership is always derived from the authenticated session. */
public class ActivityForm {
 @NotNull @PastOrPresent @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate activityDate;
 @NotNull @Min(0) private Integer steps;
 @NotNull private ExerciseType exerciseType;
 @Min(0) private Integer durationMinutes;
 @NotNull @DecimalMin("0.0") @Digits(integer=8,fraction=2) private BigDecimal caloriesBurned;
 @Size(max=100) private String customExercise;
 @Size(max=1000) private String notes;
 @AssertTrue(message="Enter a custom exercise name when choosing OTHER.")
 public boolean isCustomExerciseValid(){return exerciseType!=ExerciseType.OTHER || (customExercise!=null&&!customExercise.isBlank());}
 public LocalDate getActivityDate(){return activityDate;} public void setActivityDate(LocalDate v){activityDate=v;}
 public Integer getSteps(){return steps;} public void setSteps(Integer v){steps=v;}
 public ExerciseType getExerciseType(){return exerciseType;} public void setExerciseType(ExerciseType v){exerciseType=v;}
 public Integer getDurationMinutes(){return durationMinutes;} public void setDurationMinutes(Integer v){durationMinutes=v;}
 public BigDecimal getCaloriesBurned(){return caloriesBurned;} public void setCaloriesBurned(BigDecimal v){caloriesBurned=v;}
 public String getCustomExercise(){return customExercise;} public void setCustomExercise(String v){customExercise=v;}
 public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
