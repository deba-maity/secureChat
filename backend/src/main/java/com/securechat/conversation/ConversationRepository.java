package com.securechat.conversation;

import com.securechat.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @Query("""
            select c from Conversation c
            where (c.participantOne.id = :firstUserId and c.participantTwo.id = :secondUserId)
               or (c.participantOne.id = :secondUserId and c.participantTwo.id = :firstUserId)
            """)
    Optional<Conversation> findBetween(@Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId);

    List<Conversation> findAllByParticipantOneOrParticipantTwo(AppUser participantOne, AppUser participantTwo);
}
