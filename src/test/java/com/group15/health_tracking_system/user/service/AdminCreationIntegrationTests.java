package com.group15.health_tracking_system.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.common.exception.DuplicateResourceException;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminCreationIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void trustedBootstrapPathCreatesHashedAdministrator() {
        User admin = userService.createAdministrator(
                "System",
                "Administrator",
                "  ADMIN@Example.com  ",
                "strongPassword123"
        );

        assertEquals("admin@example.com", admin.getEmail());
        assertEquals(Role.ADMIN, admin.getRole());
        assertNotEquals("strongPassword123", admin.getPasswordHash());

        assertTrue(
                passwordEncoder.matches(
                        "strongPassword123",
                        admin.getPasswordHash()
                )
        );
    }

    @Test
    void administratorPasswordMustMeetSharedLengthRule() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createAdministrator(
                        "System",
                        "Administrator",
                        "admin-short@example.com",
                        "short"
                )
        );
    }

    @Test
    void administratorEmailCannotDuplicateExistingAccount() {
        userService.createAdministrator(
                "System",
                "Administrator",
                "admin-duplicate@example.com",
                "strongPassword123"
        );

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.createAdministrator(
                        "Another",
                        "Admin",
                        "ADMIN-DUPLICATE@example.com",
                        "anotherPassword123"
                )
        );
    }
}