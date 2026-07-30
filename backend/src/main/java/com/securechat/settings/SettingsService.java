package com.securechat.settings;

import com.securechat.common.ApiException;
import com.securechat.conversation.Conversation;
import com.securechat.conversation.ConversationRepository;
import com.securechat.conversation.FavoriteConversation;
import com.securechat.conversation.FavoriteConversationRepository;
import com.securechat.message.Message;
import com.securechat.message.MessageRepository;
import com.securechat.user.AppUser;
import com.securechat.user.UserRepository;
import com.securechat.user.UserSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SettingsService {
    private final FavoriteConversationRepository favoriteConversationRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(
            FavoriteConversationRepository favoriteConversationRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.favoriteConversationRepository = favoriteConversationRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get(AppUser user) {
        return SettingsResponse.from(managed(user));
    }

    @Transactional
    public SettingsResponse update(AppUser user, UpdateSettingsRequest request) {
        AppUser current = managed(user);
        if (request.hideLastSeen() != null) {
            current.setHideLastSeen(request.hideLastSeen());
        }
        if (request.hideOnlineStatus() != null) {
            current.setHideOnlineStatus(request.hideOnlineStatus());
        }
        if (request.readReceiptsEnabled() != null) {
            current.setReadReceiptsEnabled(request.readReceiptsEnabled());
        }
        if (request.screenshotWarningEnabled() != null) {
            current.setScreenshotWarningEnabled(request.screenshotWarningEnabled());
        }
        if (request.lockFavoriteChats() != null) {
            current.setLockFavoriteChats(request.lockFavoriteChats());
            favoriteConversationRepository.findAllByUserOrderByCreatedAtDesc(current)
                    .forEach(favorite -> favorite.setLocked(request.lockFavoriteChats()));
        }
        if (request.darkMode() != null) {
            current.setDarkMode(request.darkMode());
        }
        if (request.autoDeleteEnabled() != null) {
            current.setAutoDeleteEnabled(request.autoDeleteEnabled());
        }
        if (request.favoritePin() != null && !request.favoritePin().isBlank()) {
            current.setFavoritePinHash(passwordEncoder.encode(request.favoritePin()));
            current.setLockFavoriteChats(true);
            favoriteConversationRepository.findAllByUserOrderByCreatedAtDesc(current)
                    .forEach(favorite -> favorite.setLocked(true));
        }
        return SettingsResponse.from(current);
    }

    @Transactional(readOnly = true)
    public PinVerifyResponse verifyFavoritePin(AppUser user, PinVerifyRequest request) {
        AppUser current = managed(user);
        if (current.getFavoritePinHash() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Favorite PIN is not configured");
        }
        return new PinVerifyResponse(passwordEncoder.matches(request.pin(), current.getFavoritePinHash()));
    }

    @Transactional
    public FavoriteBackup exportFavorites(AppUser user) {
        AppUser current = managed(user);
        List<FavoriteBackup.BackupConversation> conversations = favoriteConversationRepository
                .findAllByUserOrderByCreatedAtDesc(current)
                .stream()
                .map(favorite -> backupConversation(current, favorite.getConversation()))
                .toList();
        return new FavoriteBackup(Instant.now(), conversations.size(), conversations);
    }

    @Transactional
    public ImportBackupResponse importFavorites(AppUser user, FavoriteBackup backup) {
        AppUser current = managed(user);
        int conversationsImported = 0;
        int messagesImported = 0;
        for (FavoriteBackup.BackupConversation backupConversation : backup.conversations()) {
            AppUser participant = userRepository.findById(backupConversation.participant().id())
                    .orElse(null);
            if (participant == null || participant.getId().equals(current.getId())) {
                continue;
            }
            Conversation conversation = conversationRepository.findBetween(current.getId(), participant.getId())
                    .orElseGet(() -> createPair(current, participant));
            if (favoriteConversationRepository.findByUserAndConversation(current, conversation).isEmpty()) {
                FavoriteConversation favorite = new FavoriteConversation();
                favorite.setUser(current);
                favorite.setConversation(conversation);
                favorite.setLocked(current.isLockFavoriteChats());
                favoriteConversationRepository.save(favorite);
                conversationsImported++;
            }
            for (FavoriteBackup.BackupMessage backupMessage : backupConversation.messages()) {
                AppUser sender = userRepository.findById(backupMessage.senderId()).orElse(null);
                AppUser recipient = userRepository.findById(backupMessage.recipientId()).orElse(null);
                if (sender == null || recipient == null) {
                    continue;
                }
                Message message = new Message();
                message.setConversation(conversation);
                message.setSender(sender);
                message.setRecipient(recipient);
                message.setMessageType(backupMessage.messageType());
                message.setIv(backupMessage.iv());
                message.setCipherText(backupMessage.cipherText());
                message.setTemporary(false);
                message.setCreatedAt(backupMessage.createdAt());
                message.setDeliveredAt(backupMessage.deliveredAt());
                message.setSeenAt(backupMessage.seenAt());
                message.setSelfDestructAt(backupMessage.selfDestructAt());
                messageRepository.save(message);
                messagesImported++;
            }
            conversation.setLastMessageAt(backupConversation.lastMessageAt());
            messageRepository.markPermanent(conversation);
        }
        return new ImportBackupResponse(conversationsImported, messagesImported);
    }

    @Transactional
    public void deleteAccount(AppUser user) {
        userRepository.delete(managed(user));
    }

    private FavoriteBackup.BackupConversation backupConversation(AppUser currentUser, Conversation conversation) {
        List<FavoriteBackup.BackupMessage> messages = messageRepository.findAllByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(message -> new FavoriteBackup.BackupMessage(
                        message.getSender().getId(),
                        message.getRecipient().getId(),
                        message.getMessageType(),
                        message.getIv(),
                        message.getCipherText(),
                        message.getCreatedAt(),
                        message.getDeliveredAt(),
                        message.getSeenAt(),
                        message.getSelfDestructAt()
                ))
                .toList();
        return new FavoriteBackup.BackupConversation(
                conversation.getId(),
                UserSummaryResponse.visibleToOthers(conversation.otherParticipant(currentUser)),
                conversation.getLastMessageAt(),
                messages
        );
    }

    private Conversation createPair(AppUser currentUser, AppUser participant) {
        Conversation conversation = new Conversation();
        if (currentUser.getId().compareTo(participant.getId()) < 0) {
            conversation.setParticipantOne(currentUser);
            conversation.setParticipantTwo(participant);
        } else {
            conversation.setParticipantOne(participant);
            conversation.setParticipantTwo(currentUser);
        }
        return conversationRepository.save(conversation);
    }

    private AppUser managed(AppUser user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
