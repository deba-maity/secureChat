package com.securechat.message;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageDeliveryService {
    private final SimpMessagingTemplate messagingTemplate;

    public MessageDeliveryService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void deliver(MessageResponse message) {
        messagingTemplate.convertAndSendToUser(message.sender().username(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(message.recipient().username(), "/queue/messages", message);
    }

    public void deliverTyping(String recipientUsername, TypingEvent event) {
        messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/typing", event);
    }
}
