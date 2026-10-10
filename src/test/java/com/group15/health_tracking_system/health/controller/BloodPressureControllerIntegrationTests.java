package com.group15.health_tracking_system.health.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class BloodPressureControllerIntegrationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    private MockMvc mockMvc;

    private User patient;
    private User patientTwo;
    private User provider;

    @BeforeEach
    void setUp() {

        bloodPressureRecordRepository.deleteAll();
        healthRecordRepository.deleteAll();
        userRepository.deleteAll();

        patient = saveUser(
                "Patient",
                "One",
                "patient@example.com",
                Role.PATIENT
        );

        patientTwo = saveUser(
                "Patient",
                "Two",
                "patient2@example.com",
                Role.PATIENT
        );

        provider = saveUser(
                "Provider",
                "One",
                "provider@example.com",
                Role.PROVIDER
        );

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedUserCannotViewBloodPressureRecord() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                patient,
                "120",
                "80",
                HealthRecordSource.PATIENT_REPORTED
        );

        mockMvc.perform(
                        get("/patient/health-records/blood-pressure/"
                                + record.getId())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void providerCannotUsePatientBloodPressureRoutes() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                patient,
                "120",
                "80",
                HealthRecordSource.PATIENT_REPORTED
        );

        mockMvc.perform(
                        get("/patient/health-records/blood-pressure/"
                                + record.getId())
                                .with(user(provider.getEmail())
                                        .roles(Role.PROVIDER.name()))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCanViewOwnBloodPressureRecord() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                patient,
                "120",
                "80",
                HealthRecordSource.PATIENT_REPORTED
        );

        mockMvc.perform(
                        get("/patient/health-records/blood-pressure/"
                                + record.getId())
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/blood-pressure-view"))
                .andExpect(model().attributeExists("record"));
    }

    @Test
    void patientCannotViewAnotherPatientsBloodPressureRecord() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patientTwo,
                patientTwo,
                "125",
                "82",
                HealthRecordSource.PATIENT_REPORTED
        );

        mockMvc.perform(
                        get("/patient/health-records/blood-pressure/"
                                + record.getId())
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void patientCanOpenBloodPressureEditForm() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                patient,
                "118",
                "76",
                HealthRecordSource.PATIENT_REPORTED
        );

        mockMvc.perform(
                        get("/patient/health-records/blood-pressure/"
                                + record.getId()
                                + "/edit")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/blood-pressure-edit"))
                .andExpect(model().attributeExists("bloodPressureRequest"))
                .andExpect(model().attributeExists("recordId"));
    }

    @Test
    void patientCanUpdateBothBloodPressureValues() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                patient,
                "118",
                "76",
                HealthRecordSource.PATIENT_REPORTED
        );

        mockMvc.perform(
                        post("/patient/health-records/blood-pressure/"
                                + record.getId()
                                + "/edit")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("systolic", "122")
                                .param("diastolic", "79")
                                .param("observedAt", "2026-10-09T10:00")
                                .param("notes", "Updated reading")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/health-records?updated"));

        BloodPressureRecord updated =
                bloodPressureRecordRepository
                        .findById(record.getId())
                        .orElseThrow();

        assertEquals(
                0,
                updated.getSystolic().compareTo(new BigDecimal("122"))
        );

        assertEquals(
                0,
                updated.getDiastolic().compareTo(new BigDecimal("79"))
        );

        assertEquals("Updated reading", updated.getNotes());
    }

    @Test
    void invalidBloodPressureEditRequiresBothValues() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                patient,
                "118",
                "76",
                HealthRecordSource.PATIENT_REPORTED
        );

        mockMvc.perform(
                        post("/patient/health-records/blood-pressure/"
                                + record.getId()
                                + "/edit")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("systolic", "122")
                                .param("diastolic", "")
                                .param("observedAt", "2026-10-09T10:00")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/blood-pressure-edit"))
                .andExpect(model().attributeHasFieldErrors(
                        "bloodPressureRequest",
                        "diastolic"
                ));

        BloodPressureRecord unchanged =
                bloodPressureRecordRepository
                        .findById(record.getId())
                        .orElseThrow();

        assertEquals(
                0,
                unchanged.getDiastolic().compareTo(new BigDecimal("76"))
        );
    }

    @Test
    void patientCanDeleteOwnBloodPressureRecord() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                patient,
                "118",
                "76",
                HealthRecordSource.PATIENT_REPORTED
        );

        Long recordId = record.getId();

        mockMvc.perform(
                        post("/patient/health-records/blood-pressure/"
                                + recordId
                                + "/delete")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/health-records?deleted"));

        assertFalse(
                bloodPressureRecordRepository
                        .findById(recordId)
                        .isPresent()
        );
    }

    @Test
    void patientCannotEditProviderRecordedBloodPressure() throws Exception {

        BloodPressureRecord record = saveBloodPressureRecord(
                patient,
                provider,
                "122",
                "78",
                HealthRecordSource.PROVIDER_RECORDED
        );

        mockMvc.perform(
                        get("/patient/health-records/blood-pressure/"
                                + record.getId()
                                + "/edit")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().is4xxClientError());

        assertTrue(
                bloodPressureRecordRepository
                        .findById(record.getId())
                        .isPresent()
        );
    }

    private User saveUser(
            String firstName,
            String lastName,
            String email,
            Role role) {

        return userRepository.save(
                new User(
                        firstName,
                        lastName,
                        email,
                        "test-password-hash",
                        role
                )
        );
    }

    private BloodPressureRecord saveBloodPressureRecord(
            User patientOwner,
            User recordedBy,
            String systolic,
            String diastolic,
            HealthRecordSource source) {

        return bloodPressureRecordRepository.save(
                new BloodPressureRecord(
                        patientOwner,
                        recordedBy,
                        new BigDecimal(systolic),
                        new BigDecimal(diastolic),
                        LocalDateTime.of(
                                2026,
                                10,
                                8,
                                9,
                                0
                        ),
                        null,
                        source
                )
        );
    }
}
