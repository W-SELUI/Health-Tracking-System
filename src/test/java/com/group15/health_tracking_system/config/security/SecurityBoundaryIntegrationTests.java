package com.group15.health_tracking_system.config.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class SecurityBoundaryIntegrationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User patient;
    private User provider;
    private User admin;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        patient = saveUser("patient@example.com", Role.PATIENT);
        provider = saveUser("provider@example.com", Role.PROVIDER);
        admin = saveUser("admin@example.com", Role.ADMIN);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/patient/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void patientCanOpenPatientAreaButNotProviderArea() throws Exception {
        mockMvc.perform(get("/patient/dashboard")
                        .with(user(patient.getEmail()).roles(Role.PATIENT.name())))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/patient"));

        mockMvc.perform(get("/provider/dashboard")
                        .with(user(patient.getEmail()).roles(Role.PATIENT.name())))
                .andExpect(status().isForbidden());
    }

    @Test
    void providerCanOpenProviderAreaButNotAdminArea() throws Exception {
        mockMvc.perform(get("/provider/dashboard")
                        .with(user(provider.getEmail()).roles(Role.PROVIDER.name())))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/provider"));

        mockMvc.perform(get("/admin/dashboard")
                        .with(user(provider.getEmail()).roles(Role.PROVIDER.name())))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanOpenAdminAreaButNotPatientArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user(admin.getEmail()).roles(Role.ADMIN.name())))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"));

        mockMvc.perform(get("/patient/dashboard")
                        .with(user(admin.getEmail()).roles(Role.ADMIN.name())))
                .andExpect(status().isForbidden());
    }

    @Test
    void sharedDashboardRedirectsAccordingToPersistedRole() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user(patient.getEmail()).roles(Role.PATIENT.name())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/dashboard"));

        mockMvc.perform(get("/dashboard")
                        .with(user(provider.getEmail()).roles(Role.PROVIDER.name())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/provider/dashboard"));

        mockMvc.perform(get("/dashboard")
                        .with(user(admin.getEmail()).roles(Role.ADMIN.name())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    private User saveUser(String email, Role role) {
        return userRepository.save(new User(
                "Test",
                role.getDisplayName(),
                email,
                passwordEncoder.encode("password123"),
                role
        ));
    }
}