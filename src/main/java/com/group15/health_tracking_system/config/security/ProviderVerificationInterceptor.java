package com.group15.health_tracking_system.config.security;

import com.group15.health_tracking_system.user.entity.ProviderVerification;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.entity.VerificationStatus;
import com.group15.health_tracking_system.user.repository.UserRepository;
import com.group15.health_tracking_system.user.service.ProviderVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ProviderVerificationInterceptor
        implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final ProviderVerificationService verificationService;

    public ProviderVerificationInterceptor(
            UserRepository userRepository,
            ProviderVerificationService verificationService) {

        this.userRepository = userRepository;
        this.verificationService = verificationService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        /*
         * Always allow the verification status page.
         * This prevents an endless redirect loop.
         */
        String requestUri =
                request.getRequestURI();

        if (requestUri.equals(
                "/provider/verification")) {

            return true;
        }

        /*
         * Get the logged-in user's email.
         */
        String username =
                request.getUserPrincipal()
                        .getName();

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                username)
                        .orElse(null);

        /*
         * If the user is not a provider,
         * let Spring Security handle access normally.
         */
        if (user == null
                || user.getRole()
                != Role.PROVIDER) {

            return true;
        }

        Optional<ProviderVerification> verification =
                verificationService
                        .getByProviderId(
                                user.getId());

        /*
         * Provider has no verification record yet.
         */
        if (verification.isEmpty()) {

            response.sendRedirect(
                    "/provider/verification");

            return false;
        }

        /*
         * Pending or rejected providers may only
         * access the verification status page.
         */
        if (verification.get().getStatus()
                != VerificationStatus.VERIFIED) {

            response.sendRedirect(
                    "/provider/verification");

            return false;
        }

        /*
         * Verified provider can continue normally.
         */
        return true;
    }
}