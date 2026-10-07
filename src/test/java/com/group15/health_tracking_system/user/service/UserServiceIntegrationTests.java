package com.group15.health_tracking_system.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.common.exception.DuplicateResourceException;
import com.group15.health_tracking_system.user.dto.RegistrationRequest;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServiceIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationStoresNormalizedEmailAndHashedPassword() {
        RegistrationRequest request =
                registration("  Patient.One@Example.com  ", Role.PATIENT);

        User saved = userService.register(request);

        assertEquals("patient.one@example.com", saved.getEmail());
        assertEquals(Role.PATIENT, saved.getRole());
        assertNotEquals("password123", saved.getPasswordHash());
        assertTrue(
                passwordEncoder.matches(
                        "password123",
                        saved.getPasswordHash()
                )
        );

        assertTrue(
                userRepository
                        .findByEmailIgnoreCase("PATIENT.ONE@EXAMPLE.COM")
                        .isPresent()
        );
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() {
        userService.register(
                registration("duplicate@example.com", Role.PATIENT)
        );

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.register(
                        registration("DUPLICATE@example.com", Role.PROVIDER)
                )
        );
    }

    @Test
    void publicRegistrationCannotCreateAdministrator() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(
                        registration("admin@example.com", Role.ADMIN)
                )
        );
    }

    private RegistrationRequest registration(String email, Role role) {
        RegistrationRequest request = new RegistrationRequest();

        request.setFirstName("Test");
        request.setLastName("User");
        request.setEmail(email);
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole(role);

        return request;
    }
}