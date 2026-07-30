package com.securechat.settings;

import com.securechat.message.MessageType;
import com.securechat.user.UserSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FavoriteBackup(
        Instant exportedAt,
        int conversationCount,
        List<BackupConversation> conversations
) {
    public record BackupConversation(
            UUID conversationId,
            UserSummaryResponse participant,
            Instant lastMessageAt,
            List<BackupMessage> messages
    ) {
    }

    public record BackupMessage(
            UUID senderId,
            UUID recipientId,
            MessageType messageType,
            String iv,
            String cipherText,
            Instant createdAt,
            Instant deliveredAt,
            Instant seenAt,
            Instant selfDestructAt
    ) {
    }
}

