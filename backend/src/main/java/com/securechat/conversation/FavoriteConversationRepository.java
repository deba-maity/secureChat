package com.securechat.conversation;

import com.securechat.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteConversationRepository extends JpaRepository<FavoriteConversation, UUID> {
    List<FavoriteConversation> findAllByUserOrderByCreatedAtDesc(AppUser user);

    Optional<FavoriteConversation> findByUserAndConversation(AppUser user, Conversation conversation);

    boolean existsByUserAndConversation(AppUser user, Conversation conversation);

    long countByConversation(Conversation conversation);
}

