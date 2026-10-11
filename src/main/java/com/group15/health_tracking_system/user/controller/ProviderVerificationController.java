package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.user.entity.ProviderVerification;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import com.group15.health_tracking_system.user.service.ProviderVerificationService;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/provider/verification")
public class ProviderVerificationController {

    private final ProviderVerificationService verificationService;
    private final UserRepository userRepository;

    public ProviderVerificationController(
            ProviderVerificationService verificationService,
            UserRepository userRepository) {

        this.verificationService =
                verificationService;

        this.userRepository =
                userRepository;
    }

    /*
     * Get the currently logged-in provider.
     */
    private User getLoggedInProvider(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                authentication.getName())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Logged-in user was not found."));

        if (user.getRole() != Role.PROVIDER) {

            throw new IllegalStateException(
                    "Only healthcare providers can access this page.");
        }

        return user;
    }

    /*
     * Display the provider's verification status.
     */
    @GetMapping
    public String verificationPage(
            Model model,
            Authentication authentication) {

        User provider =
                getLoggedInProvider(
                        authentication);

        Optional<ProviderVerification> verification =
                verificationService
                        .getByProviderId(
                                provider.getId());

        model.addAttribute(
                "provider",
                provider);

        model.addAttribute(
                "verification",
                verification.orElse(null));

        return "provider/verification";
    }
}