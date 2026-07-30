package com.securechat.message;

import java.util.UUID;

public record TypingSignal(
        UUID conversationId,
        UUID recipientId,
        boolean typing
) {
}

