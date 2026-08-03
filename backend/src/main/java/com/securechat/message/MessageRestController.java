package com.securechat.message;

import com.securechat.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageRestController {
    private final MessageService messageService;
    private final MessageDeliveryService messageDeliveryService;

    public MessageRestController(MessageService messageService, MessageDeliveryService messageDeliveryService) {
        this.messageService = messageService;
        this.messageDeliveryService = messageDeliveryService;
    }

    @PostMapping
    public MessageResponse send(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse message = messageService.send(currentUser, request);
        messageDeliveryService.deliver(message);
        return message;
    }
}
