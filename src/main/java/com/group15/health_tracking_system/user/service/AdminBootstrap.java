package com.group15.health_tracking_system.user.service;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the initial administrator account from trusted environment
 * configuration when one does not already exist.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.first-name:System}")
    private String adminFirstName;

    @Value("${app.bootstrap.admin.last-name:Administrator}")
    private String adminLastName;

    public AdminBootstrap(
            UserRepository userRepository,
            UserService userService) {

        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {

        boolean emailConfigured =
                adminEmail != null && !adminEmail.isBlank();

        boolean passwordConfigured =
                adminPassword != null && !adminPassword.isBlank();

        // No administrator bootstrap requested.
        if (!emailConfigured && !passwordConfigured) {
            return;
        }

        // Both values are required.
        if (!emailConfigured || !passwordConfigured) {
            LOGGER.warn(
                    "Administrator bootstrap skipped: both email and password must be configured."
            );
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();

        Optional<User> existingUser =
                userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUser.isPresent()) {

            User user = existingUser.get();

            if (user.getRole() == Role.ADMIN) {
                LOGGER.info(
                        "Administrator bootstrap skipped: administrator account already exists."
                );
            } else {
                LOGGER.warn(
                        "Administrator bootstrap skipped: {} already belongs to role {}.",
                        normalizedEmail,
                        user.getRole()
                );
            }

            return;
        }

        userService.createAdministrator(
                adminFirstName,
                adminLastName,
                normalizedEmail,
                adminPassword
        );

        LOGGER.info(
                "Administrator account created for {}.",
                normalizedEmail
        );
    }
}