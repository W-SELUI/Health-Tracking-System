package com.group15.health_tracking_system.message.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import com.group15.health_tracking_system.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A message exchanged between a patient and a provider.
 */
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    public static final int MAX_CONTENT_LENGTH = 4000;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private User sender;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    private User recipient;

    @NotBlank(message = "Message cannot be empty.")
    @Size(
            max = MAX_CONTENT_LENGTH,
            message = "Message must be at most 4,000 characters."
    )
    @Column(
            nullable = false,
            length = MAX_CONTENT_LENGTH,
            updatable = false
    )
    private String content;

    protected Message() {
        // Required by JPA.
    }

    public Message(User sender, User recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    public User getSender() {
        return sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }
}