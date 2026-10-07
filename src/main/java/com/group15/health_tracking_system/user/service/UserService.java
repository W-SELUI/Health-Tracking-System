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

//Handles user account tasks.

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /*
      Registers a Patient or Healthcare Provider account.
     
      Administrator accounts cannot be created through public registration.
     */
    
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

    /*
      Creates an Administrator account through a trusted application path.
     
      This method is intended for administrator bootstrap and is not exposed
      through public registration.
     */
    
    @Transactional
    public User createAdministrator(
            String firstName,
            String lastName,
            String email,
            String rawPassword) {

        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrator first name is required"
            );
        }

        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrator last name is required"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrator email is required"
            );
        }

        if (rawPassword == null
                || rawPassword.length() < 8
                || rawPassword.length() > 72) {

            throw new IllegalArgumentException(
                    "Administrator password must be between 8 and 72 characters"
            );
        }

        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "An account with this email already exists"
            );
        }

        User administrator = new User(
                firstName.trim(),
                lastName.trim(),
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                Role.ADMIN
        );

        return userRepository.save(administrator);
    }

    /*
     Normalizes email addresses so identity comparisons are consistent.
     */
    
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}