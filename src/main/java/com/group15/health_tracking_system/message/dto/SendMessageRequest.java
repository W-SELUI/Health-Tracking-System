package com.group15.health_tracking_system.message.dto;

import com.group15.health_tracking_system.message.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Message text submitted through the conversation form.
 */
public class SendMessageRequest {

    @NotBlank(message = "Message cannot be empty.")
    @Size(
            max = Message.MAX_CONTENT_LENGTH,
            message = "Message must be at most {max} characters."
    )
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}