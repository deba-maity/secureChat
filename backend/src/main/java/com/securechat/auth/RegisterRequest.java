package com.securechat.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 48) @Pattern(regexp = "^[a-zA-Z0-9_.-]+$")
        String username,
        @NotBlank @Size(min = 7, max = 32)
        String phoneNumber,
        @NotBlank @Size(min = 2, max = 96)
        String displayName,
        String profilePictureUrl,
        @NotBlank @Size(min = 8, max = 128)
        String password
) {
}

