package com.securechat.message;

import com.securechat.user.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UserSummaryResponse sender,
        UserSummaryResponse recipient,
        MessageType messageType,
        String content,
        Instant createdAt,
        Instant deliveredAt,
        Instant seenAt,
        Instant selfDestructAt,
        boolean temporary
) {
}

