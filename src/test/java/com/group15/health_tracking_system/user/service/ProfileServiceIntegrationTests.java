package com.group15.health_tracking_system.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group15.health_tracking_system.user.dto.ProfileUpdateRequest;
import com.group15.health_tracking_system.user.dto.RegistrationRequest;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProfileServiceIntegrationTests {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void profileUpdatePersistsTrimmedNames() {
        User user = createPatient();

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        "  Updated  ",
                        "  Patient  "
                );

        User updated = profileService.updateProfile(user, request);

        assertEquals("Updated", updated.getFirstName());
        assertEquals("Patient", updated.getLastName());

        User persisted = userRepository
                .findByEmailIgnoreCase("patient@example.com")
                .orElseThrow();

        assertEquals("Updated", persisted.getFirstName());
        assertEquals("Patient", persisted.getLastName());
    }

    @Test
    void profileUpdateDoesNotChangeEmailRoleOrPassword() {
        User user = createPatient();

        String originalEmail = user.getEmail();
        Role originalRole = user.getRole();
        String originalPasswordHash = user.getPasswordHash();

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        "Changed",
                        "Name"
                );

        User updated = profileService.updateProfile(user, request);

        assertEquals(originalEmail, updated.getEmail());
        assertEquals(originalRole, updated.getRole());
        assertEquals(originalPasswordHash, updated.getPasswordHash());
    }

    @Test
    void blankFirstNameIsRejected() {
        User user = createPatient();

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        "   ",
                        "Patient"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.updateProfile(user, request)
        );
    }

    @Test
    void blankLastNameIsRejected() {
        User user = createPatient();

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        "Test",
                        "   "
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.updateProfile(user, request)
        );
    }

    @Test
    void nameLongerThanMaximumLengthIsRejected() {
        User user = createPatient();

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        "A".repeat(101),
                        "Patient"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.updateProfile(user, request)
        );
    }

    private User createPatient() {
        RegistrationRequest request = new RegistrationRequest();

        request.setFirstName("Test");
        request.setLastName("Patient");
        request.setEmail("patient@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole(Role.PATIENT);

        return userService.register(request);
    }
}