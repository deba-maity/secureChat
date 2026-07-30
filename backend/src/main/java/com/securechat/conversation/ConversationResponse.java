package com.securechat.conversation;

import com.securechat.user.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UserSummaryResponse participant,
        boolean favorite,
        boolean locked,
        Instant lastMessageAt,
        long unreadCount
) {
}
