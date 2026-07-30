package com.securechat.message;

import java.util.UUID;

public record TypingEvent(
        UUID conversationId,
        String username,
        boolean typing
) {
}
