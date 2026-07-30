package com.securechat.settings;

import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
        Boolean hideLastSeen,
        Boolean hideOnlineStatus,
        Boolean readReceiptsEnabled,
        Boolean screenshotWarningEnabled,
        Boolean lockFavoriteChats,
        Boolean darkMode,
        Boolean autoDeleteEnabled,
        @Size(min = 4, max = 12) String favoritePin
) {
}

