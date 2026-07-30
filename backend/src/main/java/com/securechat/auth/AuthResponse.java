package com.securechat.auth;

import com.securechat.user.UserSummaryResponse;

public record AuthResponse(
        String token,
        UserSummaryResponse user
) {
}

