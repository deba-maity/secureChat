package com.securechat.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String usernameOrPhone,
        @NotBlank String password
) {
}

