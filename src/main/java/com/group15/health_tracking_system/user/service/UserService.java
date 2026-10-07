package com.group15.health_tracking_system.user.service;

import com.group15.health_tracking_system.common.exception.DuplicateResourceException;
import com.group15.health_tracking_system.user.dto.RegistrationRequest;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared account business logic.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationRequest request) {
        if (request.getRole() == null || request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Only patient and healthcare provider accounts can self-register"
            );
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "An account with this email already exists"
            );
        }

        User user = new User(
                request.getFirstName().trim(),
                request.getLastName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getRole()
        );

        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}