package com.securechat.message;

import com.securechat.common.ApiException;
import com.securechat.conversation.Conversation;
import com.securechat.conversation.ConversationRepository;
import com.securechat.conversation.ConversationService;
import com.securechat.conversation.FavoriteConversationRepository;
import com.securechat.user.AppUser;
import com.securechat.user.UserRepository;
import com.securechat.user.UserSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MessageService {
    private static final Set<Integer> SUPPORTED_SELF_DESTRUCT_SECONDS = Set.of(30, 60, 3600, 86400);

    private final MessageRepository messageRepository;
    private final MessageCryptoService cryptoService;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final FavoriteConversationRepository favoriteConversationRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            MessageCryptoService cryptoService,
            ConversationService conversationService,
            ConversationRepository conversationRepository,
            FavoriteConversationRepository favoriteConversationRepository,
            UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.cryptoService = cryptoService;
        this.conversationService = conversationService;
        this.conversationRepository = conversationRepository;
        this.favoriteConversationRepository = favoriteConversationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageResponse send(AppUser sender, SendMessageRequest request) {
        AppUser managedSender = managed(sender);
        Conversation conversation = resolveConversation(managedSender, request);
        AppUser recipient = conversation.otherParticipant(managedSender);
        if (request.recipientId() != null && !recipient.getId().equals(request.recipientId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recipient does not match the conversation");
        }

        EncryptedPayload encryptedPayload = cryptoService.encrypt(request.content().trim());
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(managedSender);
        message.setRecipient(recipient);
        message.setMessageType(MessageType.TEXT);
        message.setIv(encryptedPayload.iv());
        message.setCipherText(encryptedPayload.cipherText());
        message.setDeliveredAt(Instant.now());
        message.setTemporary(favoriteConversationRepository.countByConversation(conversation) == 0);
        message.setSelfDestructAt(selfDestructAt(request.selfDestructSeconds()));

        Message saved = messageRepository.save(message);
        conversation.setLastMessageAt(saved.getCreatedAt());
        return toResponse(saved);
    }

    @Transactional
    public List<MessageResponse> messages(AppUser currentUser, UUID conversationId) {
        messageRepository.deleteExpired(Instant.now());
        Conversation conversation = conversationService.requireParticipant(managed(currentUser), conversationId);
        return messageRepository.findAllByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markSeen(AppUser currentUser, UUID conversationId) {
        AppUser current = managed(currentUser);
        if (!current.isReadReceiptsEnabled()) {
            return;
        }
        Conversation conversation = conversationService.requireParticipant(current, conversationId);
        messageRepository.markSeen(conversation, current, Instant.now());
    }

    @Transactional
    public List<MessageResponse> searchFavoriteMessages(AppUser currentUser, UUID conversationId, String query) {
        AppUser current = managed(currentUser);
        Conversation conversation = conversationService.requireParticipant(current, conversationId);
        if (!favoriteConversationRepository.existsByUserAndConversation(current, conversation)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only favorite chats can be searched");
        }
        String normalized = query.trim().toLowerCase();
        if (normalized.length() < 2) {
            return List.of();
        }
        return messageRepository.findAllByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .filter(message -> decrypt(message).toLowerCase().contains(normalized))
                .map(this::toResponse)
                .toList();
    }

    public MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                UserSummaryResponse.visibleToOthers(message.getSender()),
                UserSummaryResponse.visibleToOthers(message.getRecipient()),
                message.getMessageType(),
                decrypt(message),
                message.getCreatedAt(),
                message.getDeliveredAt(),
                message.getSeenAt(),
                message.getSelfDestructAt(),
                message.isTemporary()
        );
    }

    private String decrypt(Message message) {
        return cryptoService.decrypt(message.getIv(), message.getCipherText());
    }

    private Conversation resolveConversation(AppUser sender, SendMessageRequest request) {
        if (request.conversationId() != null) {
            return conversationService.requireParticipant(sender, request.conversationId());
        }
        if (request.recipientId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "recipientId or conversationId is required");
        }
        AppUser recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));
        if (sender.getId().equals(recipient.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot message yourself");
        }
        return conversationRepository.findBetween(sender.getId(), recipient.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private Instant selfDestructAt(Integer seconds) {
        if (seconds == null || seconds == 0) {
            return null;
        }
        if (!SUPPORTED_SELF_DESTRUCT_SECONDS.contains(seconds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported self-destruct timer");
        }
        return Instant.now().plusSeconds(seconds);
    }

    private AppUser managed(AppUser currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
