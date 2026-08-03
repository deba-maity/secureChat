package com.securechat.message;

import com.securechat.common.ApiException;
import com.securechat.user.AppUser;
import com.securechat.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MessagingController {
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final MessageDeliveryService messageDeliveryService;

    public MessagingController(
            MessageService messageService,
            UserRepository userRepository,
            MessageDeliveryService messageDeliveryService
    ) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.messageDeliveryService = messageDeliveryService;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest request, Principal principal) {
        AppUser sender = currentUser(principal);
        MessageResponse message = messageService.send(sender, request);
        messageDeliveryService.deliver(message);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingSignal signal, Principal principal) {
        AppUser sender = currentUser(principal);
        AppUser recipient = userRepository.findById(signal.recipientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));
        messageDeliveryService.deliverTyping(
                recipient.getUsername(),
                new TypingEvent(signal.conversationId(), sender.getUsername(), signal.typing())
        );
    }

    private AppUser currentUser(Principal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "WebSocket session is not authenticated");
        }
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "WebSocket session is not authenticated"));
    }

}
