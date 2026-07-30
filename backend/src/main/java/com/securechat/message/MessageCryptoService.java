package com.securechat.message;

import com.securechat.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MessageCryptoService {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public MessageCryptoService(@Value("${securechat.crypto.secret}") String secret) {
        this.key = new SecretKeySpec(sha256(secret), "AES");
    }

    public EncryptedPayload encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPayload(
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(cipherText)
            );
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to encrypt message");
        }
    }

    public String decrypt(String iv, String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(iv))
            );
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to decrypt message");
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
