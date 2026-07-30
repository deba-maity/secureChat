package com.securechat.message;

public record EncryptedPayload(String iv, String cipherText) {
}

