package com.securechat.conversation;

import com.securechat.common.ApiException;
import com.securechat.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversations_pair",
                columnNames = {"participant_one_id", "participant_two_id"}
        )
)
public class Conversation {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_one_id", nullable = false)
    private AppUser participantOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_two_id", nullable = false)
    private AppUser participantTwo;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean hasParticipant(AppUser user) {
        UUID userId = user.getId();
        return participantOne.getId().equals(userId) || participantTwo.getId().equals(userId);
    }

    public AppUser otherParticipant(AppUser user) {
        if (participantOne.getId().equals(user.getId())) {
            return participantTwo;
        }
        if (participantTwo.getId().equals(user.getId())) {
            return participantOne;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "User is not part of this conversation");
    }

    public UUID getId() {
        return id;
    }

    public AppUser getParticipantOne() {
        return participantOne;
    }

    public void setParticipantOne(AppUser participantOne) {
        this.participantOne = participantOne;
    }

    public AppUser getParticipantTwo() {
        return participantTwo;
    }

    public void setParticipantTwo(AppUser participantTwo) {
        this.participantTwo = participantTwo;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

