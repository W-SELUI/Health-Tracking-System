package com.group15.health_tracking_system.activity;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.*;
import java.util.*;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service @Transactional
public class ActivityService {
 private final ActivityLogRepository repository; private final CurrentPatient currentPatient; private final Validator validator;
 public ActivityService(ActivityLogRepository r,CurrentPatient p,Validator v){repository=r;currentPatient=p;validator=v;}
 private void validate(ActivityForm f){var errors=validator.validate(f);if(!errors.isEmpty())throw new ConstraintViolationException(errors);}
 public ActivityLog create(ActivityForm form){var patient=currentPatient.requirePatient();validate(form);return repository.save(new ActivityLog(patient,form));}
 @Transactional(readOnly=true) public ActivityLog get(Long id){return owned(id,currentPatient.requirePatientId());}
 private ActivityLog owned(Long id,Long patient){return repository.findByIdAndPatient_Id(id,patient).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Activity not found"));}
 public ActivityLog edit(Long id,ActivityForm form){ActivityLog log=owned(id,currentPatient.requirePatientId());validate(form);log.apply(form);return repository.save(log);}
 public void delete(Long id){repository.delete(owned(id,currentPatient.requirePatientId()));}
 @Transactional(readOnly=true) public List<ActivityLog> history(LocalDate start,LocalDate end){checkRange(start,end);return repository.findByPatient_IdAndActivityDateBetweenOrderByActivityDateDescIdDesc(currentPatient.requirePatientId(),start,end);}
 /** Patient-facing helper. Provider analytics must use William's consent guard before querying data. */
 @Transactional(readOnly=true) public ActivitySummary summary(LocalDate start,LocalDate end){
  var logs=history(start,end);long days=ChronoUnit.DAYS.between(start,end)+1;
  long steps=logs.stream().mapToLong(ActivityLog::getSteps).sum();
  long minutes=logs.stream().mapToLong(a->a.getDurationMinutes()==null?0:a.getDurationMinutes()).sum();
  BigDecimal calories=logs.stream().map(ActivityLog::getCaloriesBurned).reduce(BigDecimal.ZERO,BigDecimal::add);
  long recordedDays=logs.stream().map(ActivityLog::getActivityDate).distinct().count();
  return new ActivitySummary(logs.size(),recordedDays,days,steps,minutes,calories,BigDecimal.valueOf(steps).divide(BigDecimal.valueOf(days),2,RoundingMode.HALF_UP));
 }
 public static void checkRange(LocalDate start,LocalDate end){if(start==null||end==null||start.isAfter(end))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid date range: start must be on or before end.");}
}
