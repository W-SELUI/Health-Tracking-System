package com.group15.health_tracking_system.message.service;

import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.consent.service.ProviderAccessService;
import com.group15.health_tracking_system.message.entity.Message;
import com.group15.health_tracking_system.message.repository.MessageRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MessageServiceIntegrationTests {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private ProviderAccessService providerAccessService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private User patient;
    private User otherPatient;
    private User providerA;
    private User providerB;
    private User admin;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        String passwordHash = passwordEncoder.encode("MessageTest123!");

        patient = saveUser("Patient", Role.PATIENT, passwordHash);
        otherPatient = saveUser("OtherPatient", Role.PATIENT, passwordHash);
        providerA = saveUser("ProviderA", Role.PROVIDER, passwordHash);
        providerB = saveUser("ProviderB", Role.PROVIDER, passwordHash);
        admin = saveUser("Admin", Role.ADMIN, passwordHash);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesTwoWayConversationUsingTheLoggedInSender() {
        logInAs(patient);

        Message first = messageService.sendMessage(
                providerA.getId(),
                "  Hello doctor.  "
        );

        logInAs(providerA);

        Message reply = messageService.sendMessage(
                patient.getId(),
                "Hello, how can I help?"
        );

        messageRepository.flush();

        // Force the following reads to load saved database data.
        entityManager.clear();

        logInAs(patient);
        List<Message> history =
                messageService.getConversation(providerA.getId());

        assertEquals(
                List.of(first.getId(), reply.getId()),
                history.stream().map(Message::getId).toList()
        );

        assertEquals("Hello doctor.", history.get(0).getContent());
        assertEquals(patient.getId(), history.get(0).getSender().getId());
        assertEquals(providerA.getId(), history.get(0).getRecipient().getId());
        assertEquals(providerA.getId(), history.get(1).getSender().getId());
        assertEquals(patient.getId(), history.get(1).getRecipient().getId());
        assertNotNull(history.get(0).getCreatedAt());

        logInAs(providerA);

        assertEquals(
                List.of(first.getId(), reply.getId()),
                messageService.getConversation(patient.getId())
                        .stream()
                        .map(Message::getId)
                        .toList()
        );
    }

    @Test
    void providerCanStartAConversationWithActiveConsent() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        Message sent = messageService.sendMessage(
                patient.getId(),
                "Welcome to your care conversation."
        );

        assertEquals(providerA.getId(), sent.getSender().getId());
        assertEquals(patient.getId(), sent.getRecipient().getId());

        logInAs(patient);

        assertEquals(
                sent.getId(),
                messageService.getConversation(providerA.getId())
                        .get(0)
                        .getId()
        );
    }

    @Test
    void unrelatedProviderCannotReadOrStartAPatientsConversation() {
        logInAs(patient);
        messageService.sendMessage(providerA.getId(), "Private message.");

        logInAs(providerB);

        assertThrows(AccessDeniedException.class, () ->
                messageService.getConversation(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(patient.getId(), "Unauthorized.")
        );

        assertTrue(messageService.getConversationPartners().isEmpty());
    }

    @Test
    void consentDoesNotExposeAnotherProvidersMessages() {
        logInAs(patient);

        Message privateMessage = messageService.sendMessage(
                providerA.getId(),
                "This conversation is with provider A."
        );

        consentService.grantConsent(patient.getId(), providerB.getId());
        logInAs(providerB);

        // Consent permits a new chat, not access to another provider's chat.
        assertTrue(
                messageService.getConversation(patient.getId()).isEmpty()
        );

        Message separateMessage = messageService.sendMessage(
                patient.getId(),
                "This is a separate conversation with provider B."
        );

        assertEquals(
                List.of(separateMessage.getId()),
                messageService.getConversation(patient.getId())
                        .stream()
                        .map(Message::getId)
                        .toList()
        );

        logInAs(providerA);

        assertEquals(
                List.of(privateMessage.getId()),
                messageService.getConversation(patient.getId())
                        .stream()
                        .map(Message::getId)
                        .toList()
        );
    }

    @Test
    void anotherPatientCannotReadSomeoneElsesHistory() {
        logInAs(patient);
        messageService.sendMessage(providerA.getId(), "My private message.");

        logInAs(otherPatient);

        assertTrue(
                messageService.getConversation(providerA.getId()).isEmpty()
        );

        assertThrows(AccessDeniedException.class, () ->
                messageService.getConversation(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(patient.getId(), "Not allowed.")
        );
    }

    @Test
    void rejectsEmptyBlankAndOversizedMessagesWithoutSavingThem() {
        logInAs(patient);
        long countBefore = messageRepository.count();

        String[] invalidMessages = {
            null,
            "",
            " \t\n ",
            "x".repeat(Message.MAX_CONTENT_LENGTH + 1)
        };

        for (String content : invalidMessages) {
            assertThrows(IllegalArgumentException.class, () ->
                    messageService.sendMessage(providerA.getId(), content)
            );
        }

        assertEquals(countBefore, messageRepository.count());
    }

    @Test
    void acceptsAMessageAtTheMaximumLength() {
        logInAs(patient);
        String content = "x".repeat(Message.MAX_CONTENT_LENGTH);

        Message sent = messageService.sendMessage(
                providerA.getId(),
                content
        );

        messageRepository.flush();
        entityManager.clear();

        Message saved = messageRepository.findById(sent.getId())
                .orElseThrow();

        assertEquals(content, saved.getContent());
    }

    @Test
    void revocationBlocksRecordsButExistingMessagingContinues() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        messageService.sendMessage(patient.getId(), "Initial message.");

        consentService.revokeConsent(patient.getId(), providerA.getId());

        assertThrows(AccessDeniedException.class, () ->
                providerAccessService.requireActiveConsent(patient.getId())
        );

        assertEquals(
                1,
                messageService.getConversation(patient.getId()).size()
        );

        messageService.sendMessage(
                patient.getId(),
                "We can still use this existing conversation."
        );

        logInAs(patient);
        messageService.sendMessage(providerA.getId(), "Thank you.");

        assertEquals(
                3,
                messageService.getConversation(providerA.getId()).size()
        );
    }

    @Test
    void revokedConsentDoesNotAllowAProviderToStartANewChat() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        consentService.revokeConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        assertFalse(
                messageService.getAvailableRecipients()
                        .stream()
                        .anyMatch(user -> user.getId().equals(patient.getId()))
        );

        assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(patient.getId(), "New chat.")
        );
    }

    @Test
    void disabledRecipientHistoryRemainsReadableButSendingIsBlocked() {
        logInAs(patient);
        messageService.sendMessage(providerA.getId(), "Existing message.");

        providerA.setEnabled(false);
        userRepository.saveAndFlush(providerA);

        assertEquals(
                1,
                messageService.getConversation(providerA.getId()).size()
        );

        assertTrue(
                messageService.getConversationPartners()
                        .stream()
                        .anyMatch(user -> user.getId().equals(providerA.getId()))
        );

        assertFalse(
                messageService.getAvailableRecipients()
                        .stream()
                        .anyMatch(user -> user.getId().equals(providerA.getId()))
        );

        assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(providerA.getId(), "New message.")
        );
    }

    @Test
    void disabledSenderCannotUseAnExistingLoginForMessaging() {
        logInAs(patient);

        patient.setEnabled(false);
        userRepository.saveAndFlush(patient);

        assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(providerA.getId(), "Not allowed.")
        );

        assertThrows(AccessDeniedException.class, () ->
                messageService.getConversationPartners()
        );
    }

    @Test
    void adminCannotAccessPatientProviderMessaging() {
        logInAs(admin);

        assertThrows(AccessDeniedException.class, () ->
                messageService.getConversation(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(patient.getId(), "Not allowed.")
        );
    }

    @Test
    void unauthenticatedCallerCannotReadOrSendMessages() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                messageService.getConversation(patient.getId())
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                messageService.sendMessage(patient.getId(), "Not allowed.")
        );
    }

    @Test
    void providerContactListsArePrivateAndContainNoDuplicates() {
        consentService.grantConsent(patient.getId(), providerA.getId());

        logInAs(patient);
        messageService.sendMessage(providerA.getId(), "First message.");
        messageService.sendMessage(providerA.getId(), "Second message.");

        logInAs(otherPatient);
        messageService.sendMessage(providerB.getId(), "Separate chat.");

        logInAs(providerA);

        assertEquals(
                List.of(patient.getId()),
                messageService.getAvailableRecipients()
                        .stream()
                        .map(User::getId)
                        .toList()
        );

        assertEquals(
                List.of(patient.getId()),
                messageService.getConversationPartners()
                        .stream()
                        .map(User::getId)
                        .toList()
        );
    }

    private User saveUser(
            String name,
            Role role,
            String passwordHash
    ) {
        return userRepository.save(new User(
                name,
                "Test",
                "message." + name + "@example.com",
                passwordHash,
                role
        ));
    }

    private void logInAs(User account) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        account.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + account.getRole().name()
                        ))
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}