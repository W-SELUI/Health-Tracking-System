package com.group15.health_tracking_system.user.repository;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence contract for the shared user identity model.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(Role role);
}