package com.securechat.conversation;

import com.securechat.common.ApiException;
import com.securechat.message.MessageRepository;
import com.securechat.user.AppUser;
import com.securechat.user.UserRepository;
import com.securechat.user.UserSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final FavoriteConversationRepository favoriteConversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            FavoriteConversationRepository favoriteConversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.favoriteConversationRepository = favoriteConversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConversationResponse start(AppUser currentUser, StartConversationRequest request) {
        AppUser current = managed(currentUser);
        AppUser target = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (current.getId().equals(target.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot start a chat with yourself");
        }
        Conversation conversation = conversationRepository
                .findBetween(current.getId(), target.getId())
                .orElseGet(() -> createPair(current, target));
        return toResponse(conversation, current);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> favorites(AppUser currentUser) {
        AppUser current = managed(currentUser);
        return favoriteConversationRepository.findAllByUserOrderByCreatedAtDesc(current)
                .stream()
                .map(favorite -> toResponse(favorite.getConversation(), current))
                .toList();
    }

    @Transactional
    public ConversationResponse favorite(AppUser currentUser, UUID conversationId) {
        AppUser current = managed(currentUser);
        Conversation conversation = requireParticipant(current, conversationId);
        FavoriteConversation favorite = favoriteConversationRepository
                .findByUserAndConversation(current, conversation)
                .orElseGet(() -> {
                    FavoriteConversation created = new FavoriteConversation();
                    created.setUser(current);
                    created.setConversation(conversation);
                    created.setLocked(current.isLockFavoriteChats());
                    return favoriteConversationRepository.save(created);
                });
        messageRepository.markPermanent(conversation);
        return toResponse(favorite.getConversation(), current);
    }

    @Transactional
    public ConversationResponse unfavorite(AppUser currentUser, UUID conversationId) {
        AppUser current = managed(currentUser);
        Conversation conversation = requireParticipant(current, conversationId);
        favoriteConversationRepository.findByUserAndConversation(current, conversation)
                .ifPresent(favoriteConversationRepository::delete);
        return toResponse(conversation, current);
    }

    @Transactional
    public void deleteTemporary(AppUser currentUser, UUID conversationId) {
        AppUser current = managed(currentUser);
        Conversation conversation = requireParticipant(current, conversationId);
        if (favoriteConversationRepository.existsByUserAndConversation(current, conversation)) {
            return;
        }
        if (favoriteConversationRepository.countByConversation(conversation) == 0) {
            messageRepository.deleteAllByConversation(conversation);
            conversationRepository.delete(conversation);
        }
    }

    @Transactional
    public void deleteTemporaryForUser(AppUser currentUser) {
        AppUser current = managed(currentUser);
        conversationRepository.findAllByParticipantOneOrParticipantTwo(current, current)
                .stream()
                .filter(conversation -> favoriteConversationRepository.countByConversation(conversation) == 0)
                .forEach(conversation -> {
                    messageRepository.deleteAllByConversation(conversation);
                    conversationRepository.delete(conversation);
                });
    }

    @Transactional(readOnly = true)
    public Conversation requireParticipant(AppUser currentUser, UUID conversationId) {
        AppUser current = managed(currentUser);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!conversation.hasParticipant(current)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of this conversation");
        }
        return conversation;
    }

    ConversationResponse toResponse(Conversation conversation, AppUser currentUser) {
        FavoriteConversation favorite = favoriteConversationRepository
                .findByUserAndConversation(currentUser, conversation)
                .orElse(null);
        long unreadCount = messageRepository.countByConversationAndRecipientAndSeenAtIsNull(conversation, currentUser);
        return new ConversationResponse(
                conversation.getId(),
                UserSummaryResponse.visibleToOthers(conversation.otherParticipant(currentUser)),
                favorite != null,
                favorite != null && favorite.isLocked(),
                conversation.getLastMessageAt(),
                unreadCount);
    }

    private Conversation createPair(AppUser currentUser, AppUser target) {
        Conversation conversation = new Conversation();
        if (currentUser.getId().compareTo(target.getId()) < 0) {
            conversation.setParticipantOne(currentUser);
            conversation.setParticipantTwo(target);
        } else {
            conversation.setParticipantOne(target);
            conversation.setParticipantTwo(currentUser);
        }
        return conversationRepository.save(conversation);
    }

    private AppUser managed(AppUser currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
