package com.securechat.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "app_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_app_users_phone", columnNames = "phone_number")
        }
)
public class AppUser {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 48)
    private String username;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "display_name", nullable = false, length = 96)
    private String displayName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "hide_last_seen", nullable = false)
    private boolean hideLastSeen;

    @Column(name = "hide_online_status", nullable = false)
    private boolean hideOnlineStatus;

    @Column(name = "read_receipts_enabled", nullable = false)
    private boolean readReceiptsEnabled = true;

    @Column(name = "screenshot_warning_enabled", nullable = false)
    private boolean screenshotWarningEnabled = true;

    @Column(name = "lock_favorite_chats", nullable = false)
    private boolean lockFavoriteChats;

    @Column(name = "dark_mode", nullable = false)
    private boolean darkMode = true;

    @Column(name = "auto_delete_enabled", nullable = false)
    private boolean autoDeleteEnabled = true;

    @Column(name = "favorite_pin_hash")
    private String favoritePinHash;

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

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isHideLastSeen() {
        return hideLastSeen;
    }

    public void setHideLastSeen(boolean hideLastSeen) {
        this.hideLastSeen = hideLastSeen;
    }

    public boolean isHideOnlineStatus() {
        return hideOnlineStatus;
    }

    public void setHideOnlineStatus(boolean hideOnlineStatus) {
        this.hideOnlineStatus = hideOnlineStatus;
    }

    public boolean isReadReceiptsEnabled() {
        return readReceiptsEnabled;
    }

    public void setReadReceiptsEnabled(boolean readReceiptsEnabled) {
        this.readReceiptsEnabled = readReceiptsEnabled;
    }

    public boolean isScreenshotWarningEnabled() {
        return screenshotWarningEnabled;
    }

    public void setScreenshotWarningEnabled(boolean screenshotWarningEnabled) {
        this.screenshotWarningEnabled = screenshotWarningEnabled;
    }

    public boolean isLockFavoriteChats() {
        return lockFavoriteChats;
    }

    public void setLockFavoriteChats(boolean lockFavoriteChats) {
        this.lockFavoriteChats = lockFavoriteChats;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public boolean isAutoDeleteEnabled() {
        return autoDeleteEnabled;
    }

    public void setAutoDeleteEnabled(boolean autoDeleteEnabled) {
        this.autoDeleteEnabled = autoDeleteEnabled;
    }

    public String getFavoritePinHash() {
        return favoritePinHash;
    }

    public void setFavoritePinHash(String favoritePinHash) {
        this.favoritePinHash = favoritePinHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

