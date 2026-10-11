package com.group15.health_tracking_system.user.service;

import com.group15.health_tracking_system.user.dto.ProfileUpdateRequest;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Business logic for viewing and updating an authenticated user's profile.
 *
 * Profile updates intentionally modify only first name and last name.
 * Authentication identity, role and password are not changed here.
 */
@Service
public class ProfileService {

    private static final long MAX_COVER_PHOTO_SIZE = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_COVER_PHOTO_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

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

    /**
     * Stores a cover photo for any authenticated account role.
     */
    @Transactional
    public User updateCoverPhoto(
            User currentUser,
            MultipartFile coverPhoto) {

        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }

        if (coverPhoto == null || coverPhoto.isEmpty()) {
            throw new IllegalArgumentException("Please choose a cover photo.");
        }

        if (coverPhoto.getSize() > MAX_COVER_PHOTO_SIZE) {
            throw new IllegalArgumentException(
                    "Cover photos must be 5 MB or smaller."
            );
        }

        String contentType = coverPhoto.getContentType();
        String normalizedContentType = contentType == null
                ? ""
                : contentType.toLowerCase(Locale.ROOT);

        if (!ALLOWED_COVER_PHOTO_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException(
                    "Use a JPG, PNG or WebP image for your cover photo."
            );
        }

        try {
            currentUser.setCoverPhotoData(coverPhoto.getBytes());
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "The cover photo could not be read. Please try another image.",
                    exception
            );
        }

        currentUser.setCoverPhotoContentType(normalizedContentType);

        return userRepository.save(currentUser);
    }

    @Transactional
    public User removeCoverPhoto(User currentUser) {

        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }

        currentUser.setCoverPhotoData(null);
        currentUser.setCoverPhotoContentType(null);

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
