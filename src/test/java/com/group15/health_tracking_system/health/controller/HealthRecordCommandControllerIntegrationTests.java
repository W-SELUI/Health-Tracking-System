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
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
class HealthRecordCommandControllerIntegrationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    private MockMvc mockMvc;

    private User patientOne;
    private User patientTwo;
    private User provider;

    @BeforeEach
    void setUp() {

        bloodPressureRecordRepository.deleteAll();
        healthRecordRepository.deleteAll();
        userRepository.deleteAll();

        patientOne = saveUser(
                "Patient",
                "One",
                "patient1@example.com",
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
                "User",
                "provider@example.com",
                Role.PROVIDER
        );

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedUserCannotOpenCreateForm() throws Exception {

        mockMvc.perform(
                        get("/patient/health-records/new")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void providerCannotOpenPatientCreateForm() throws Exception {

        mockMvc.perform(
                        get("/patient/health-records/new")
                                .with(user(provider.getEmail())
                                        .roles(Role.PROVIDER.name()))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCanOpenUnifiedCreateForm() throws Exception {

        mockMvc.perform(
                        get("/patient/health-records/new")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/create"))
                .andExpect(model().attributeExists("healthRecordCreateRequest"))
                .andExpect(model().attributeExists("metricOptions"));
    }

    @Test
    void patientCanCreateScalarHealthRecordFromUnifiedForm() throws Exception {

        mockMvc.perform(
                        post("/patient/health-records")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("metricType", "HEART_RATE")
                                .param("metricValue", "72")
                                .param("observedAt", "2026-10-09T09:30")
                                .param("notes", "Morning reading")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/health-records?created"));

        List<HealthRecord> records =
                healthRecordRepository
                        .findByPatientOrderByObservedAtDesc(patientOne);

        assertEquals(1, records.size());
        assertTrue(bloodPressureRecordRepository.findAll().isEmpty());

        HealthRecord saved = records.get(0);

        assertEquals(patientOne.getId(), saved.getPatient().getId());
        assertEquals(patientOne.getId(), saved.getRecordedBy().getId());
        assertEquals(HealthRecordSource.PATIENT_REPORTED, saved.getSource());
        assertEquals(HealthMetricType.HEART_RATE, saved.getMetricType());
    }

    @Test
    void patientCanCreateBloodPressureFromUnifiedForm() throws Exception {

        mockMvc.perform(
                        post("/patient/health-records")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("metricType", "BLOOD_PRESSURE")
                                .param("systolic", "118")
                                .param("diastolic", "76")
                                .param("observedAt", "2026-10-09T09:30")
                                .param("notes", "Morning blood pressure")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/health-records?created"));

        List<BloodPressureRecord> records =
                bloodPressureRecordRepository
                        .findByPatientOrderByObservedAtDesc(patientOne);

        assertEquals(1, records.size());
        assertTrue(healthRecordRepository.findAll().isEmpty());

        BloodPressureRecord saved = records.get(0);

        assertEquals(
                0,
                saved.getSystolic().compareTo(new BigDecimal("118"))
        );

        assertEquals(
                0,
                saved.getDiastolic().compareTo(new BigDecimal("76"))
        );

        assertEquals(patientOne.getId(), saved.getPatient().getId());
        assertEquals(patientOne.getId(), saved.getRecordedBy().getId());
        assertEquals(HealthRecordSource.PATIENT_REPORTED, saved.getSource());
    }

    @Test
    void scalarRecordCannotBeCreatedWithoutMeasurementValue() throws Exception {

        mockMvc.perform(
                        post("/patient/health-records")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("metricType", "HEART_RATE")
                                .param("metricValue", "")
                                .param("observedAt", "2026-10-09T09:30")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/create"))
                .andExpect(model().attributeHasFieldErrors(
                        "healthRecordCreateRequest",
                        "metricValue"
                ));

        assertTrue(healthRecordRepository.findAll().isEmpty());
        assertTrue(bloodPressureRecordRepository.findAll().isEmpty());
    }

    @Test
    void bloodPressureCannotBeCreatedWithoutDiastolic() throws Exception {

        mockMvc.perform(
                        post("/patient/health-records")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("metricType", "BLOOD_PRESSURE")
                                .param("systolic", "118")
                                .param("diastolic", "")
                                .param("observedAt", "2026-10-09T09:30")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/create"))
                .andExpect(model().attributeHasFieldErrors(
                        "healthRecordCreateRequest",
                        "diastolic"
                ));

        assertTrue(healthRecordRepository.findAll().isEmpty());
        assertTrue(bloodPressureRecordRepository.findAll().isEmpty());
    }

    @Test
    void patientCanOpenEditFormForOwnScalarRecord() throws Exception {

        HealthRecord record = savePatientRecord(patientOne);

        mockMvc.perform(
                        get("/patient/health-records/"
                                + record.getId()
                                + "/edit")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/edit"))
                .andExpect(model().attributeExists("healthRecordRequest"))
                .andExpect(model().attributeExists("recordId"))
                .andExpect(model().attributeExists("metricTypes"));
    }

    @Test
    void patientCanUpdateOwnScalarRecord() throws Exception {

        HealthRecord record = savePatientRecord(patientOne);

        mockMvc.perform(
                        post("/patient/health-records/"
                                + record.getId()
                                + "/edit")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("metricType", "WEIGHT")
                                .param("metricValue", "74.5")
                                .param("observedAt", "2026-10-09T10:00")
                                .param("notes", "Updated")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/health-records?updated"));

        HealthRecord updated =
                healthRecordRepository
                        .findById(record.getId())
                        .orElseThrow();

        assertEquals(HealthMetricType.WEIGHT, updated.getMetricType());
        assertEquals(
                0,
                updated.getMetricValue().compareTo(new BigDecimal("74.5"))
        );
        assertEquals(HealthRecordSource.PATIENT_REPORTED, updated.getSource());
    }

    @Test
    void patientCanDeleteOwnScalarRecord() throws Exception {

        HealthRecord record = savePatientRecord(patientOne);
        Long recordId = record.getId();

        mockMvc.perform(
                        post("/patient/health-records/"
                                + recordId
                                + "/delete")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/health-records?deleted"));

        assertFalse(healthRecordRepository.findById(recordId).isPresent());
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

    private HealthRecord savePatientRecord(User patient) {

        return healthRecordRepository.save(
                new HealthRecord(
                        patient,
                        patient,
                        HealthMetricType.WEIGHT,
                        new BigDecimal("75.0"),
                        LocalDateTime.of(
                                2026,
                                10,
                                8,
                                9,
                                0
                        ),
                        null,
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }
}
