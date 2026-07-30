package com.securechat.message;

import com.securechat.common.ApiException;
import com.securechat.user.AppUser;
import com.securechat.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MessagingController {
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessagingController(
            MessageService messageService,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest request, Principal principal) {
        AppUser sender = currentUser(principal);
        MessageResponse message = messageService.send(sender, request);
        messagingTemplate.convertAndSendToUser(message.sender().username(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(message.recipient().username(), "/queue/messages", message);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingSignal signal, Principal principal) {
        AppUser sender = currentUser(principal);
        AppUser recipient = userRepository.findById(signal.recipientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));
        messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/typing",
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
