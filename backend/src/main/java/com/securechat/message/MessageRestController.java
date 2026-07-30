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

    public MessageRestController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public MessageResponse send(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messageService.send(currentUser, request);
    }
}
