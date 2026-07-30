package com.securechat.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        UUID conversationId,
        UUID recipientId,
        @NotBlank @Size(max = 4000) String content,
        Integer selfDestructSeconds
) {
}

