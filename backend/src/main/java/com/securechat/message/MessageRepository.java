package com.securechat.message;

import com.securechat.conversation.Conversation;
import com.securechat.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findAllByConversationOrderByCreatedAtAsc(Conversation conversation);

    long countByConversationAndRecipientAndSeenAtIsNull(Conversation conversation, AppUser recipient);

    void deleteAllByConversation(Conversation conversation);

    @Modifying
    @Query("delete from Message m where m.selfDestructAt is not null and m.selfDestructAt <= :now")
    int deleteExpired(@Param("now") Instant now);

    @Modifying
    @Query("update Message m set m.temporary = false where m.conversation = :conversation")
    int markPermanent(@Param("conversation") Conversation conversation);

    @Modifying
    @Query("""
            update Message m set m.seenAt = :seenAt
            where m.conversation = :conversation and m.recipient = :recipient and m.seenAt is null
            """)
    int markSeen(@Param("conversation") Conversation conversation, @Param("recipient") AppUser recipient, @Param("seenAt") Instant seenAt);
}

