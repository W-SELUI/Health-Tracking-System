package com.group15.health_tracking_system.activity;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class AuthenticatedPatient implements CurrentPatient {
 private final CurrentUserService users;
 public AuthenticatedPatient(CurrentUserService users) { this.users = users; }
 public User requirePatient() {
  User user = users.getCurrentUser();
  if (!user.isEnabled() || user.getRole() != Role.PATIENT)
   throw new AccessDeniedException("Patient account required");
  return user;
 }
}
