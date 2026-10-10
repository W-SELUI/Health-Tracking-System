package com.group15.health_tracking_system.message.service;

import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.message.entity.Message;
import com.group15.health_tracking_system.message.repository.MessageRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends patient-provider messages and restricts conversation access
 * to the signed-in user's own conversations.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER')")
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ConsentService consentService;

    public MessageService(
            MessageRepository messageRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ConsentService consentService
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.consentService = consentService;
    }

    /**
     * Returns the signed-in user's existing conversation partners.
     * Disabled partners remain visible so history can still be read.
     */
    public List<User> getConversationPartners() {
        User currentUser = getMessagingUser();

        return messageRepository
                .findConversationPartners(currentUser.getId())
                .stream()
                .filter(partner -> hasOppositeRole(currentUser, partner))
                .toList();
    }

    /**
     * Returns people the signed-in user can currently message.
     */
    public List<User> getAvailableRecipients() {
        User currentUser = getMessagingUser();

        if (currentUser.getRole() == Role.PATIENT) {
            return userRepository
                    .findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(
                            Role.PROVIDER
                    );
        }

        List<User> candidates = new ArrayList<>(
                consentService.getAuthorizedPatients(currentUser.getId())
        );

        candidates.addAll(
                messageRepository.findConversationPartners(currentUser.getId())
        );

        Map<Long, User> uniquePatients = new LinkedHashMap<>();

        for (User candidate : candidates) {
            if (candidate.getRole() == Role.PATIENT
                    && candidate.isEnabled()) {
                uniquePatients.put(candidate.getId(), candidate);
            }
        }

        return uniquePatients.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                User::getFullName,
                                String.CASE_INSENSITIVE_ORDER
                        ).thenComparing(User::getId)
                )
                .toList();
    }

    /**
     * Checks access before returning the other person's details.
     */
    public User getConversationPartner(Long otherUserId) {
        User currentUser = getMessagingUser();
        return requireConversationPartner(currentUser, otherUserId);
    }

    /**
     * Loads only messages exchanged between the signed-in user
     * and the selected partner.
     */
    public List<Message> getConversation(Long otherUserId) {
        User currentUser = getMessagingUser();
        User partner = requireConversationPartner(currentUser, otherUserId);

        return messageRepository.findConversation(
                currentUser.getId(),
                partner.getId()
        );
    }

    /**
     * The sender always comes from the authenticated session.
     */
    @Transactional
    public Message sendMessage(Long recipientId, String content) {
        User sender = getMessagingUser();
        User recipient = requireConversationPartner(sender, recipientId);

        if (!recipient.isEnabled()) {
            throw new AccessDeniedException(
                    "This account is not currently available for messaging."
            );
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty."
            );
        }

        if (content.length() > Message.MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Message must be at most 4,000 characters."
            );
        }

        Message message = new Message(
                sender,
                recipient,
                content.strip()
        );

        return messageRepository.save(message);
    }

    private User getMessagingUser() {
        User currentUser = currentUserService.getCurrentUser();

        if (!currentUser.isEnabled()
                || (currentUser.getRole() != Role.PATIENT
                && currentUser.getRole() != Role.PROVIDER)) {
            throw new AccessDeniedException(
                    "Your account cannot access patient-provider messaging."
            );
        }

        return currentUser;
    }

    private User requireConversationPartner(
            User currentUser,
            Long otherUserId
    ) {
        if (otherUserId == null
                || otherUserId.equals(currentUser.getId())) {
            throw conversationDenied();
        }

        User partner = userRepository.findById(otherUserId)
                .orElseThrow(() -> conversationDenied());

        if (!hasOppositeRole(currentUser, partner)) {
            throw conversationDenied();
        }

        // Existing participants retain access to their conversation.
        if (messageRepository.existsConversation(
                currentUser.getId(),
                partner.getId()
        )) {
            return partner;
        }

        // A new conversation requires an enabled recipient.
        if (!partner.isEnabled()) {
            throw conversationDenied();
        }

        // Patients may initiate a conversation with a provider.
        if (currentUser.getRole() == Role.PATIENT) {
            return partner;
        }

        // Providers may initiate only with a consenting patient.
        if (consentService.hasActiveConsent(
                partner.getId(),
                currentUser.getId()
        )) {
            return partner;
        }

        throw conversationDenied();
    }

    private boolean hasOppositeRole(User currentUser, User partner) {
        return (currentUser.getRole() == Role.PATIENT
                && partner.getRole() == Role.PROVIDER)
                || (currentUser.getRole() == Role.PROVIDER
                && partner.getRole() == Role.PATIENT);
    }

    private AccessDeniedException conversationDenied() {
        return new AccessDeniedException(
                "You do not have permission to open this conversation."
        );
    }
}