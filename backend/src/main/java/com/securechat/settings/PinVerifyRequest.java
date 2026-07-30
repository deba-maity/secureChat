package com.securechat.settings;

import jakarta.validation.constraints.NotBlank;

public record PinVerifyRequest(
        @NotBlank String pin
) {
}

