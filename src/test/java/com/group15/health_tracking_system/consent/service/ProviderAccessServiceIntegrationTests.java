package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProviderAccessServiceIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private ProviderAccessService providerAccessService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patient;
    private User otherPatient;
    private User providerA;
    private User providerB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        String passwordHash = passwordEncoder.encode("ConsentTest123!");

        patient = saveUser(
                "Patient",
                "consent.patient@example.com",
                Role.PATIENT,
                passwordHash
        );

        otherPatient = saveUser(
                "OtherPatient",
                "consent.otherpatient@example.com",
                Role.PATIENT,
                passwordHash
        );

        providerA = saveUser(
                "ProviderA",
                "consent.provider.a@example.com",
                Role.PROVIDER,
                passwordHash
        );

        providerB = saveUser(
                "ProviderB",
                "consent.provider.b@example.com",
                Role.PROVIDER,
                passwordHash
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeConsentAllowsTheMatchingProvider() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA, Role.PROVIDER);

        assertDoesNotThrow(() ->
                providerAccessService.requireActiveConsent(patient.getId())
        );
    }

    @Test
    void missingConsentDeniesAccess() {
        logInAs(providerA, Role.PROVIDER);

        assertThrows(AccessDeniedException.class, () ->
                providerAccessService.requireActiveConsent(patient.getId())
        );
    }

    @Test
    void revokingConsentDeniesAccessWithoutLoggingInAgain() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA, Role.PROVIDER);

        assertDoesNotThrow(() ->
                providerAccessService.requireActiveConsent(patient.getId())
        );

        consentService.revokeConsent(patient.getId(), providerA.getId());

        assertThrows(AccessDeniedException.class, () ->
                providerAccessService.requireActiveConsent(patient.getId())
        );
    }

    @Test
    void providerBCannotUseProviderAsConsent() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerB, Role.PROVIDER);

        assertThrows(AccessDeniedException.class, () ->
                providerAccessService.requireActiveConsent(patient.getId())
        );
    }

    @Test
    void consentForOnePatientDoesNotAllowAccessToAnotherPatient() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA, Role.PROVIDER);

        assertThrows(AccessDeniedException.class, () ->
                providerAccessService.requireActiveConsent(otherPatient.getId())
        );
    }

    @Test
    void patientRoleCannotUseProviderAccess() {
        logInAs(patient, Role.PATIENT);

        assertThrows(AccessDeniedException.class, () ->
                providerAccessService.requireActiveConsent(patient.getId())
        );
    }

    @Test
    void patientClaimingProviderAuthorityIsStillDenied() {
        // Simulate an authority that disagrees with the user's stored role.
        logInAs(patient, Role.PROVIDER);

        assertThrows(AccessDeniedException.class, () ->
                providerAccessService.requireActiveConsent(patient.getId())
        );
    }

    @Test
    void unauthenticatedCallerIsDenied() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                providerAccessService.requireActiveConsent(patient.getId())
        );
    }

    private User saveUser(
            String firstName,
            String email,
            Role role,
            String passwordHash
    ) {
        return userRepository.save(new User(
                firstName,
                "Test",
                email,
                passwordHash,
                role
        ));
    }

    private void logInAs(User account, Role authorityRole) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        account.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + authorityRole.name()
                        ))
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}