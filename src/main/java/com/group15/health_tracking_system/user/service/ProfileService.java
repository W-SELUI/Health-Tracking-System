package com.group15.health_tracking_system.user.service;

import com.group15.health_tracking_system.user.dto.ProfileUpdateRequest;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for viewing and updating an authenticated user's profile.
 *
 * Profile updates intentionally modify only first name and last name.
 * Authentication identity, role and password are not changed here.
 */
@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates an editable profile DTO from the current persisted user.
     */
    @Transactional(readOnly = true)
    public ProfileUpdateRequest createUpdateRequest(User currentUser) {

        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }

        return new ProfileUpdateRequest(
                currentUser.getFirstName(),
                currentUser.getLastName()
        );
    }

    /**
     * Updates the editable fields of the current user's profile.
     */
    @Transactional
    public User updateProfile(
            User currentUser,
            ProfileUpdateRequest request) {

        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }

        if (request == null) {
            throw new IllegalArgumentException("Profile update request is required");
        }

        String firstName = validateAndTrimName(
                request.getFirstName(),
                "First name"
        );

        String lastName = validateAndTrimName(
                request.getLastName(),
                "Last name"
        );

        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);

        return userRepository.save(currentUser);
    }

    private String validateAndTrimName(String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        String trimmed = value.trim();

        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed 100 characters"
            );
        }

        return trimmed;
    }
}