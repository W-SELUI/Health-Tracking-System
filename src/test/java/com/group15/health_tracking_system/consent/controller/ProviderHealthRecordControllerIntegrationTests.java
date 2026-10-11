package com.group15.health_tracking_system.consent.controller;

import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.health.dto.HealthRecordListItem;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@Transactional
class ProviderHealthRecordControllerIntegrationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    private User patient;
    private User otherPatient;
    private User provider;
    private User otherProvider;

    private HealthRecord weightRecord;
    private HealthRecord otherWeightRecord;
    private BloodPressureRecord bloodPressureRecord;
    private BloodPressureRecord otherBloodPressureRecord;

    private String passwordHash;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        passwordHash = passwordEncoder.encode("ProviderWebTest123!");

        patient = saveUser("Patient", Role.PATIENT);
        otherPatient = saveUser("OtherPatient", Role.PATIENT);
        provider = saveUser("Provider", Role.PROVIDER);
        otherProvider = saveUser("OtherProvider", Role.PROVIDER);

        LocalDateTime observation =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        weightRecord = saveWeight(
                patient,
                "70",
                observation,
                "Weight sample for provider testing."
        );

        bloodPressureRecord = saveBloodPressure(
                patient,
                "120",
                "80",
                observation.plusHours(1),
                "BP demo <b>plain text</b>"
        );

        otherWeightRecord = saveWeight(
                otherPatient,
                "90",
                observation.plusHours(2),
                "Other patient's private weight note."
        );

        otherBloodPressureRecord = saveBloodPressure(
                otherPatient,
                "125",
                "85",
                observation.plusHours(3),
                "Other patient's private blood-pressure note."
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void historyShowsBothRecordTypesWithCorrectLinks() throws Exception {
        grantAccess();

        MvcResult result = getAs(historyUrl(patient), provider)
                .andExpect(status().isOk())
                .andExpect(view().name("consent/provider-health-records"))
                .andExpect(content().string(containsString("70 kg")))
                .andExpect(content().string(containsString("120 / 80 mmHg")))
                .andExpect(content().string(containsString(
                        "href=\"" + recordUrl(patient, weightRecord) + "\""
                )))
                .andExpect(content().string(containsString(
                        "href=\"" + bloodPressureUrl(
                                patient,
                                bloodPressureRecord
                        ) + "\""
                )))
                .andReturn();

        assertNotNull(result.getModelAndView());

        List<?> records = (List<?>) result
                .getModelAndView()
                .getModel()
                .get("records");

        assertNotNull(records);
        assertEquals(2, records.size());

        HealthRecordListItem first =
                (HealthRecordListItem) records.get(0);

        HealthRecordListItem second =
                (HealthRecordListItem) records.get(1);

        // Only this patient's records appear, newest first.
        assertTrue(first.isBloodPressure());
        assertEquals(bloodPressureRecord.getId(), first.getId());

        assertFalse(second.isBloodPressure());
        assertEquals(weightRecord.getId(), second.getId());
    }

    @Test
    void bothDetailPagesRenderAndNotesRemainPlainText() throws Exception {
        grantAccess();

        getAs(recordUrl(patient, weightRecord), provider)
                .andExpect(status().isOk())
                .andExpect(view().name("consent/provider-health-record"))
                .andExpect(content().string(containsString(
                        "Weight sample for provider testing."
                )));

        getAs(bloodPressureUrl(patient, bloodPressureRecord), provider)
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "consent/provider-blood-pressure-record"
                ))
                .andExpect(content().string(containsString("mmHg")))
                .andExpect(content().string(containsString(
                        "BP demo &lt;b&gt;plain text&lt;/b&gt;"
                )))
                .andExpect(content().string(not(containsString(
                        "<b>plain text</b>"
                ))));
    }

    @Test
    void consentingPatientWithoutRecordsShowsEmptyState() throws Exception {
        User emptyPatient = saveUser("EmptyPatient", Role.PATIENT);

        consentService.grantConsent(
                emptyPatient.getId(),
                provider.getId()
        );

        getAs(historyUrl(emptyPatient), provider)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "No health records have been added for this patient yet."
                )));
    }

    @Test
    void missingConsentBlocksHistoryAndBothDetailRoutes() throws Exception {
        for (String url : patientRecordUrls()) {
            getAs(url, provider)
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void anotherProvidersConsentDoesNotGrantAccess() throws Exception {
        grantAccess();

        for (String url : patientRecordUrls()) {
            getAs(url, otherProvider)
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void changingPatientIdCannotReuseConsent() throws Exception {
        grantAccess();

        List<String> otherPatientUrls = List.of(
                historyUrl(otherPatient),
                recordUrl(otherPatient, otherWeightRecord),
                bloodPressureUrl(otherPatient, otherBloodPressureRecord)
        );

        for (String url : otherPatientUrls) {
            getAs(url, provider)
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void anotherPatientsRecordIdReturnsNotFound() throws Exception {
        grantAccess();

        // Keep the authorized patient ID but substitute another record ID.
        getAs(recordUrl(patient, otherWeightRecord), provider)
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString(
                        "Other patient's private weight note."
                ))));

        getAs(
                bloodPressureUrl(patient, otherBloodPressureRecord),
                provider
        )
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString(
                        "Other patient's private blood-pressure note."
                ))));
    }

    @Test
    void revocationBlocksRequestsAndRegrantRestoresAccess() throws Exception {
        grantAccess();

        getAs(bloodPressureUrl(patient, bloodPressureRecord), provider)
                .andExpect(status().isOk());

        consentService.revokeConsent(
                patient.getId(),
                provider.getId()
        );

        for (String url : patientRecordUrls()) {
            getAs(url, provider)
                    .andExpect(status().isForbidden());
        }

        grantAccess();

        for (String url : patientRecordUrls()) {
            getAs(url, provider)
                    .andExpect(status().isOk());
        }
    }

    @Test
    void patientCannotUseProviderRecordRoutes() throws Exception {
        grantAccess();

        for (String url : patientRecordUrls()) {
            getAs(url, patient)
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unauthenticatedRequestsRedirectToLogin() throws Exception {
        for (String url : patientRecordUrls()) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void disabledProviderIsBlockedEvenWithConsent() throws Exception {
        grantAccess();

        provider.setEnabled(false);
        userRepository.saveAndFlush(provider);
        entityManager.clear();

        for (String url : patientRecordUrls()) {
            getAs(url, provider)
                    .andExpect(status().isForbidden());
        }
    }

    private void grantAccess() {
        consentService.grantConsent(
                patient.getId(),
                provider.getId()
        );
    }

    private ResultActions getAs(String url, User viewer) throws Exception {
        return mockMvc.perform(
                get(url).with(
                        user(viewer.getEmail())
                                .roles(viewer.getRole().name())
                )
        );
    }

    private List<String> patientRecordUrls() {
        return List.of(
                historyUrl(patient),
                recordUrl(patient, weightRecord),
                bloodPressureUrl(patient, bloodPressureRecord)
        );
    }

    private String historyUrl(User owner) {
        return "/provider/patients/"
                + owner.getId()
                + "/health-records";
    }

    private String recordUrl(User owner, HealthRecord record) {
        return historyUrl(owner) + "/" + record.getId();
    }

    private String bloodPressureUrl(
            User owner,
            BloodPressureRecord record
    ) {
        return historyUrl(owner)
                + "/blood-pressure/"
                + record.getId();
    }

    private User saveUser(String name, Role role) {
        return userRepository.save(
                new User(
                        name,
                        "Example",
                        "provider-web-" + name + "@example.com",
                        passwordHash,
                        role
                )
        );
    }

    private HealthRecord saveWeight(
            User owner,
            String value,
            LocalDateTime observedAt,
            String notes
    ) {
        return healthRecordRepository.save(
                new HealthRecord(
                        owner,
                        owner,
                        HealthMetricType.WEIGHT,
                        new BigDecimal(value),
                        observedAt,
                        notes,
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }

    private BloodPressureRecord saveBloodPressure(
            User owner,
            String systolic,
            String diastolic,
            LocalDateTime observedAt,
            String notes
    ) {
        return bloodPressureRecordRepository.save(
                new BloodPressureRecord(
                        owner,
                        owner,
                        new BigDecimal(systolic),
                        new BigDecimal(diastolic),
                        observedAt,
                        notes,
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }
}