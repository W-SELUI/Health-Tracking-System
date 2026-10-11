package com.group15.health_tracking_system.message.controller;

import com.group15.health_tracking_system.message.dto.SendMessageRequest;
import com.group15.health_tracking_system.message.entity.Message;
import com.group15.health_tracking_system.message.service.MessageService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Displays the messaging inbox and individual conversations.
 */
@Controller
@RequestMapping("/messages")
@PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER')")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    public MessageController(
            MessageService messageService,
            CurrentUserService currentUserService
    ) {
        this.messageService = messageService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String showInbox(Model model) {
        model.addAttribute(
                "currentUser",
                currentUserService.getCurrentUser()
        );

        model.addAttribute(
                "conversationPartners",
                messageService.getConversationPartners()
        );

        model.addAttribute(
                "availableRecipients",
                messageService.getAvailableRecipients()
        );

        return "message/inbox";
    }

    @GetMapping("/{otherUserId}")
    public String showConversation(
            @PathVariable("otherUserId") Long otherUserId,
            Model model
    ) {
        addConversationAttributes(otherUserId, model);

        model.addAttribute(
                "messageRequest",
                new SendMessageRequest()
        );

        return "message/conversation";
    }

    @PostMapping("/{otherUserId}")
    public String sendMessage(
            @PathVariable("otherUserId") Long otherUserId,
            @Valid @ModelAttribute("messageRequest")
            SendMessageRequest messageRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addConversationAttributes(otherUserId, model);
            return "message/conversation";
        }

        try {
            messageService.sendMessage(
                    otherUserId,
                    messageRequest.getContent()
            );
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue(
                    "content",
                    "message.invalid",
                    exception.getMessage()
            );

            addConversationAttributes(otherUserId, model);
            return "message/conversation";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Message sent."
        );

        return "redirect:/messages/" + otherUserId;
    }

    private void addConversationAttributes(
            Long otherUserId,
            Model model
    ) {
        User partner = messageService.getConversationPartner(otherUserId);

        model.addAttribute(
                "currentUser",
                currentUserService.getCurrentUser()
        );

        model.addAttribute("partner", partner);

        model.addAttribute(
                "messages",
                messageService.getConversation(otherUserId)
        );

        model.addAttribute(
                "maxContentLength",
                Message.MAX_CONTENT_LENGTH
        );

        model.addAttribute(
                "canSend",
                partner.isEnabled()
        );
    }
}