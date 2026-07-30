package com.securechat.user;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String phoneNumber,
        String displayName,
        String profilePictureUrl,
        boolean online,
        Instant lastSeen
) {
    public static UserSummaryResponse self(AppUser user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                user.isOnline(),
                user.getLastSeen()
        );
    }

    public static UserSummaryResponse visibleToOthers(AppUser user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                !user.isHideOnlineStatus() && user.isOnline(),
                user.isHideLastSeen() ? null : user.getLastSeen()
        );
    }
}

