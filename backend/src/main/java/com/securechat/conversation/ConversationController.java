package com.securechat.conversation;

import com.securechat.message.MessageResponse;
import com.securechat.message.MessageService;
import com.securechat.user.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping("/start")
    public ConversationResponse start(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody StartConversationRequest request
    ) {
        return conversationService.start(currentUser, request);
    }

    @GetMapping("/favorites")
    public List<ConversationResponse> favorites(@AuthenticationPrincipal AppUser currentUser) {
        return conversationService.favorites(currentUser);
    }

    @PostMapping("/{conversationId}/favorite")
    public ConversationResponse favorite(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID conversationId
    ) {
        return conversationService.favorite(currentUser, conversationId);
    }

    @DeleteMapping("/{conversationId}/favorite")
    public ConversationResponse unfavorite(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID conversationId
    ) {
        return conversationService.unfavorite(currentUser, conversationId);
    }

    @DeleteMapping("/{conversationId}/temporary")
    public ResponseEntity<Void> deleteTemporary(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID conversationId
    ) {
        conversationService.deleteTemporary(currentUser, conversationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> messages(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID conversationId
    ) {
        return messageService.messages(currentUser, conversationId);
    }

    @PostMapping("/{conversationId}/seen")
    public ResponseEntity<Void> markSeen(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID conversationId
    ) {
        messageService.markSeen(currentUser, conversationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{conversationId}/messages/search")
    public List<MessageResponse> searchMessages(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID conversationId,
            @RequestParam("q") @NotBlank String query
    ) {
        return messageService.searchFavoriteMessages(currentUser, conversationId, query);
    }
}

