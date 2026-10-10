package com.group15.health_tracking_system.activity;
import com.group15.health_tracking_system.user.entity.User;
public interface CurrentPatient {
 User requirePatient();
 default Long requirePatientId() { return requirePatient().getId(); }
}
