package com.securechat.settings;

import com.securechat.user.AppUser;

public record SettingsResponse(
        boolean hideLastSeen,
        boolean hideOnlineStatus,
        boolean readReceiptsEnabled,
        boolean screenshotWarningEnabled,
        boolean lockFavoriteChats,
        boolean darkMode,
        boolean autoDeleteEnabled,
        boolean favoritePinConfigured
) {
    static SettingsResponse from(AppUser user) {
        return new SettingsResponse(
                user.isHideLastSeen(),
                user.isHideOnlineStatus(),
                user.isReadReceiptsEnabled(),
                user.isScreenshotWarningEnabled(),
                user.isLockFavoriteChats(),
                user.isDarkMode(),
                user.isAutoDeleteEnabled(),
                user.getFavoritePinHash() != null
        );
    }
}

